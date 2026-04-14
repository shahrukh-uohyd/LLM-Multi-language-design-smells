/**
 * crypto_native.cpp
 *
 * JNI implementation of com.app.crypto.CryptoNative.
 *
 * Algorithm : AES-256-GCM (encrypt/decrypt) + HKDF-SHA-256 (key derivation)
 * Dependency: OpenSSL >= 1.1.0   (link with -lssl -lcrypto)
 *
 * Compile (Linux example):
 *   g++ -std=c++17 -shared -fPIC -O2 -Wall -Wextra \
 *       -I"${JAVA_HOME}/include"       \
 *       -I"${JAVA_HOME}/include/linux" \
 *       crypto_native.cpp              \
 *       -lssl -lcrypto                 \
 *       -o libcrypto_native.so
 *
 * Wire format produced by encrypt():
 *   ┌──────────────┬──────────────────────────┬─────────────────┐
 *   │  IV  (12 B)  │  Ciphertext  (variable)  │  GCM Tag (16 B) │
 *   └──────────────┴──────────────────────────┴─────────────────┘
 */

#include <jni.h>

#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/err.h>
#include <openssl/kdf.h>
#include <openssl/crypto.h>

#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <memory>
#include <string>

// ── Constants ────────────────────────────────────────────────────────────────
static constexpr int AES_KEY_LEN  = 32;   // AES-256
static constexpr int GCM_IV_LEN   = 12;   // 96-bit nonce (NIST recommended)
static constexpr int GCM_TAG_LEN  = 16;   // 128-bit authentication tag

// ── JNI class/exception paths ─────────────────────────────────────────────────
static constexpr const char* CRYPTO_EXCEPTION_CLASS = "com/app/crypto/CryptoException";
static constexpr const char* IAE_CLASS              = "java/lang/IllegalArgumentException";
static constexpr const char* OOM_CLASS              = "java/lang/OutOfMemoryError";

// ── CryptoException::Operation ordinals (must mirror the Java enum order) ────
static constexpr jint OP_ENCRYPT     = 0;
static constexpr jint OP_DECRYPT     = 1;
static constexpr jint OP_KEY_DERIV   = 2;
static constexpr jint OP_CTX_INIT    = 3;
static constexpr jint OP_UNKNOWN     = 4;

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Formats the OpenSSL error queue into a single string. */
static std::string openssl_error_string() {
    char buf[256];
    unsigned long err = ERR_get_error();
    if (err == 0) return "no OpenSSL error";
    ERR_error_string_n(err, buf, sizeof(buf));
    return std::string(buf);
}

/** Throws java.lang.IllegalArgumentException. */
static void throw_iae(JNIEnv* env, const char* message) {
    jclass cls = env->FindClass(IAE_CLASS);
    if (cls) { env->ThrowNew(cls, message); env->DeleteLocalRef(cls); }
}

/** Throws java.lang.OutOfMemoryError. */
static void throw_oom(JNIEnv* env, const char* message) {
    jclass cls = env->FindClass(OOM_CLASS);
    if (cls) { env->ThrowNew(cls, message); env->DeleteLocalRef(cls); }
}

/**
 * Throws com.app.crypto.CryptoException(String message, long errorCode, int operationOrdinal).
 * Mirrors the three-argument constructor in CryptoException.java.
 */
static void throw_crypto_exception(JNIEnv* env,
                                    const char* context,
                                    jlong       errorCode,
                                    jint        operationOrdinal) {
    jclass cls = env->FindClass(CRYPTO_EXCEPTION_CLASS);
    if (!cls) return; // FindClass already threw NoClassDefFoundError

    jmethodID ctor = env->GetMethodID(cls, "<init>", "(Ljava/lang/String;JI)V");
    if (!ctor) { env->DeleteLocalRef(cls); return; }

    std::string detail = std::string(context) + ": " + openssl_error_string();
    jstring jmsg = env->NewStringUTF(detail.c_str());
    if (!jmsg) { env->DeleteLocalRef(cls); return; } // OOM already pending

    jobject ex = env->NewObject(cls, ctor, jmsg, errorCode, operationOrdinal);
    if (ex) { env->Throw(static_cast<jthrowable>(ex)); env->DeleteLocalRef(ex); }

    env->DeleteLocalRef(jmsg);
    env->DeleteLocalRef(cls);
}

