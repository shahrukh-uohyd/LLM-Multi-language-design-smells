/**
 * crypto_processor.cpp
 *
 * JNI implementation of com.app.crypto.CryptoProcessor.
 *
 * Algorithms  : AES-256-GCM (AEAD), SHA-256/512, HMAC-SHA256, PBKDF2-SHA256
 * Dependency  : OpenSSL >= 1.1.0   (link with -lssl -lcrypto)
 * C++ standard: C++17
 *
 * Compile (Linux):
 *   g++ -std=c++17 -shared -fPIC -O2 -Wall -Wextra             \
 *       -I"${JAVA_HOME}/include"                                 \
 *       -I"${JAVA_HOME}/include/linux"                           \
 *       crypto_processor.cpp                                     \
 *       -lssl -lcrypto                                           \
 *       -o libcrypto_processor.so
 *
 * AES-256-GCM wire format:
 *   ┌─────────────┬──────────────────────────┬────────────────┐
 *   │  IV  (12 B) │  Ciphertext  (variable)  │  GCM Tag (16 B)│
 *   └─────────────┴──────────────────────────┴────────────────┘
 */

#include <jni.h>

#include <openssl/evp.h>
#include <openssl/rand.h>
#include <openssl/hmac.h>
#include <openssl/err.h>
#include <openssl/sha.h>
#include <openssl/crypto.h>

#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <memory>
#include <string>

// ── Algorithm constants ────────────────────────────────────────────────────
static constexpr int AES_KEY_LEN  = 32;   // AES-256
static constexpr int GCM_IV_LEN   = 12;   // 96-bit nonce (NIST SP 800-38D)
static constexpr int GCM_TAG_LEN  = 16;   // 128-bit authentication tag
static constexpr int SHA256_LEN   = 32;
static constexpr int SHA512_LEN   = 64;
static constexpr int HMAC256_LEN  = 32;

// ── Hash algorithm selectors (must match CryptoProcessor.java constants) ──
static constexpr int HASH_SHA256  = 0;
static constexpr int HASH_SHA512  = 1;

// ── Operation ordinals (must mirror CryptoException.Operation enum order) ─
static constexpr jint OP_ENCRYPT     = 0;
static constexpr jint OP_DECRYPT     = 1;
static constexpr jint OP_HASH        = 2;
static constexpr jint OP_HMAC        = 3;
static constexpr jint OP_KEY_DERIV   = 4;
static constexpr jint OP_RANDOM      = 5;
static constexpr jint OP_CTX_INIT    = 6;
static constexpr jint OP_UNKNOWN     = 7;

// ── JNI class paths ────────────────────────────────────────────────────────
static constexpr const char* CRYPTO_EXCEPTION = "com/app/crypto/CryptoException";
static constexpr const char* IAE_CLASS        = "java/lang/IllegalArgumentException";
static constexpr const char* OOM_CLASS        = "java/lang/OutOfMemoryError";

// ═════════════════════════════════════════════════════════════════════════════
// RAII wrappers
// ═════════════════════════════════════════════════════════════════════════════

struct EvpCipherCtxDeleter {
    void operator()(EVP_CIPHER_CTX* p) const noexcept { EVP_CIPHER_CTX_free(p); }
};
using EvpCipherCtxPtr = std::unique_ptr<EVP_CIPHER_CTX, EvpCipherCtxDeleter>;

struct EvpMdCtxDeleter {
    void operator()(EVP_MD_CTX* p) const noexcept { EVP_MD_CTX_free(p); }
};
using EvpMdCtxPtr = std::unique_ptr<EVP_MD_CTX, EvpMdCtxDeleter>;

// ═════════════════════════════════════════════════════════════════════════════
// JNI pinned byte-array helper (RAII)
// ═════════════════════════════════════════════════════════════════════════════

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
    bool ok()    const noexcept { return data != nullptr; }
    bool empty() const noexcept { return length == 0; }

    PinnedByteArray(const PinnedByteArray&)            = delete;
    PinnedByteArray& operator=(const PinnedByteArray&) = delete;
};

