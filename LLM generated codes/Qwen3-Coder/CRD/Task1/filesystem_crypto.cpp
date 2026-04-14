// filesystem_crypto.cpp
#include "filesystem_crypto.h"
#include <iostream>
#include <fstream>
#include <vector>
#include <cstring>
#include <random>
#include <openssl/evp.h>
#include <openssl/aes.h>
#include <openssl/sha.h>
#include <openssl/rand.h>

// Helper function to convert jstring to std::string
std::string jstringToString(JNIEnv* env, jstring jstr) {
    const char* cstr = env->GetStringUTFChars(jstr, nullptr);
    std::string str(cstr);
    env->ReleaseStringUTFChars(jstr, cstr);
    return str;
}

// Helper function to convert jbyteArray to std::vector<uint8_t>
std::vector<uint8_t> jbyteArrayToVector(JNIEnv* env, jbyteArray array) {
    jsize length = env->GetArrayLength(array);
    std::vector<uint8_t> vec(length);
    
    jbyte* elements = env->GetByteArrayElements(array, nullptr);
    memcpy(vec.data(), elements, length);
    env->ReleaseByteArrayElements(array, elements, JNI_ABORT);
    
    return vec;
}

// Helper function to convert std::vector<uint8_t> to jbyteArray
jbyteArray vectorToJByteArray(JNIEnv* env, const std::vector<uint8_t>& vec) {
    jbyteArray result = env->NewByteArray(vec.size());
    env->SetByteArrayRegion(result, 0, vec.size(), 
                           reinterpret_cast<const jbyte*>(vec.data()));
    return result;
}

// File system operations
JNIEXPORT jboolean JNICALL Java_FileSystemManager_createFile(JNIEnv *env, jobject obj, jstring filename) {
    std::string fname = jstringToString(env, filename);
    std::ofstream file(fname.c_str());
    return file.is_open() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_readFile(JNIEnv *env, jobject obj, jstring filename) {
    std::string fname = jstringToString(env, filename);
    
    std::ifstream file(fname.c_str(), std::ios::binary | std::ios::ate);
    if (!file.is_open()) {
        return nullptr;
    }
    
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);
    
    std::vector<char> buffer(size);
    if (!file.read(buffer.data(), size)) {
        return nullptr;
    }
    
    jbyteArray result = env->NewByteArray(size);
    env->SetByteArrayRegion(result, 0, size, 
                           reinterpret_cast<const jbyte*>(buffer.data()));
    return result;
}