// ─────────────────────────────────────────────────────────────────────────────
// RAII wrappers for OpenSSL resources (no raw pointer management in logic)
// ─────────────────────────────────────────────────────────────────────────────

struct EvpCipherCtxDeleter {
    void operator()(EVP_CIPHER_CTX* ctx) const { EVP_CIPHER_CTX_free(ctx); }
};
using EvpCipherCtxPtr = std::unique_ptr<EVP_CIPHER_CTX, EvpCipherCtxDeleter>;

struct EvpPkeyCtxDeleter {
    void operator()(EVP_PKEY_CTX* ctx) const { EVP_PKEY_CTX_free(ctx); }
};
using EvpPkeyCtxPtr = std::unique_ptr<EVP_PKEY_CTX, EvpPkeyCtxDeleter>;

// ─────────────────────────────────────────────────────────────────────────────
// JNI pinned-array helper (RAII)
// ─────────────────────────────────────────────────────────────────────────────

struct PinnedByteArray {
    JNIEnv*    env;
    jbyteArray jarray;
    jbyte*     data   = nullptr;
    jsize      length = 0;

    PinnedByteArray(JNIEnv* e, jbyteArray arr) : env(e), jarray(arr) {
        if (arr) {
            data   = env->GetByteArrayElements(arr, nullptr);
            length = env->GetArrayLength(arr);
        }
    }
    ~PinnedByteArray() {
        if (data) env->ReleaseByteArrayElements(jarray, data, JNI_ABORT);
    }
    bool ok() const { return data != nullptr; }

    // Non-copyable
    PinnedByteArray(const PinnedByteArray&)            = delete;
    PinnedByteArray& operator=(const PinnedByteArray&) = delete;
};