// ═════════════════════════════════════════════════════════════════════════════
// Exception helpers
// ═════════════════════════════════════════════════════════════════════════════

static void throw_iae(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass(IAE_CLASS);
    if (cls) { env->ThrowNew(cls, msg); env->DeleteLocalRef(cls); }
}

static void throw_oom(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass(OOM_CLASS);
    if (cls) { env->ThrowNew(cls, msg); env->DeleteLocalRef(cls); }
}

/** Returns the current OpenSSL error string (clears the error queue). */
static std::string openssl_err_str() {
    char buf[256] = {};
    unsigned long e = ERR_get_error();
    if (e == 0) return "no OpenSSL error queued";
    ERR_error_string_n(e, buf, sizeof(buf));
    return std::string(buf);
}

/**
 * Throws CryptoException(String message, long opensslErrorCode, int operationOrdinal).
 * Mirrors the 3-argument constructor in CryptoException.java.
 */
static void throw_crypto_exception(JNIEnv*     env,
                                    const char* context,
                                    jlong       opensslCode,
                                    jint        opOrdinal) {
    jclass cls = env->FindClass(CRYPTO_EXCEPTION);
    if (!cls) return;

    jmethodID ctor = env->GetMethodID(cls, "<init>", "(Ljava/lang/String;JI)V");
    if (!ctor) { env->DeleteLocalRef(cls); return; }

    std::string detail = std::string(context) + ": " + openssl_err_str();
    jstring jmsg = env->NewStringUTF(detail.c_str());
    if (!jmsg) { env->DeleteLocalRef(cls); return; }

    jobject ex = env->NewObject(cls, ctor, jmsg, opensslCode, opOrdinal);
    if (ex) { env->Throw(static_cast<jthrowable>(ex)); env->DeleteLocalRef(ex); }

    env->DeleteLocalRef(jmsg);
    env->DeleteLocalRef(cls);
}

/** Convenience overload that extracts the current OpenSSL error code automatically. */
static void throw_crypto_exception(JNIEnv* env, const char* context, jint opOrdinal) {
    throw_crypto_exception(env, context, static_cast<jlong>(ERR_get_error()), opOrdinal);
}

// ═════════════════════════════════════════════════════════════════════════════
// Helper: build a Java byte[] from a native buffer
// ═════════════════════════════════════════════════════════════════════════════

static jbyteArray make_byte_array(JNIEnv* env, const void* data, jsize len) {
    jbyteArray arr = env->NewByteArray(len);
    if (!arr) { throw_oom(env, "failed to allocate result byte[]"); return nullptr; }
    env->SetByteArrayRegion(arr, 0, len, reinterpret_cast<const jbyte*>(data));
    return arr;
}

