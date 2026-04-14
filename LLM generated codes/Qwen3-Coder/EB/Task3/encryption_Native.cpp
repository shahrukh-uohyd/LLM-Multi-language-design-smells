#include <jni.h>
#include <iostream>
#include <vector>
#include <string>
#include <cstring>
#include <memory>
#include <mutex>
#include <stdexcept>

// Include AES implementation headers (you can use OpenSSL or implement your own)
#include <openssl/aes.h>
#include <openssl/evp.h>
#include <openssl/rand.h>

#include "EncryptionEngine.h"
#include "SecurityKey.h" // Generated from SecurityKey.java

// Global variables to maintain encryption engine state
static std::unique_ptr<AES_KEY> g_encryption_key = nullptr;
static std::unique_ptr<AES_KEY> g_decryption_key = nullptr;
static std::string g_algorithm;
static bool g_is_initialized = false;
static std::mutex g_engine_mutex;

// Helper functions to access SecurityKey object fields
static jfieldID getEncryptionKeyFieldID(JNIEnv* env, jobject securityKeyObj) {
    jclass keyClass = env->GetObjectClass(securityKeyObj);
    if (keyClass == nullptr) return nullptr;
    
    return env->GetFieldID(keyClass, "encryptionKey", "[B");
}

static jfieldID getAlgorithmFieldID(JNIEnv* env, jobject securityKeyObj) {
    jclass keyClass = env->GetObjectClass(securityKeyObj);
    if (keyClass == nullptr) return nullptr;
    
    return env->GetFieldID(keyClass, "algorithm", "Ljava/lang/String;");
}

static jfieldID getIsValidFieldID(JNIEnv* env, jobject securityKeyObj) {
    jclass keyClass = env->GetObjectClass(securityKeyObj);
    if (keyClass == nullptr) return nullptr;
    
    return env->GetFieldID(keyClass, "isValid", "Z");
}

static jfieldID getKeyPurposeFieldID(JNIEnv* env, jobject securityKeyObj) {
    jclass keyClass = env->GetObjectClass(securityKeyObj);
    if (keyClass == nullptr) return nullptr;
    
    return env->GetFieldID(keyClass, "keyPurpose", "Ljava/lang/String;");
}

// Extract key bytes from SecurityKey object
static std::vector<unsigned char> extractKeyFromSecurityObject(JNIEnv* env, jobject securityKeyObj) {
    if (securityKeyObj == nullptr) {
        throw std::invalid_argument("SecurityKey object is null");
    }

    // Get the encryption key field
    jfieldID keyField = getEncryptionKeyFieldID(env, securityKeyObj);
    if (keyField == nullptr) {
        throw std::runtime_error("Failed to get encryptionKey field ID");
    }

    jbyteArray keyArray = (jbyteArray)env->GetObjectField(securityKeyObj, keyField);
    if (keyArray == nullptr) {
        throw std::runtime_error("Encryption key array is null");
    }

    jsize keyLength = env->GetArrayLength(keyArray);
    if (keyLength <= 0) {
        throw std::runtime_error("Invalid key length");
    }

    // Get key bytes
    jbyte* keyBytes = env->GetByteArrayElements(keyArray, nullptr);
    if (keyBytes == nullptr) {
        throw std::runtime_error("Failed to get key bytes");
    }

    std::vector<unsigned char> result((unsigned char*)keyBytes, 
                                     (unsigned char*)keyBytes + keyLength);

    // Release the array elements without copying back (we only read)
    env->ReleaseByteArrayElements(keyArray, keyBytes, JNI_ABORT);
    env->DeleteLocalRef(keyArray);

    return result;
}

// Extract algorithm from SecurityKey object
static std::string extractAlgorithmFromSecurityObject(JNIEnv* env, jobject securityKeyObj) {
    jfieldID algorithmField = getAlgorithmFieldID(env, securityKeyObj);
    if (algorithmField == nullptr) {
        throw std::runtime_error("Failed to get algorithm field ID");
    }

    jstring algorithmStr = (jstring)env->GetObjectField(securityKeyObj, algorithmField);
    if (algorithmStr == nullptr) {
        throw std::runtime_error("Algorithm string is null");
    }

    const char* algorithmCStr = env->GetStringUTFChars(algorithmStr, nullptr);
    if (algorithmCStr == nullptr) {
        throw std::runtime_error("Failed to convert algorithm to UTF string");
    }

    std::string result(algorithmCStr);
    
    env->ReleaseStringUTFChars(algorithmStr, algorithmCStr);
    env->DeleteLocalRef(algorithmStr);

    return result;
}