// ─────────────────────────────────────────────────────────────────────────────
// encrypt()
//
// JNI name: Java_com_app_crypto_CryptoNative_encrypt
// Signature: ([B[B[B)[B
// ─────────────────────────────────────────────────────────────────────────────
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoNative_encrypt(JNIEnv*    env,
                                          jobject    /* thiz */,
                                          jbyteArray jKey,
                                          jbyteArray jPlaintext,
                                          jbyteArray jAad) {
    // ── 1. Validate arguments ─────────────────────────────────────────
    if (!jKey) { throw_iae(env, "key must not be null"); return nullptr; }
    if (!jPlaintext) { throw_iae(env, "plaintext must not be null"); return nullptr; }

    if (env->GetArrayLength(jKey) != AES_KEY_LEN) {
        throw_iae(env, "AES-256 key must be 32 bytes");
        return nullptr;
    }
    jsize plaintextLen = env->GetArrayLength(jPlaintext);
    if (plaintextLen == 0) { throw_iae(env, "plaintext must not be empty"); return nullptr; }

    // ── 2. Pin Java arrays ────────────────────────────────────────────
    PinnedByteArray key(env, jKey);
    PinnedByteArray plaintext(env, jPlaintext);
    if (!key.ok() || !plaintext.ok()) {
        throw_oom(env, "failed to pin input arrays");
        return nullptr;
    }

    // AAD is optional
    PinnedByteArray aad(env, jAad); // data will be nullptr if jAad is null — that is fine

    // ── 3. Generate random IV ─────────────────────────────────────────
    unsigned char iv[GCM_IV_LEN];
    if (RAND_bytes(iv, GCM_IV_LEN) != 1) {
        throw_crypto_exception(env, "RAND_bytes (IV generation) failed",
                               static_cast<jlong>(ERR_get_error()), OP_ENCRYPT);
        return nullptr;
    }

    // ── 4. Allocate output buffer: IV + ciphertext (same size as plaintext) + tag
    jsize outputLen = GCM_IV_LEN + plaintextLen + GCM_TAG_LEN;
    auto  output    = std::make_unique<unsigned char[]>(static_cast<size_t>(outputLen));

    // Embed IV at the front of the output buffer
    std::memcpy(output.get(), iv, GCM_IV_LEN);

    unsigned char* cipherStart = output.get() + GCM_IV_LEN;
    unsigned char* tagStart    = cipherStart + plaintextLen;

    // ── 5. AES-256-GCM encrypt ────────────────────────────────────────
    EvpCipherCtxPtr ctx(EVP_CIPHER_CTX_new());
    if (!ctx) {
        throw_crypto_exception(env, "EVP_CIPHER_CTX_new failed",
                               static_cast<jlong>(ERR_get_error()), OP_CTX_INIT);
        return nullptr;
    }

    if (EVP_EncryptInit_ex(ctx.get(), EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1) {
        throw_crypto_exception(env, "EVP_EncryptInit_ex (cipher) failed",
                               static_cast<jlong>(ERR_get_error()), OP_CTX_INIT);
        return nullptr;
    }

    // Set IV length (12 bytes is the default, but explicit is safer)
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_IVLEN, GCM_IV_LEN, nullptr) != 1) {
        throw_crypto_exception(env, "EVP_CTRL_GCM_SET_IVLEN failed",
                               static_cast<jlong>(ERR_get_error()), OP_CTX_INIT);
        return nullptr;
    }

    // Set key and IV
    if (EVP_EncryptInit_ex(ctx.get(), nullptr, nullptr,
                            reinterpret_cast<const unsigned char*>(key.data),
                            iv) != 1) {
        throw_crypto_exception(env, "EVP_EncryptInit_ex (key+iv) failed",
                               static_cast<jlong>(ERR_get_error()), OP_ENCRYPT);
        return nullptr;
    }

    // Provide AAD (if any)
    if (aad.ok() && aad.length > 0) {
        int aadOutLen = 0;
        if (EVP_EncryptUpdate(ctx.get(), nullptr, &aadOutLen,
                               reinterpret_cast<const unsigned char*>(aad.data),
                               static_cast<int>(aad.length)) != 1) {
            throw_crypto_exception(env, "EVP_EncryptUpdate (AAD) failed",
                                   static_cast<jlong>(ERR_get_error()), OP_ENCRYPT);
            return nullptr;
        }
    }

    // Encrypt plaintext
    int encryptedLen = 0;
    if (EVP_EncryptUpdate(ctx.get(), cipherStart, &encryptedLen,
                           reinterpret_cast<const unsigned char*>(plaintext.data),
                           static_cast<int>(plaintextLen)) != 1) {
        throw_crypto_exception(env, "EVP_EncryptUpdate (plaintext) failed",
                               static_cast<jlong>(ERR_get_error()), OP_ENCRYPT);
        return nullptr;
    }

    // Finalise
    int finalLen = 0;
    if (EVP_EncryptFinal_ex(ctx.get(), cipherStart + encryptedLen, &finalLen) != 1) {
        throw_crypto_exception(env, "EVP_EncryptFinal_ex failed",
                               static_cast<jlong>(ERR_get_error()), OP_ENCRYPT);
        return nullptr;
    }

    // Extract GCM tag
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_GET_TAG, GCM_TAG_LEN, tagStart) != 1) {
        throw_crypto_exception(env, "EVP_CTRL_GCM_GET_TAG failed",
                               static_cast<jlong>(ERR_get_error()), OP_ENCRYPT);
        return nullptr;
    }

    // ── 6. Build result Java byte[] ───────────────────────────────────
    jbyteArray result = env->NewByteArray(outputLen);
    if (!result) { throw_oom(env, "failed to allocate encrypt result array"); return nullptr; }

    env->SetByteArrayRegion(result, 0, outputLen,
                             reinterpret_cast<const jbyte*>(output.get()));
    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// decrypt()