// ═════════════════════════════════════════════════════════════════════════════
// encrypt
// Java_com_app_crypto_CryptoProcessor_encrypt
// ([B[B[B)[B
// ═════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoProcessor_encrypt(JNIEnv*    env,
                                             jobject    /*thiz*/,
                                             jbyteArray jKey,
                                             jbyteArray jPlaintext,
                                             jbyteArray jAad) {
    // ── 1. Argument validation ─────────────────────────────────────────
    if (!jKey)       { throw_iae(env, "key must not be null");       return nullptr; }
    if (!jPlaintext) { throw_iae(env, "plaintext must not be null"); return nullptr; }

    if (env->GetArrayLength(jKey) != AES_KEY_LEN) {
        throw_iae(env, "AES-256 key must be 32 bytes"); return nullptr;
    }
    jsize ptLen = env->GetArrayLength(jPlaintext);
    if (ptLen == 0) { throw_iae(env, "plaintext must not be empty"); return nullptr; }

    // ── 2. Pin Java arrays ─────────────────────────────────────────────
    PinnedByteArray key(env, jKey);
    PinnedByteArray pt(env, jPlaintext);
    if (!key.ok() || !pt.ok()) {
        throw_oom(env, "failed to pin encrypt input arrays"); return nullptr;
    }
    PinnedByteArray aad(env, jAad);   // jAad may be null — that is fine

    // ── 3. Generate random IV ──────────────────────────────────────────
    unsigned char iv[GCM_IV_LEN];
    if (RAND_bytes(iv, GCM_IV_LEN) != 1) {
        throw_crypto_exception(env, "RAND_bytes (IV) failed", OP_ENCRYPT);
        return nullptr;
    }

    // ── 4. Allocate output: IV + ciphertext + tag ──────────────────────
    jsize outLen = GCM_IV_LEN + ptLen + GCM_TAG_LEN;
    auto  output = std::make_unique<unsigned char[]>(static_cast<size_t>(outLen));

    unsigned char* ivDst     = output.get();
    unsigned char* cipherDst = output.get() + GCM_IV_LEN;
    unsigned char* tagDst    = cipherDst + ptLen;

    std::memcpy(ivDst, iv, GCM_IV_LEN);

    // ── 5. AES-256-GCM encrypt ─────────────────────────────────────────
    EvpCipherCtxPtr ctx(EVP_CIPHER_CTX_new());
    if (!ctx) {
        throw_crypto_exception(env, "EVP_CIPHER_CTX_new failed", OP_CTX_INIT);
        return nullptr;
    }

    if (EVP_EncryptInit_ex(ctx.get(), EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1) {
        throw_crypto_exception(env, "EVP_EncryptInit_ex (cipher) failed", OP_CTX_INIT);
        return nullptr;
    }
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_IVLEN, GCM_IV_LEN, nullptr) != 1) {
        throw_crypto_exception(env, "EVP_CTRL_GCM_SET_IVLEN failed", OP_CTX_INIT);
        return nullptr;
    }
    if (EVP_EncryptInit_ex(ctx.get(), nullptr, nullptr,
                            reinterpret_cast<const unsigned char*>(key.data), iv) != 1) {
        throw_crypto_exception(env, "EVP_EncryptInit_ex (key+iv) failed", OP_ENCRYPT);
        return nullptr;
    }

    // Optional AAD
    if (aad.ok() && aad.length > 0) {
        int unused = 0;
        if (EVP_EncryptUpdate(ctx.get(), nullptr, &unused,
                               reinterpret_cast<const unsigned char*>(aad.data),
                               static_cast<int>(aad.length)) != 1) {
            throw_crypto_exception(env, "EVP_EncryptUpdate (AAD) failed", OP_ENCRYPT);
            return nullptr;
        }
    }

    // Encrypt plaintext
    int encLen = 0;
    if (EVP_EncryptUpdate(ctx.get(), cipherDst, &encLen,
                           reinterpret_cast<const unsigned char*>(pt.data),
                           static_cast<int>(ptLen)) != 1) {
        throw_crypto_exception(env, "EVP_EncryptUpdate (plaintext) failed", OP_ENCRYPT);
        return nullptr;
    }

    int finalLen = 0;
    if (EVP_EncryptFinal_ex(ctx.get(), cipherDst + encLen, &finalLen) != 1) {
        throw_crypto_exception(env, "EVP_EncryptFinal_ex failed", OP_ENCRYPT);
        return nullptr;
    }

    // Extract GCM tag
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_GET_TAG, GCM_TAG_LEN, tagDst) != 1) {
        throw_crypto_exception(env, "EVP_CTRL_GCM_GET_TAG failed", OP_ENCRYPT);
        return nullptr;
    }

    return make_byte_array(env, output.get(), outLen);
}