// Validate the security key
static bool validateSecurityKey(JNIEnv* env, jobject securityKeyObj) {
    // Check if key is valid
    jfieldID validField = getIsValidFieldID(env, securityKeyObj);
    if (validField == nullptr) {
        std::cerr << "[NATIVE ENCRYPTION] Failed to get isValid field" << std::endl;
        return false;
    }

    jboolean isValid = env->GetBooleanField(securityKeyObj, validField);
    if (!isValid) {
        std::cerr << "[NATIVE ENCRYPTION] Security key is invalid" << std::endl;
        return false;
    }

    // Check key purpose
    jfieldID purposeField = getKeyPurposeFieldID(env, securityKeyObj);
    if (purposeField != nullptr) {
        jstring purposeStr = (jstring)env->GetObjectField(securityKeyObj, purposeField);
        if (purposeStr != nullptr) {
            const char* purposeCStr = env->GetStringUTFChars(purposeStr, nullptr);
            if (purposeCStr != nullptr) {
                if (std::string(purposeCStr) != "ENCRYPTION") {
                    std::cerr << "[NATIVE ENCRYPTION] Security key purpose is not ENCRYPTION" << std::endl;
                    env->ReleaseStringUTFChars(purposeStr, purposeCStr);
                    env->DeleteLocalRef(purposeStr);
                    return false;
                }
                env->ReleaseStringUTFChars(purposeStr, purposeCStr);
            }
            env->DeleteLocalRef(purposeStr);
        }
    }

    return true;
}

/*
 * Class:     EncryptionEngine
 * Method:    initializeEncryption
 * Signature: (LSecurityKey;)Z
 */
JNIEXPORT jboolean JNICALL Java_EncryptionEngine_initializeEncryption
  (JNIEnv *env, jobject obj, jobject securityKeyObj) {
    
    std::lock_guard<std::mutex> lock(g_engine_mutex);
    
    try {
        // Validate the security key
        if (!validateSecurityKey(env, securityKeyObj)) {
            std::cerr << "[NATIVE ENCRYPTION] Security key validation failed" << std::endl;
            return JNI_FALSE;
        }

        // Extract key from security object
        std::vector<unsigned char> keyBytes = extractKeyFromSecurityObject(env, securityKeyObj);
        std::string algorithm = extractAlgorithmFromSecurityObject(env, securityKeyObj);

        // Validate key length based on algorithm
        if (algorithm == "AES" && keyBytes.size() != 16 && keyBytes.size() != 24 && keyBytes.size() != 32) {
            std::cerr << "[NATIVE ENCRYPTION] Invalid AES key length: " << keyBytes.size() << std::endl;
            return JNI_FALSE;
        }

        // Initialize encryption key
        g_encryption_key = std::make_unique<AES_KEY>();
        g_decryption_key = std::make_unique<AES_KEY>();

        int enc_result = AES_set_encrypt_key(keyBytes.data(), keyBytes.size() * 8, g_encryption_key.get());
        int dec_result = AES_set_decrypt_key(keyBytes.data(), keyBytes.size() * 8, g_decryption_key.get());

        if (enc_result < 0 || dec_result < 0) {
            std::cerr << "[NATIVE ENCRYPTION] Failed to set AES keys" << std::endl;
            g_encryption_key.reset();
            g_decryption_key.reset();
            return JNI_FALSE;
        }

        g_algorithm = algorithm;
        g_is_initialized = true;

        std::cout << "[NATIVE ENCRYPTION] Engine initialized successfully with " 
                  << algorithm << " algorithm and " << keyBytes.size() << " byte key" << std::endl;

        return JNI_TRUE;

    } catch (const std::exception& e) {
        std::cerr << "[NATIVE ENCRYPTION] Exception during initialization: " << e.what() << std::endl;
        return JNI_FALSE;
    }
}