//
// JNI name: Java_com_app_crypto_CryptoNative_decrypt
// Signature: ([B[B[B)[B
// ─────────────────────────────────────────────────────────────────────────────
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoNative_decrypt(JNIEnv*    env,
                                          jobject    /* thiz */,
                                          jbyteArray jKey,
                                          jbyteArray jCipherBlob,
                                          jbyteArray jAad) {
    // ── 1. Validate ───────────────────────────────────────────────────
    if (!jKey)        { throw_iae(env, "key must not be null");        return nullptr; }
    if (!jCipherBlob) { throw_iae(env, "cipherBlob must not be null"); return nullptr; }

    if (env->GetArrayLength(jKey) != AES_KEY_LEN) {
        throw_iae(env, "AES-256 key must be 32 bytes");
        return nullptr;
    }

    jsize blobLen = env->GetArrayLength(jCipherBlob);
    jsize minLen  = GCM_IV_LEN + 1 + GCM_TAG_LEN;  // IV + 1 byte cipher + tag
    if (blobLen < minLen) {
        char msg[128];
        std::snprintf(msg, sizeof(msg),
                      "cipherBlob too short: need >= %d bytes, got %d", minLen, (int)blobLen);
        throw_iae(env, msg);
        return nullptr;
    }

    // ── 2. Pin arrays ─────────────────────────────────────────────────
    PinnedByteArray key(env, jKey);
    PinnedByteArray blob(env, jCipherBlob);
    if (!key.ok() || !blob.ok()) {
        throw_oom(env, "failed to pin input arrays");
        return nullptr;
    }

    PinnedByteArray aad(env, jAad);

    // ── 3. Split blob into IV | ciphertext | tag ──────────────────────
    const auto* blobBytes    = reinterpret_cast<const unsigned char*>(blob.data);
    const unsigned char* iv  = blobBytes;
    const unsigned char* cipher    = blobBytes + GCM_IV_LEN;
    jsize               cipherLen  = blobLen - GCM_IV_LEN - GCM_TAG_LEN;
    const unsigned char* tag       = blobBytes + GCM_IV_LEN + cipherLen;

    // ── 4. Allocate plaintext output buffer ───────────────────────────
    auto plaintext = std::make_unique<unsigned char[]>(static_cast<size_t>(cipherLen));

    // ── 5. AES-256-GCM decrypt ──────────────────────────��─────────────
    EvpCipherCtxPtr ctx(EVP_CIPHER_CTX_new());
    if (!ctx) {
        throw_crypto_exception(env, "EVP_CIPHER_CTX_new failed",
                               static_cast<jlong>(ERR_get_error()), OP_CTX_INIT);
        return nullptr;
    }

    if (EVP_DecryptInit_ex(ctx.get(), EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1) {
        throw_crypto_exception(env, "EVP_DecryptInit_ex (cipher) failed",
                               static_cast<jlong>(ERR_get_error()), OP_CTX_INIT);
        return nullptr;
    }

    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_IVLEN, GCM_IV_LEN, nullptr) != 1) {
        throw_crypto_exception(env, "EVP_CTRL_GCM_SET_IVLEN failed",
                               static_cast<jlong>(ERR_get_error()), OP_CTX_INIT);
        return nullptr;
    }

    if (EVP_DecryptInit_ex(ctx.get(), nullptr, nullptr,
                            reinterpret_cast<const unsigned char*>(key.data),
                            iv) != 1) {
        throw_crypto_exception(env, "EVP_DecryptInit_ex (key+iv) failed",
                               static_cast<jlong>(ERR_get_error()), OP_DECRYPT);
        return nullptr;
    }

    // Set expected tag BEFORE processing data
    // EVP_CTRL_GCM_SET_TAG takes a non-const void* — copy tag to mutable buffer
    unsigned char tagCopy[GCM_TAG_LEN];
    std::memcpy(tagCopy, tag, GCM_TAG_LEN);
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_TAG,
                             GCM_TAG_LEN, tagCopy) != 1) {
        throw_crypto_exception(env, "EVP_CTRL_GCM_SET_TAG failed",
                               static_cast<jlong>(ERR_get_error()), OP_DECRYPT);
        return nullptr;
    }

    // Provide AAD (if any)
    if (aad.ok() && aad.length > 0) {
        int aadOutLen = 0;
        if (EVP_DecryptUpdate(ctx.get(), nullptr, &aadOutLen,
                               reinterpret_cast<const unsigned char*>(aad.data),
                               static_cast<int>(aad.length)) != 1) {
            throw_crypto_exception(env, "EVP_DecryptUpdate (AAD) failed",
                                   static_cast<jlong>(ERR_get_error()), OP_DECRYPT);
            return nullptr;
        }
    }

    // Decrypt ciphertext
    int decryptedLen = 0;
    if (EVP_DecryptUpdate(ctx.get(), plaintext.get(), &decryptedLen,
                           cipher, static_cast<int>(cipherLen)) != 1) {
        throw_crypto_exception(env, "EVP_DecryptUpdate (ciphertext) failed",
                               static_cast<jlong>(ERR_get_error()), OP_DECRYPT);
        return nullptr;
    }

    // Finalise — this is where GCM authentication tag verification happens.
    // A return value of <= 0 means tag mismatch (tampered/corrupted data).
    int finalLen = 0;
    if (EVP_DecryptFinal_ex(ctx.get(), plaintext.get() + decryptedLen, &finalLen) <= 0) {
        // Zero-out any partial plaintext before throwing
        OPENSSL_cleanse(plaintext.get(), static_cast<size_t>(cipherLen));
        throw_crypto_exception(env,
            "GCM authentication tag verification failed — data may be corrupted or tampered",
            static_cast<jlong>(ERR_get_error()), OP_DECRYPT);
        return nullptr;
    }

    // ── 6. Build result Java byte[] ───────────────────────────────────
    jsize resultLen = static_cast<jsize>(decryptedLen + finalLen);
    jbyteArray result = env->NewByteArray(resultLen);
    if (!result) {
        OPENSSL_cleanse(plaintext.get(), static_cast<size_t>(cipherLen));
        throw_oom(env, "failed to allocate decrypt result array");
        return nullptr;
    }

    env->SetByteArrayRegion(result, 0, resultLen,
                             reinterpret_cast<const jbyte*>(plaintext.get()));

    // Cleanse plaintext buffer before it is freed
    OPENSSL_cleanse(plaintext.get(), static_cast<size_t>(cipherLen));

    return result;
}