// ═════════════════════════════════════════════════════════════════════════════
// decrypt
// Java_com_app_crypto_CryptoProcessor_decrypt
// ([B[B[B)[B
// ═════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoProcessor_decrypt(JNIEnv*    env,
                                             jobject    /*thiz*/,
                                             jbyteArray jKey,
                                             jbyteArray jCipherBlob,
                                             jbyteArray jAad) {
    // ── 1. Validate ────────────────────────────────────────────────────
    if (!jKey)        { throw_iae(env, "key must not be null");        return nullptr; }
    if (!jCipherBlob) { throw_iae(env, "cipherBlob must not be null"); return nullptr; }

    if (env->GetArrayLength(jKey) != AES_KEY_LEN) {
        throw_iae(env, "AES-256 key must be 32 bytes"); return nullptr;
    }

    jsize blobLen = env->GetArrayLength(jCipherBlob);
    jsize minLen  = GCM_IV_LEN + 1 + GCM_TAG_LEN;
    if (blobLen < minLen) {
        char msg[128];
        std::snprintf(msg, sizeof(msg),
                      "cipherBlob too short: need >= %d bytes, got %d", (int)minLen, (int)blobLen);
        throw_iae(env, msg); return nullptr;
    }

    // ── 2. Pin arrays ──────────────────────────────────────────────────
    PinnedByteArray key(env, jKey);
    PinnedByteArray blob(env, jCipherBlob);
    if (!key.ok() || !blob.ok()) {
        throw_oom(env, "failed to pin decrypt input arrays"); return nullptr;
    }
    PinnedByteArray aad(env, jAad);

    // ── 3. Split blob → IV | ciphertext | tag ──────────────────────────
    const auto*          blobBytes  = reinterpret_cast<const unsigned char*>(blob.data);
    const unsigned char* ivPtr      = blobBytes;
    const unsigned char* cipherPtr  = blobBytes + GCM_IV_LEN;
    jsize                cipherLen  = blobLen - GCM_IV_LEN - GCM_TAG_LEN;
    const unsigned char* tagPtr     = blobBytes + GCM_IV_LEN + cipherLen;

    // ── 4. Allocate plaintext buffer ───────────────────────────────────
    auto plain = std::make_unique<unsigned char[]>(static_cast<size_t>(cipherLen));

    // ── 5. AES-256-GCM decrypt ─────────────────────────────────────────
    EvpCipherCtxPtr ctx(EVP_CIPHER_CTX_new());
    if (!ctx) {
        throw_crypto_exception(env, "EVP_CIPHER_CTX_new failed", OP_CTX_INIT);
        return nullptr;
    }

    if (EVP_DecryptInit_ex(ctx.get(), EVP_aes_256_gcm(), nullptr, nullptr, nullptr) != 1) {
        throw_crypto_exception(env, "EVP_DecryptInit_ex (cipher) failed", OP_CTX_INIT);
        return nullptr;
    }
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_IVLEN, GCM_IV_LEN, nullptr) != 1) {
        throw_crypto_exception(env, "EVP_CTRL_GCM_SET_IVLEN failed", OP_CTX_INIT);
        return nullptr;
    }
    if (EVP_DecryptInit_ex(ctx.get(), nullptr, nullptr,
                            reinterpret_cast<const unsigned char*>(key.data), ivPtr) != 1) {
        throw_crypto_exception(env, "EVP_DecryptInit_ex (key+iv) failed", OP_DECRYPT);
        return nullptr;
    }

    // Set expected tag BEFORE decrypting data
    unsigned char tagCopy[GCM_TAG_LEN];
    std::memcpy(tagCopy, tagPtr, GCM_TAG_LEN);
    if (EVP_CIPHER_CTX_ctrl(ctx.get(), EVP_CTRL_GCM_SET_TAG,
                             GCM_TAG_LEN, tagCopy) != 1) {
        throw_crypto_exception(env, "EVP_CTRL_GCM_SET_TAG failed", OP_DECRYPT);
        return nullptr;
    }

    // Optional AAD
    if (aad.ok() && aad.length > 0) {
        int unused = 0;
        if (EVP_DecryptUpdate(ctx.get(), nullptr, &unused,
                               reinterpret_cast<const unsigned char*>(aad.data),
                               static_cast<int>(aad.length)) != 1) {
            throw_crypto_exception(env, "EVP_DecryptUpdate (AAD) failed", OP_DECRYPT);
            return nullptr;
        }
    }

    // Decrypt ciphertext
    int decLen = 0;
    if (EVP_DecryptUpdate(ctx.get(), plain.get(), &decLen,
                           cipherPtr, static_cast<int>(cipherLen)) != 1) {
        throw_crypto_exception(env, "EVP_DecryptUpdate (ciphertext) failed", OP_DECRYPT);
        return nullptr;
    }

    // Finalise — GCM tag verification happens here.
    // A return value <= 0 means authentication failure.
    int finalLen = 0;
    if (EVP_DecryptFinal_ex(ctx.get(), plain.get() + decLen, &finalLen) <= 0) {
        OPENSSL_cleanse(plain.get(), static_cast<size_t>(cipherLen));
        throw_crypto_exception(env,
            "GCM authentication tag verification FAILED — "
            "data may be tampered, corrupted, or the wrong key/AAD was used",
            static_cast<jlong>(ERR_get_error()), OP_DECRYPT);
        return nullptr;
    }

    // ── 6. Build result ────────────────────────────────────────────────
    jsize resultLen = static_cast<jsize>(decLen + finalLen);
    jbyteArray result = make_byte_array(env, plain.get(), resultLen);
    OPENSSL_cleanse(plain.get(), static_cast<size_t>(cipherLen));
    return result;
}

