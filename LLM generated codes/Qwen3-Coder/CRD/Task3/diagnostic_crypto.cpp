// diagnostic_crypto.cpp
#include "diagnostic_crypto.h"
#include <iostream>
#include <fstream>
#include <sstream>
#include <chrono>
#include <ctime>
#include <iomanip>
#include <thread>
#include <random>
#include <sys/resource.h>
#include <sys/utsname.h>
#include <unistd.h>
#include <openssl/evp.h>
#include <openssl/aes.h>
#include <openssl/sha.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
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

// Diagnostic logging implementations
JNIEXPORT void JNICALL Java_DiagnosticLogger_logDiagnosticMessage(JNIEnv *env, jobject obj, jstring message) {
    std::string msg = jstringToString(env, message);
    std::cout << "[DIAGNOSTIC] " << msg << std::endl;
}

JNIEXPORT jstring JNICALL Java_DiagnosticLogger_getSystemDiagnostics(JNIEnv *env, jobject obj) {
    struct utsname sysinfo;
    uname(&sysinfo);
    
    std::ostringstream oss;
    oss << "OS: " << sysinfo.sysname 
        << ", Release: " << sysinfo.release 
        << ", Version: " << sysinfo.version
        << ", Machine: " << sysinfo.machine;
    
    return env->NewStringUTF(oss.str().c_str());
}

