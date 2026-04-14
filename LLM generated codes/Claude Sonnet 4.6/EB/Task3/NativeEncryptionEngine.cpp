#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <cstring>
#include <stdexcept>
#include <iostream>
#include <algorithm>

// ════════════════════════════════════════════════════════════════════════════
//  Native Encryption Engine State
//  (static / process-global for this demo; use a handle map for multi-instance)
// ════════════════════════════════════════════════════════════════════════════
struct EngineState {
    std::vector<uint8_t> keyMaterial;   // Raw key bytes copied from Java
    std::string          algorithm;     // "AES_128" or "AES_256"
    std::string          keyId;         // Key label / identifier
    bool                 initialized;   // True after a successful initializeEngine()

    EngineState() : initialized(false) {}

    // Securely wipe key material on teardown
    void wipe() {
        std::fill(keyMaterial.begin(), keyMaterial.end(), 0);
        keyMaterial.clear();
        algorithm.clear();
        keyId.clear();
        initialized = false;
    }
};

static EngineState g_engine;   // Single global engine instance

// ════════════════════════════════════════════════════════════════════════════
//  JNI Utility Helpers
// ════════════════════════════════════════════════════════════════════════════

/** Throw a Java RuntimeException from C++. */
static void throwJavaException(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls) env->ThrowNew(cls, msg);
}

/** Convert jstring → std::string. Returns "" if jstr is null. */
static std::string jstringToStd(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* raw = env->GetStringUTFChars(jstr, nullptr);
    std::string result(raw);
    env->ReleaseStringUTFChars(jstr, raw);
    return result;
}

