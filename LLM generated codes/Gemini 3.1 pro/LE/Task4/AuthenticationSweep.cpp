#include <jni.h>
#include <vector>
#include <cstdint>
#include <stdexcept>

// --- Mocking the Existing Core C++ Security Library ---
// In a real application, these functions represent your existing specialized logic.

struct AuthContext {
    std::vector<uint8_t> extracted_minutiae;
    std::vector<uint8_t> crypto_signature;
    bool has_minutiae = false;
    bool has_signature = false;

    // Custom destructor to securely zero out memory before deallocation
    ~AuthContext() {
        std::fill(extracted_minutiae.begin(), extracted_minutiae.end(), 0);
        std::fill(crypto_signature.begin(), crypto_signature.end(), 0);
    }
};

AuthContext* core_extract_minutiae(const std::vector<uint8_t>& raw_data) {
    auto* ctx = new AuthContext();
    // Mock extraction logic: using raw data directly as minutiae for demonstration
    ctx->extracted_minutiae = raw_data; 
    ctx->has_minutiae = true;
    return ctx;
}

void core_generate_signature(AuthContext* ctx) {
    if (ctx && ctx->has_minutiae) {
        // Mock cryptographic signing logic
        ctx->crypto_signature = ctx->extracted_minutiae; 
        ctx->crypto_signature.push_back(0xFF); // append mock signature byte
        ctx->has_signature = true;
    }
}

bool core_transmit_to_vault(AuthContext* ctx) {
    if (ctx && ctx->has_signature) {
        // Mock secure vault transmission logic
        return true; 
    }
    return false;
}
// ------------------------------------------------------

extern "C" {

// Step 1: JNI Wrapper for Minutiae Extraction
JNIEXPORT jlong JNICALL
Java_com_example_security_AuthenticationSweep_extractMinutiae(JNIEnv *env, jobject thiz, jbyteArray rawData) {
    if (rawData == nullptr) return 0;

    // Get the length of the raw biometric data
    jsize length = env->GetArrayLength(rawData);
    
    // Copy the Java byte array into a C++ std::vector buffer safely
    std::vector<uint8_t> cppBuffer(length);
    env->GetByteArrayRegion(rawData, 0, length, reinterpret_cast<jbyte*>(cppBuffer.data()));

    // Call core extraction logic to initialize the context
    AuthContext* ctx = core_extract_minutiae(cppBuffer);

    // Cast the memory pointer to a jlong handle to be managed by Java
    return reinterpret_cast<jlong>(ctx);
}

// Step 2: JNI Wrapper for Cryptographic Signature Generation
JNIEXPORT void JNICALL
Java_com_example_security_AuthenticationSweep_generateSignature(JNIEnv *env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<AuthContext*>(handle);
    
    if (ctx != nullptr) {
        core_generate_signature(ctx);
    }
}

// Step 3: JNI Wrapper for Vault Transmission
JNIEXPORT jboolean JNICALL
Java_com_example_security_AuthenticationSweep_transmitToVault(JNIEnv *env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<AuthContext*>(handle);
    
    if (ctx == nullptr) {
        return JNI_FALSE;
    }

    // Call core transmission logic and return the boolean result
    bool success = core_transmit_to_vault(ctx);
    return success ? JNI_TRUE : JNI_FALSE;
}

// Guaranteed Step: JNI Wrapper for Secure Memory Cleanup
JNIEXPORT void JNICALL
Java_com_example_security_AuthenticationSweep_cleanupContext(JNIEnv *env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<AuthContext*>(handle);
    
    // Deleting the pointer triggers the AuthContext destructor, 
    // which securely zeroes out the sensitive minutiae and signature vectors.
    if (ctx != nullptr) {
        delete ctx;
    }
}

}