// ═════════════════════════════════════════════════════════════════════════════
// computeHash
// Java_com_app_crypto_CryptoProcessor_computeHash
// ([BI)[B
// ═════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoProcessor_computeHash(JNIEnv*    env,
                                                 jobject    /*thiz*/,
                                                 jbyteArray jData,
                                                 jint       hashAlgorithm) {
    if (!jData) { throw_iae(env, "data must not be null"); return nullptr; }
    if (hashAlgorithm != HASH_SHA256 && hashAlgorithm != HASH_SHA512) {
        throw_iae(env, "hashAlgorithm must be 0 (SHA-256) or 1 (SHA-512)");
        return nullptr;
    }

    PinnedByteArray data(env, jData);
    if (!data.ok())   { throw_oom(env, "failed to pin data"); return nullptr; }
    if (data.empty()) { throw_iae(env, "data must not be empty"); return nullptr; }

    const EVP_MD* md     = (hashAlgorithm == HASH_SHA256) ? EVP_sha256() : EVP_sha512();
    int           mdSize = (hashAlgorithm == HASH_SHA256) ? SHA256_LEN   : SHA512_LEN;

    EvpMdCtxPtr ctx(EVP_MD_CTX_new());
    if (!ctx) {
        throw_crypto_exception(env, "EVP_MD_CTX_new failed", OP_CTX_INIT);
        return nullptr;
    }

    unsigned char digest[SHA512_LEN] = {};  // large enough for both
    unsigned int  digestLen = 0;

    if (EVP_DigestInit_ex(ctx.get(), md, nullptr) != 1 ||
        EVP_DigestUpdate(ctx.get(),
                          reinterpret_cast<const unsigned char*>(data.data),
                          static_cast<size_t>(data.length)) != 1 ||
        EVP_DigestFinal_ex(ctx.get(), digest, &digestLen) != 1) {
        throw_crypto_exception(env, "EVP_Digest* failed", OP_HASH);
        return nullptr;
    }

    return make_byte_array(env, digest, static_cast<jsize>(digestLen));
}

// ═════════════════════════════════════════════════════════════════════════════
// computeHmacSha256
// Java_com_app_crypto_CryptoProcessor_computeHmacSha256
// ([B[B)[B
// ═════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoProcessor_computeHmacSha256(JNIEnv*    env,
                                                        jobject    /*thiz*/,
                                                        jbyteArray jKey,
                                                        jbyteArray jData) {
    if (!jKey)  { throw_iae(env, "key must not be null");  return nullptr; }
    if (!jData) { throw_iae(env, "data must not be null"); return nullptr; }

    PinnedByteArray key(env, jKey);
    PinnedByteArray data(env, jData);
    if (!key.ok() || !data.ok()) {
        throw_oom(env, "failed to pin HMAC arrays"); return nullptr;
    }
    if (key.empty())  { throw_iae(env, "key must not be empty");  return nullptr; }
    if (data.empty()) { throw_iae(env, "data must not be empty"); return nullptr; }

    unsigned char mac[HMAC256_LEN] = {};
    unsigned int  macLen           = 0;

    unsigned char* result = HMAC(
        EVP_sha256(),
        reinterpret_cast<const unsigned char*>(key.data),
        static_cast<int>(key.length),
        reinterpret_cast<const unsigned char*>(data.data),
        static_cast<size_t>(data.length),
        mac,
        &macLen);

    if (!result) {
        throw_crypto_exception(env, "HMAC-SHA256 failed", OP_HMAC);
        return nullptr;
    }

    return make_byte_array(env, mac, static_cast<jsize>(macLen));
}