/** Convert std::string → jstring. */
static jstring stdToJstring(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

// ════════════════════════════════════════════════════════════════════════════
//  SecurityKey Reader
//  Extracts all fields from the Java SecurityKey object into native structs.
// ════════════════════════════════════════════════════════════════════════════
struct SecurityKeyData {
    std::vector<uint8_t> keyBytes;
    std::string          algorithm;
    std::string          keyId;
    bool                 revoked;
};

static SecurityKeyData readSecurityKey(JNIEnv* env, jobject keyObj) {
    if (!keyObj) throw std::runtime_error("SecurityKey object is null");

    SecurityKeyData data{};

    jclass keyClass = env->GetObjectClass(keyObj);
    if (!keyClass) throw std::runtime_error("Cannot resolve SecurityKey class");

    // ── isRevoked() ──────────────────────────────────────────────────────
    jmethodID isRevoked = env->GetMethodID(keyClass, "isRevoked", "()Z");
    if (!isRevoked) throw std::runtime_error("Cannot find isRevoked()");
    data.revoked = (env->CallBooleanMethod(keyObj, isRevoked) == JNI_TRUE);

    // ── getAlgorithm() ───────────────────────────────────────────────────
    jmethodID getAlgorithm =
        env->GetMethodID(keyClass, "getAlgorithm", "()Ljava/lang/String;");
    if (!getAlgorithm) throw std::runtime_error("Cannot find getAlgorithm()");
    data.algorithm =
        jstringToStd(env, (jstring)env->CallObjectMethod(keyObj, getAlgorithm));

    // ── getKeyId() ───────────────────────────────────────────────────────
    jmethodID getKeyId =
        env->GetMethodID(keyClass, "getKeyId", "()Ljava/lang/String;");
    if (!getKeyId) throw std::runtime_error("Cannot find getKeyId()");
    data.keyId =
        jstringToStd(env, (jstring)env->CallObjectMethod(keyObj, getKeyId));

    // ── getKeyBytes()  →  byte[] ─────────────────────────────────────────
    jmethodID getKeyBytes =
        env->GetMethodID(keyClass, "getKeyBytes", "()[B");
    if (!getKeyBytes) throw std::runtime_error("Cannot find getKeyBytes()");

    auto jKeyBytes =
        (jbyteArray)env->CallObjectMethod(keyObj, getKeyBytes);
    if (!jKeyBytes) throw std::runtime_error("getKeyBytes() returned null");

    jsize len    = env->GetArrayLength(jKeyBytes);
    jbyte* elems = env->GetByteArrayElements(jKeyBytes, nullptr);
    if (!elems)  throw std::runtime_error("Failed to access key byte array");

    data.keyBytes.assign(
        reinterpret_cast<uint8_t*>(elems),
        reinterpret_cast<uint8_t*>(elems) + len
    );

    // Release with JNI_ABORT so we don't write back (read-only access)
    // and to avoid leaving raw key bytes in the JVM's pinned array buffer.
    env->ReleaseByteArrayElements(jKeyBytes, elems, JNI_ABORT);

    return data;
}

// ════════════════════════════════════════════════════════════════════════════
//  Validate Key Length Against Algorithm
// ════════════════════════════════════════════════════════════════════════════
static void validateKeyLength(const SecurityKeyData& keyData) {
    size_t len = keyData.keyBytes.size();
    if (keyData.algorithm == "AES_128" && len != 16)
        throw std::runtime_error(
            "AES_128 requires a 16-byte key, got " + std::to_string(len));
    if (keyData.algorithm == "AES_256" && len != 32)
        throw std::runtime_error(
            "AES_256 requires a 32-byte key, got " + std::to_string(len));
}

// ════════════════════════════════════════════════════════════════════════════
//  Minimal Stand-in Cipher (XOR stream)
//  Replace the body of nativeEncryptDecrypt() with OpenSSL AES-CBC/GCM
//  calls to use real cryptography.
// ════════════════════════════════════════════════════════════════════════════
static std::vector<uint8_t> nativeEncryptDecrypt(
        const std::vector<uint8_t>& input,
        const std::vector<uint8_t>& key)
{
    std::vector<uint8_t> output(input.size());
    for (size_t i = 0; i < input.size(); ++i)
        output[i] = input[i] ^ key[i % key.size()];
    return output;
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: EncryptionEngine.initializeEngine(SecurityKey securityKey)
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jboolean JNICALL
Java_EncryptionEngine_initializeEngine(JNIEnv* env,
                                       jobject /*thisObj*/,
                                       jobject securityKeyObj)
{
    try {
        // Wipe any previous engine state first
        g_engine.wipe();

        // ── Read the Java SecurityKey ────────────────────────────────────
        SecurityKeyData keyData = readSecurityKey(env, securityKeyObj);

        // ── Guard: revoked key ───────────────────────────────────────────
        if (keyData.revoked) {
            std::cerr << "[NATIVE] Initialization refused: key '"
                      << keyData.keyId << "' has been revoked.\n";
            return JNI_FALSE;
        }

        // ── Guard: unsupported algorithm ─────────────────────────────────
        if (keyData.algorithm != "AES_128" && keyData.algorithm != "AES_256") {
            throw std::runtime_error(
                "Unsupported algorithm: " + keyData.algorithm);
        }

        // ── Guard: correct key length ────────────────────────────────────
        validateKeyLength(keyData);

        // ── Copy key material into the engine ────────────────────────────
        g_engine.keyMaterial  = std::move(keyData.keyBytes);
        g_engine.algorithm    = keyData.algorithm;
        g_engine.keyId        = keyData.keyId;
        g_engine.initialized  = true;

        std::cout << "[NATIVE] Engine initialized.\n"
                  << "         Key ID    : " << g_engine.keyId      << "\n"
                  << "         Algorithm : " << g_engine.algorithm   << "\n"
                  << "         Key Length: " << g_engine.keyMaterial.size()
                                             << " bytes\n";

        return JNI_TRUE;

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return JNI_FALSE;
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: EncryptionEngine.encrypt(byte[] plaintext)
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_EncryptionEngine_encrypt(JNIEnv* env,
                              jobject /*thisObj*/,
                              jbyteArray jPlaintext)
{
    try {
        if (!g_engine.initialized)
            throw std::runtime_error("Engine is not initialized. Call initializeEngine() first.");
        if (!jPlaintext)
            throw std::runtime_error("Plaintext byte array is null.");

        // ── Read plaintext from Java ─────────────────────────────────────
        jsize  len   = env->GetArrayLength(jPlaintext);
        jbyte* elems = env->GetByteArrayElements(jPlaintext, nullptr);
        if (!elems)  throw std::runtime_error("Failed to access plaintext array.");

        std::vector<uint8_t> plaintext(
            reinterpret_cast<uint8_t*>(elems),
            reinterpret_cast<uint8_t*>(elems) + len);
        env->ReleaseByteArrayElements(jPlaintext, elems, JNI_ABORT);

        // ── Encrypt ──────────────────────────────────────────────────────
        std::vector<uint8_t> ciphertext =
            nativeEncryptDecrypt(plaintext, g_engine.keyMaterial);

        std::cout << "[NATIVE] Encrypted " << len << " bytes → "
                  << ciphertext.size() << " bytes ciphertext.\n";

        // ── Return ciphertext as Java byte[] ─────────────────────────────
        jbyteArray jCiphertext = env->NewByteArray((jsize)ciphertext.size());
        if (!jCiphertext) throw std::runtime_error("Failed to allocate output byte array.");
        env->SetByteArrayRegion(jCiphertext, 0, (jsize)ciphertext.size(),
                                reinterpret_cast<const jbyte*>(ciphertext.data()));
        return jCiphertext;

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return nullptr;
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: EncryptionEngine.decrypt(byte[] ciphertext)
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_EncryptionEngine_decrypt(JNIEnv* env,
                              jobject /*thisObj*/,
                              jbyteArray jCiphertext)
{
    try {
        if (!g_engine.initialized)
            throw std::runtime_error("Engine is not initialized. Call initializeEngine() first.");
        if (!jCiphertext)
            throw std::runtime_error("Ciphertext byte array is null.");

        // ── Read ciphertext from Java ────────────────────────────────────
        jsize  len   = env->GetArrayLength(jCiphertext);
        jbyte* elems = env->GetByteArrayElements(jCiphertext, nullptr);
        if (!elems) throw std::runtime_error("Failed to access ciphertext array.");

        std::vector<uint8_t> ciphertext(
            reinterpret_cast<uint8_t*>(elems),
            reinterpret_cast<uint8_t*>(elems) + len);
        env->ReleaseByteArrayElements(jCiphertext, elems, JNI_ABORT);

        // ── Decrypt (symmetric: same XOR operation) ──────────────────────
        std::vector<uint8_t> plaintext =
            nativeEncryptDecrypt(ciphertext, g_engine.keyMaterial);

        std::cout << "[NATIVE] Decrypted " << len << " bytes → "
                  << plaintext.size() << " bytes plaintext.\n";

        // ── Return plaintext as Java byte[] ──────────────────────────────
        jbyteArray jPlaintext = env->NewByteArray((jsize)plaintext.size());
        if (!jPlaintext) throw std::runtime_error("Failed to allocate output byte array.");
        env->SetByteArrayRegion(jPlaintext, 0, (jsize)plaintext.size(),
                                reinterpret_cast<const jbyte*>(plaintext.data()));
        return jPlaintext;

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return nullptr;
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: EncryptionEngine.getEngineStatus()
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jstring JNICALL
Java_EncryptionEngine_getEngineStatus(JNIEnv* env,
                                      jobject /*thisObj*/)
{
    std::ostringstream oss;
    oss << "[ENGINE STATUS]\n"
        << "  Initialized : " << (g_engine.initialized ? "YES" : "NO") << "\n";

    if (g_engine.initialized) {
        oss << "  Key ID      : " << g_engine.keyId                        << "\n"
            << "  Algorithm   : " << g_engine.algorithm                    << "\n"
            << "  Key Length  : " << g_engine.keyMaterial.size() << " bytes\n";
    }

    return stdToJstring(env, oss.str());
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: EncryptionEngine.shutdownEngine()
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT void JNICALL
Java_EncryptionEngine_shutdownEngine(JNIEnv* /*env*/,
                                     jobject /*thisObj*/)
{
    g_engine.wipe();
    std::cout << "[NATIVE] Engine shut down. Key material securely wiped.\n";
}