/*
 * Class:     EncryptionEngine
 * Method:    encryptData
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_EncryptionEngine_encryptData
  (JNIEnv *env, jobject obj, jbyteArray plaintext) {
    
    std::lock_guard<std::mutex> lock(g_engine_mutex);

    if (!g_is_initialized) {
        std::cerr << "[NATIVE ENCRYPTION] Engine not initialized" << std::endl;
        return nullptr;
    }

    if (plaintext == nullptr) {
        std::cerr << "[NATIVE ENCRYPTION] Plaintext is null" << std::endl;
        return nullptr;
    }

    jsize plaintext_len = env->GetArrayLength(plaintext);
    if (plaintext_len <= 0) {
        std::cerr << "[NATIVE ENCRYPTION] Invalid plaintext length" << std::endl;
        return nullptr;
    }

    // Get plaintext bytes
    jbyte* plaintext_bytes = env->GetByteArrayElements(plaintext, nullptr);
    if (plaintext_bytes == nullptr) {
        std::cerr << "[NATIVE ENCRYPTION] Failed to get plaintext bytes" << std::endl;
        return nullptr;
    }

    // Pad the data to AES block size (16 bytes)
    int padded_length = ((plaintext_len + 15) / 16) * 16;
    std::vector<unsigned char> padded_data(padded_length, 0);
    std::memcpy(padded_data.data(), plaintext_bytes, plaintext_len);

    // Add PKCS7 padding
    int padding = padded_length - plaintext_len;
    for (int i = plaintext_len; i < padded_length; i++) {
        padded_data[i] = padding;
    }

    // Encrypt data in blocks
    std::vector<unsigned char> encrypted_data(padded_length);
    for (int i = 0; i < padded_length; i += 16) {
        AES_encrypt(&padded_data[i], &encrypted_data[i], g_encryption_key.get());
    }

    // Release plaintext array
    env->ReleaseByteArrayElements(plaintext, plaintext_bytes, JNI_ABORT);

    // Create result array
    jbyteArray result = env->NewByteArray(padded_length);
    if (result == nullptr) {
        std::cerr << "[NATIVE ENCRYPTION] Failed to create result array" << std::endl;
        return nullptr;
    }

    env->SetByteArrayRegion(result, 0, padded_length, (jbyte*)encrypted_data.data());

    return result;
}

/*
 * Class:     EncryptionEngine
 * Method:    decryptData
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_EncryptionEngine_decryptData
  (JNIEnv *env, jobject obj, jbyteArray ciphertext) {
    
    std::lock_guard<std::mutex> lock(g_engine_mutex);

    if (!g_is_initialized) {
        std::cerr << "[NATIVE ENCRYPTION] Engine not initialized" << std::endl;
        return nullptr;
    }

    if (ciphertext == nullptr) {
        std::cerr << "[NATIVE ENCRYPTION] Ciphertext is null" << std::endl;
        return nullptr;
    }

    jsize ciphertext_len = env->GetArrayLength(ciphertext);
    if (ciphertext_len <= 0 || ciphertext_len % 16 != 0) {
        std::cerr << "[NATIVE ENCRYPTION] Invalid ciphertext length (must be multiple of 16)" << std::endl;
        return nullptr;
    }

    // Get ciphertext bytes
    jbyte* ciphertext_bytes = env->GetByteArrayElements(ciphertext, nullptr);
    if (ciphertext_bytes == nullptr) {
        std::cerr << "[NATIVE ENCRYPTION] Failed to get ciphertext bytes" << std::endl;
        return nullptr;
    }

    // Decrypt data in blocks
    std::vector<unsigned char> decrypted_data(ciphertext_len);
    for (int i = 0; i < ciphertext_len; i += 16) {
        AES_decrypt(&ciphertext_bytes[i], &decrypted_data[i], g_decryption_key.get());
    }

    // Remove PKCS7 padding
    unsigned char padding = decrypted_data[ciphertext_len - 1];
    if (padding > 0 && padding <= 16) {
        for (int i = ciphertext_len - padding; i < ciphertext_len; i++) {
            if (decrypted_data[i] != padding) {
                std::cerr << "[NATIVE ENCRYPTION] Invalid padding detected" << std::endl;
                env->ReleaseByteArrayElements(ciphertext, ciphertext_bytes, JNI_ABORT);
                return nullptr;
            }
        }
        ciphertext_len -= padding;
    }

    // Release ciphertext array
    env->ReleaseByteArrayElements(ciphertext, ciphertext_bytes, JNI_ABORT);

    // Create result array
    jbyteArray result = env->NewByteArray(ciphertext_len);
    if (result == nullptr) {
        std::cerr << "[NATIVE ENCRYPTION] Failed to create result array" << std::endl;
        return nullptr;
    }

    env->SetByteArrayRegion(result, 0, ciphertext_len, (jbyte*)decrypted_data.data());

    return result;
}

/*
 * Class:     EncryptionEngine
 * Method:    validateKey
 * Signature: (LSecurityKey;)Z
 */
JNIEXPORT jboolean JNICALL Java_EncryptionEngine_validateKey
  (JNIEnv *env, jobject obj, jobject securityKeyObj) {
    
    try {
        bool isValid = validateSecurityKey(env, securityKeyObj);
        std::cout << "[NATIVE ENCRYPTION] Key validation result: " << (isValid ? "VALID" : "INVALID") << std::endl;
        return isValid ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        std::cerr << "[NATIVE ENCRYPTION] Exception during key validation: " << e.what() << std::endl;
        return JNI_FALSE;
    }
}

/*
 * Class:     EncryptionEngine
 * Method:    cleanup
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_EncryptionEngine_cleanup
  (JNIEnv *env, jobject obj) {
    
    std::lock_guard<std::mutex> lock(g_engine_mutex);

    if (g_encryption_key) {
        // Clear the key memory for security
        std::memset(g_encryption_key.get(), 0, sizeof(AES_KEY));
        g_encryption_key.reset();
    }

    if (g_decryption_key) {
        std::memset(g_decryption_key.get(), 0, sizeof(AES_KEY));
        g_decryption_key.reset();
    }

    g_algorithm.clear();
    g_is_initialized = false;

    std::cout << "[NATIVE ENCRYPTION] Engine cleaned up successfully" << std::endl;
}