// ═════════════════════════════════════════════════════════════════════════════
// deriveKeyPBKDF2
// Java_com_app_crypto_CryptoProcessor_deriveKeyPBKDF2
// ([B[BII)[B
// ═════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoProcessor_deriveKeyPBKDF2(JNIEnv*    env,
                                                     jobject    /*thiz*/,
                                                     jbyteArray jPassword,
                                                     jbyteArray jSalt,
                                                     jint       iterations,
                                                     jint       outputKeyLength) {
    // ── 1. Validate ────────────────────────────────────────────────────
    if (!jPassword) { throw_iae(env, "password must not be null"); return nullptr; }
    if (!jSalt)     { throw_iae(env, "salt must not be null");     return nullptr; }
    if (iterations <= 0) {
        throw_iae(env, "iterations must be > 0"); return nullptr;
    }
    if (outputKeyLength <= 0 || outputKeyLength > 1024) {
        throw_iae(env, "outputKeyLength must be in [1, 1024]"); return nullptr;
    }

    PinnedByteArray pass(env, jPassword);
    PinnedByteArray salt(env, jSalt);
    if (!pass.ok() || !salt.ok()) {
        throw_oom(env, "failed to pin PBKDF2 arrays"); return nullptr;
    }
    if (pass.empty()) { throw_iae(env, "password must not be empty"); return nullptr; }

    // ── 2. PBKDF2-HMAC-SHA256 ─────────────────────────────────────────
    auto derived = std::make_unique<unsigned char[]>(static_cast<size_t>(outputKeyLength));

    int rc = PKCS5_PBKDF2_HMAC(
        reinterpret_cast<const char*>(pass.data),
        static_cast<int>(pass.length),
        reinterpret_cast<const unsigned char*>(salt.data),
        static_cast<int>(salt.length),
        iterations,
        EVP_sha256(),
        outputKeyLength,
        derived.get());

    if (rc != 1) {
        OPENSSL_cleanse(derived.get(), static_cast<size_t>(outputKeyLength));
        throw_crypto_exception(env, "PKCS5_PBKDF2_HMAC failed", OP_KEY_DERIV);
        return nullptr;
    }

    jbyteArray result = make_byte_array(env, derived.get(),
                                         static_cast<jsize>(outputKeyLength));
    OPENSSL_cleanse(derived.get(), static_cast<size_t>(outputKeyLength));
    return result;
}

// ═════════════════════════════════════════════════════════════════════════════
// generateSecureRandom
// Java_com_app_crypto_CryptoProcessor_generateSecureRandom
// (I)[B
// ═════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_app_crypto_CryptoProcessor_generateSecureRandom(JNIEnv* env,
                                                          jobject /*thiz*/,
                                                          jint    count) {
    if (count <= 0) { throw_iae(env, "count must be > 0"); return nullptr; }

    auto buf = std::make_unique<unsigned char[]>(static_cast<size_t>(count));

    if (RAND_bytes(buf.get(), count) != 1) {
        throw_crypto_exception(env, "RAND_bytes failed", OP_RANDOM);
        return nullptr;
    }

    jbyteArray result = make_byte_array(env, buf.get(), count);
    OPENSSL_cleanse(buf.get(), static_cast<size_t>(count));
    return result;
}

// ═════════════════════════════════════════════════════════════════════════════
// openSSLVersion
// Java_com_app_crypto_CryptoProcessor_openSSLVersion
// ()Ljava/lang/String;
// ═════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jstring JNICALL
Java_com_app_crypto_CryptoProcessor_openSSLVersion(JNIEnv* env, jobject /*thiz*/) {
    return env->NewStringUTF(OpenSSL_version(OPENSSL_VERSION));
}