// ────────────��────────────────────────────────────────────────────────────────
// deriveKey()
//
// JNI name: Java_com_app_crypto_CryptoNative_deriveKey
// Signature: ([B[B[BI)[B
// ─────────────────────────────────────────────────────────────────────────────
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoNative_deriveKey(JNIEnv*    env,
                                            jobject    /* thiz */,
                                            jbyteArray jIkm,
                                            jbyteArray jSalt,
                                            jbyteArray jInfo,
                                            jint       outputKeyLength) {
    // ── 1. Validate ───────────────────────────────────────────────────
    if (!jIkm) { throw_iae(env, "inputKeyMaterial must not be null"); return nullptr; }

    jsize ikmLen = env->GetArrayLength(jIkm);
    if (ikmLen == 0) { throw_iae(env, "inputKeyMaterial must not be empty"); return nullptr; }

    if (outputKeyLength <= 0 || outputKeyLength > 255 * 32) {
        throw_iae(env, "outputKeyLength must be in [1, 8160]");
        return nullptr;
    }

    // ── 2. Pin arrays ─────────────────────────────────────────────────
    PinnedByteArray ikm(env, jIkm);
    PinnedByteArray salt(env, jSalt);   // may be null — that is valid for HKDF
    PinnedByteArray info(env, jInfo);   // may be null

    if (!ikm.ok()) { throw_oom(env, "failed to pin ikm"); return nullptr; }

    // ── 3. HKDF-SHA-256 ───────────────────────────────────────────────
    EvpPkeyCtxPtr pctx(EVP_PKEY_CTX_new_id(EVP_PKEY_HKDF, nullptr));
    if (!pctx) {
        throw_crypto_exception(env, "EVP_PKEY_CTX_new_id (HKDF) failed",
                               static_cast<jlong>(ERR_get_error()), OP_KEY_DERIV);
        return nullptr;
    }

    if (EVP_PKEY_derive_init(pctx.get()) <= 0) {
        throw_crypto_exception(env, "EVP_PKEY_derive_init failed",
                               static_cast<jlong>(ERR_get_error()), OP_KEY_DERIV);
        return nullptr;
    }

    if (EVP_PKEY_CTX_set_hkdf_md(pctx.get(), EVP_sha256()) <= 0) {
        throw_crypto_exception(env, "EVP_PKEY_CTX_set_hkdf_md failed",
                               static_cast<jlong>(ERR_get_error()), OP_KEY_DERIV);
        return nullptr;
    }

    // Salt (optional — OpenSSL uses a zero-filled salt if none provided,
    //  matching RFC 5869 § 2.2)
    if (salt.ok() && salt.length > 0) {
        if (EVP_PKEY_CTX_set1_hkdf_salt(
                pctx.get(),
                reinterpret_cast<const unsigned char*>(salt.data),
                static_cast<int>(salt.length)) <= 0) {
            throw_crypto_exception(env, "EVP_PKEY_CTX_set1_hkdf_salt failed",
                                   static_cast<jlong>(ERR_get_error()), OP_KEY_DERIV);
            return nullptr;
        }
    }

    if (EVP_PKEY_CTX_set1_hkdf_key(
            pctx.get(),
            reinterpret_cast<const unsigned char*>(ikm.data),
            static_cast<int>(ikmLen)) <= 0) {
        throw_crypto_exception(env, "EVP_PKEY_CTX_set1_hkdf_key failed",
                               static_cast<jlong>(ERR_get_error()), OP_KEY_DERIV);
        return nullptr;
    }

    // Info (optional context / application label)
    if (info.ok() && info.length > 0) {
        if (EVP_PKEY_CTX_add1_hkdf_info(
                pctx.get(),
                reinterpret_cast<const unsigned char*>(info.data),
                static_cast<int>(info.length)) <= 0) {
            throw_crypto_exception(env, "EVP_PKEY_CTX_add1_hkdf_info failed",
                                   static_cast<jlong>(ERR_get_error()), OP_KEY_DERIV);
            return nullptr;
        }
    }

    auto   derivedKey = std::make_unique<unsigned char[]>(static_cast<size_t>(outputKeyLength));
    size_t derivedLen = static_cast<size_t>(outputKeyLength);

    if (EVP_PKEY_derive(pctx.get(), derivedKey.get(), &derivedLen) <= 0) {
        throw_crypto_exception(env, "EVP_PKEY_derive (HKDF) failed",
                               static_cast<jlong>(ERR_get_error()), OP_KEY_DERIV);
        return nullptr;
    }

    // ── 4. Build result Java byte[] ───────────────────────────────────
    jbyteArray result = env->NewByteArray(static_cast<jsize>(derivedLen));
    if (!result) {
        OPENSSL_cleanse(derivedKey.get(), derivedLen);
        throw_oom(env, "failed to allocate deriveKey result array");
        return nullptr;
    }

    env->SetByteArrayRegion(result, 0, static_cast<jsize>(derivedLen),
                             reinterpret_cast<const jbyte*>(derivedKey.get()));

    OPENSSL_cleanse(derivedKey.get(), derivedLen);
    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// generateSecureRandom()
//
// JNI name: Java_com_app_crypto_CryptoNative_generateSecureRandom
// Signature: (I)[B
// ─────────────────────────────────────────────────────────────────────────────
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoNative_generateSecureRandom(JNIEnv* env,
                                                       jobject /* thiz */,
                                                       jint    count) {
    if (count <= 0) {
        throw_iae(env, "count must be > 0");
        return nullptr;
    }

    auto buf = std::make_unique<unsigned char[]>(static_cast<size_t>(count));

    if (RAND_bytes(buf.get(), count) != 1) {
        throw_crypto_exception(env, "RAND_bytes failed",
                               static_cast<jlong>(ERR_get_error()), OP_UNKNOWN);
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(count);
    if (!result) { throw_oom(env, "failed to allocate random byte array"); return nullptr; }

    env->SetByteArrayRegion(result, 0, count,
                             reinterpret_cast<const jbyte*>(buf.get()));

    OPENSSL_cleanse(buf.get(), static_cast<size_t>(count));
    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// openSSLVersion()
//
// JNI name: Java_com_app_crypto_CryptoNative_openSSLVersion
// Signature: ()Ljava/lang/String;
// ─────────────────────────────────────────────────────────────────────────────
extern "C"
JNIEXPORT jstring JNICALL
Java_com_app_crypto_CryptoNative_openSSLVersion(JNIEnv* env, jobject /* thiz */) {
    return env->NewStringUTF(OpenSSL_version(OPENSSL_VERSION));
}