JNIEXPORT jboolean JNICALL Java_FileSystemManager_writeFile(JNIEnv *env, jobject obj, jstring filename, jbyteArray data) {
    std::string fname = jstringToString(env, filename);
    std::vector<uint8_t> vec = jbyteArrayToVector(env, data);
    
    std::ofstream file(fname.c_str(), std::ios::binary);
    if (!file.is_open()) {
        return JNI_FALSE;
    }
    
    file.write(reinterpret_cast<const char*>(vec.data()), vec.size());
    return file.good() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_FileSystemManager_deleteFile(JNIEnv *env, jobject obj, jstring filename) {
    std::string fname = jstringToString(env, filename);
    return remove(fname.c_str()) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_FileSystemManager_getFileSize(JNIEnv *env, jobject obj, jstring filename) {
    std::string fname = jstringToString(env, filename);
    std::ifstream file(fname.c_str(), std::ios::ate | std::ios::binary);
    if (!file.is_open()) {
        return -1;
    }
    return file.tellg();
}

// Cryptographic operations
JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_encryptData(JNIEnv *env, jobject obj, jbyteArray data, jbyteArray key, jstring algorithm) {
    std::string algo = jstringToString(env, algorithm);
    std::vector<uint8_t> dataVec = jbyteArrayToVector(env, data);
    std::vector<uint8_t> keyVec = jbyteArrayToVector(env, key);
    
    if (algo == "AES") {
        // Simple AES encryption example
        std::vector<uint8_t> iv(AES_BLOCK_SIZE, 0); // In practice, use a random IV
        
        // Initialize OpenSSL cipher context
        EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
        if (!ctx) return nullptr;
        
        // Initialize for encryption
        if (EVP_EncryptInit_ex(ctx, EVP_aes_256_cbc(), NULL, keyVec.data(), iv.data()) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            return nullptr;
        }
        
        std::vector<uint8_t> ciphertext(dataVec.size() + AES_BLOCK_SIZE);
        int len;
        int ciphertext_len;
        
        if (EVP_EncryptUpdate(ctx, ciphertext.data(), &len, dataVec.data(), dataVec.size()) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            return nullptr;
        }
        ciphertext_len = len;
        
        if (EVP_EncryptFinal_ex(ctx, ciphertext.data() + len, &len) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            return nullptr;
        }
        ciphertext_len += len;
        
        EVP_CIPHER_CTX_free(ctx);
        
        // Resize to actual length
        ciphertext.resize(ciphertext_len);
        return vectorToJByteArray(env, ciphertext);
    }
    
    // Return original data if algorithm not supported
    return vectorToJByteArray(env, dataVec);
}

JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_decryptData(JNIEnv *env, jobject obj, jbyteArray encryptedData, jbyteArray key, jstring algorithm) {
    std::string algo = jstringToString(env, algorithm);
    std::vector<uint8_t> encryptedVec = jbyteArrayToVector(env, encryptedData);
    std::vector<uint8_t> keyVec = jbyteArrayToVector(env, key);
    
    if (algo == "AES") {
        // Simple AES decryption example
        std::vector<uint8_t> iv(AES_BLOCK_SIZE, 0); // In practice, extract from encrypted data
        
        // Initialize OpenSSL cipher context
        EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
        if (!ctx) return nullptr;
        
        // Initialize for decryption
        if (EVP_DecryptInit_ex(ctx, EVP_aes_256_cbc(), NULL, keyVec.data(), iv.data()) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            return nullptr;
        }
        
        std::vector<uint8_t> plaintext(encryptedVec.size());
        int len;
        int plaintext_len;
        
        if (EVP_DecryptUpdate(ctx, plaintext.data(), &len, encryptedVec.data(), encryptedVec.size()) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            return nullptr;
        }
        plaintext_len = len;
        
        if (EVP_DecryptFinal_ex(ctx, plaintext.data() + len, &len) != 1) {
            EVP_CIPHER_CTX_free(ctx);
            return nullptr;
        }
        plaintext_len += len;
        
        EVP_CIPHER_CTX_free(ctx);
        
        // Resize to actual length
        plaintext.resize(plaintext_len);
        return vectorToJByteArray(env, plaintext);
    }
    
    // Return encrypted data if algorithm not supported
    return vectorToJByteArray(env, encryptedVec);
}

JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_generateHash(JNIEnv *env, jobject obj, jbyteArray data, jstring algorithm) {
    std::string algo = jstringToString(env, algorithm);
    std::vector<uint8_t> dataVec = jbyteArrayToVector(env, data);
    
    if (algo == "SHA-256") {
        unsigned char hash[SHA256_DIGEST_LENGTH];
        SHA256_CTX sha256;
        SHA256_Init(&sha256);
        SHA256_Update(&sha256, dataVec.data(), dataVec.size());
        SHA256_Final(hash, &sha256);
        
        std::vector<uint8_t> result(hash, hash + SHA256_DIGEST_LENGTH);
        return vectorToJByteArray(env, result);
    }
    
    // Return empty array if algorithm not supported
    return env->NewByteArray(0);
}

JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_generateKey(JNIEnv *env, jobject obj, jint keyLength) {
    std::vector<uint8_t> key(keyLength);
    
    // Use OpenSSL for secure random generation
    if (RAND_bytes(key.data(), keyLength) != 1) {
        // Fallback to standard random if OpenSSL fails
        std::random_device rd;
        std::mt19937 gen(rd());
        std::uniform_int_distribution<> dis(0, 255);
        
        for (auto& byte : key) {
            byte = static_cast<uint8_t>(dis(gen));
        }
    }
    
    return vectorToJByteArray(env, key);
}

JNIEXPORT jboolean JNICALL Java_FileSystemManager_verifySignature(JNIEnv *env, jobject obj, jbyteArray data, jbyteArray signature, jbyteArray publicKey) {
    // Placeholder implementation - would typically use OpenSSL for signature verification
    // This is a simplified version for demonstration
    std::vector<uint8_t> dataVec = jbyteArrayToVector(env, data);
    std::vector<uint8_t> sigVec = jbyteArrayToVector(env, signature);
    std::vector<uint8_t> pubKeyVec = jbyteArrayToVector(env, publicKey);
    
    // In a real implementation, you would verify the signature against the data
    // using the provided public key with an appropriate algorithm
    
    // For now, return true if all arrays are non-empty (placeholder)
    return (dataVec.size() > 0 && sigVec.size() > 0 && pubKeyVec.size() > 0) ? JNI_TRUE : JNI_FALSE;
}