JNIEXPORT jboolean JNICALL Java_DiagnosticLogger_writeLogToFile(JNIEnv *env, jobject obj, jstring filename, jstring message) {
    std::string fname = jstringToString(env, filename);
    std::string msg = jstringToString(env, message);
    
    std::ofstream file(fname, std::ios::app);
    if (!file.is_open()) {
        return JNI_FALSE;
    }
    
    auto now = std::chrono::system_clock::now();
    auto time_t = std::chrono::system_clock::to_time_t(now);
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(
        now.time_since_epoch()) % 1000;
    
    std::stringstream ss;
    ss << std::put_time(std::localtime(&time_t), "%Y-%m-%d %H:%M:%S");
    ss << '.' << std::setfill('0') << std::setw(3) << ms.count();
    
    file << "[" << ss.str() << "] " << msg << std::endl;
    file.close();
    
    return file.good() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_DiagnosticLogger_getProcessInfo(JNIEnv *env, jobject obj) {
    pid_t pid = getpid();
    uid_t uid = getuid();
    
    std::ostringstream oss;
    oss << "PID: " << pid << ", UID: " << uid;
    
    return env->NewStringUTF(oss.str().c_str());
}

JNIEXPORT jlong JNICALL Java_DiagnosticLogger_getMemoryUsage(JNIEnv *env, jobject obj) {
    struct rusage usage;
    getrusage(RUSAGE_SELF, &usage);
    return static_cast<jlong>(usage.ru_maxrss * 1024LL); // Convert from KB to bytes
}

JNIEXPORT jstring JNICALL Java_DiagnosticLogger_getSystemTime(JNIEnv *env, jobject obj) {
    auto now = std::chrono::system_clock::now();
    auto time_t = std::chrono::system_clock::to_time_t(now);
    
    std::stringstream ss;
    ss << std::put_time(std::localtime(&time_t), "%Y-%m-%d %H:%M:%S");
    
    return env->NewStringUTF(ss.str().c_str());
}

JNIEXPORT jboolean JNICALL Java_DiagnosticLogger_flushLogs(JNIEnv *env, jobject obj) {
    std::cout.flush();
    std::cerr.flush();
    return JNI_TRUE;
}

// Encryption/decryption implementations
JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_encryptData(JNIEnv *env, jobject obj, jbyteArray data, jbyteArray key, jstring algorithm) {
    std::string algo = jstringToString(env, algorithm);
    std::vector<uint8_t> dataVec = jbyteArrayToVector(env, data);
    std::vector<uint8_t> keyVec = jbyteArrayToVector(env, key);
    
    if (algo == "AES") {
        // AES encryption
        std::vector<uint8_t> iv(AES_BLOCK_SIZE, 0); // In practice, use a random IV
        
        EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
        if (!ctx) return nullptr;
        
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
        
        ciphertext.resize(ciphertext_len);
        return vectorToJByteArray(env, ciphertext);
    }
    
    return vectorToJByteArray(env, dataVec);
}

JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_decryptData(JNIEnv *env, jobject obj, jbyteArray encryptedData, jbyteArray key, jstring algorithm) {
    std::string algo = jstringToString(env, algorithm);
    std::vector<uint8_t> encryptedVec = jbyteArrayToVector(env, encryptedData);
    std::vector<uint8_t> keyVec = jbyteArrayToVector(env, key);
    
    if (algo == "AES") {
        // AES decryption
        std::vector<uint8_t> iv(AES_BLOCK_SIZE, 0); // In practice, extract from encrypted data
        
        EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
        if (!ctx) return nullptr;
        
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
        
        plaintext.resize(plaintext_len);
        return vectorToJByteArray(env, plaintext);
    }
    
    return vectorToJByteArray(env, encryptedVec);
}

JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_generateKey(JNIEnv *env, jobject obj, jint keyLength) {
    std::vector<uint8_t> key(keyLength);
    
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

JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_hashData(JNIEnv *env, jobject obj, jbyteArray data, jstring algorithm) {
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
    } else if (algo == "SHA-512") {
        unsigned char hash[SHA512_DIGEST_LENGTH];
        SHA512_CTX sha512;
        SHA512_Init(&sha512);
        SHA512_Update(&sha512, dataVec.data(), dataVec.size());
        SHA512_Final(hash, &sha512);
        
        std::vector<uint8_t> result(hash, hash + SHA512_DIGEST_LENGTH);
        return vectorToJByteArray(env, result);
    }
    
    return env->NewByteArray(0);
}

JNIEXPORT jboolean JNICALL Java_DiagnosticLogger_verifyHash(JNIEnv *env, jobject obj, jbyteArray data, jbyteArray expectedHash, jstring algorithm) {
    std::vector<uint8_t> dataVec = jbyteArrayToVector(env, data);
    std::vector<uint8_t> expectedHashVec = jbyteArrayToVector(env, expectedHash);
    
    std::vector<uint8_t> calculatedHash = jbyteArrayToVector(env, Java_DiagnosticLogger_hashData(env, obj, Java_DiagnosticLogger_generateKey(env, obj, dataVec.size()), algorithm));
    
    if (calculatedHash.size() != expectedHashVec.size()) {
        return JNI_FALSE;
    }
    
    return std::equal(calculatedHash.begin(), calculatedHash.end(), expectedHashVec.begin()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_signData(JNIEnv *env, jobject obj, jbyteArray data, jbyteArray privateKey) {
    // Simple signature placeholder - in real implementation, use proper RSA signing
    std::vector<uint8_t> dataVec = jbyteArrayToVector(env, data);
    std::vector<uint8_t> privKeyVec = jbyteArrayToVector(env, privateKey);
    
    // This is a placeholder implementation
    std::vector<uint8_t> signature(256, 0); // RSA signature size
    for (size_t i = 0; i < dataVec.size(); ++i) {
        signature[i % 256] ^= dataVec[i];
    }
    
    return vectorToJByteArray(env, signature);
}

JNIEXPORT jboolean JNICALL Java_DiagnosticLogger_verifySignature(JNIEnv *env, jobject obj, jbyteArray data, jbyteArray signature, jbyteArray publicKey) {
    // Simple signature verification placeholder
    std::vector<uint8_t> dataVec = jbyteArrayToVector(env, data);
    std::vector<uint8_t> sigVec = jbyteArrayToVector(env, signature);
    std::vector<uint8_t> pubKeyVec = jbyteArrayToVector(env, publicKey);
    
    // This is a placeholder implementation
    return (dataVec.size() > 0 && sigVec.size() > 0 && pubKeyVec.size() > 0) ? JNI_TRUE : JNI_FALSE;
}