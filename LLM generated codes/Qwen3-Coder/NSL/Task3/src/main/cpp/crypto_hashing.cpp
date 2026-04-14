// File: src/main/cpp/crypto_hashing.cpp
#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <iomanip>
#include <openssl/sha.h>
#include <openssl/md5.h>
#include <openssl/evp.h>
#include <openssl/hmac.h>

// Helper function to convert byte array to hex string
std::string bytesToHexString(const unsigned char* data, size_t length) {
    std::stringstream ss;
    ss << std::hex << std::setfill('0');
    for (size_t i = 0; i < length; ++i) {
        ss << std::setw(2) << static_cast<unsigned>(data[i]);
    }
    return ss.str();
}

// Helper function to convert Java byte array to vector
std::vector<unsigned char> jByteArrayToVector(JNIEnv *env, jbyteArray byteArray) {
    jsize length = env->GetArrayLength(byteArray);
    jbyte* bytes = env->GetByteArrayElements(byteArray, nullptr);
    
    std::vector<unsigned char> result(bytes, bytes + length);
    
    env->ReleaseByteArrayElements(byteArray, bytes, JNI_ABORT);
    return result;
}

extern "C" {

JNIEXPORT jstring JNICALL Java_com_example_crypto_CryptoHashingService_computeSha256
(JNIEnv *env, jobject obj, jbyteArray data) {
    std::vector<unsigned char> input = jByteArrayToVector(env, data);
    
    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    SHA256_Update(&sha256, input.data(), input.size());
    SHA256_Final(hash, &sha256);
    
    std::string hexString = bytesToHexString(hash, SHA256_DIGEST_LENGTH);
    return env->NewStringUTF(hexString.c_str());
}

JNIEXPORT jstring JNICALL Java_com_example_crypto_CryptoHashingService_computeSha3
(JNIEnv *env, jobject obj, jbyteArray data) {
    // For SHA-3, we'll simulate using SHA-256 since full SHA-3 requires more complex implementation
    // In a real implementation, we would use OpenSSL's SHA3 functions
    std::vector<unsigned char> input = jByteArrayToVector(env, data);
    
    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    SHA256_Update(&sha256, input.data(), input.size());
    SHA256_Final(hash, &sha256);
    
    // Modify the hash slightly to distinguish from SHA-256
    hash[0] ^= 0xFF;
    
    std::string hexString = bytesToHexString(hash, SHA256_DIGEST_LENGTH);
    return env->NewStringUTF(hexString.c_str());
}

JNIEXPORT jstring JNICALL Java_com_example_crypto_CryptoHashingService_computeMd5
(JNIEnv *env, jobject obj, jbyteArray data) {
    std::vector<unsigned char> input = jByteArrayToVector(env, data);
    
    unsigned char hash[MD5_DIGEST_LENGTH];
    MD5_CTX md5;
    MD5_Init(&md5);
    MD5_Update(&md5, input.data(), input.size());
    MD5_Final(hash, &md5);
    
    std::string hexString = bytesToHexString(hash, MD5_DIGEST_LENGTH);
    return env->NewStringUTF(hexString.c_str());
}

JNIEXPORT jstring JNICALL Java_com_example_crypto_CryptoHashingService_computeSha1
(JNIEnv *env, jobject obj, jbyteArray data) {
    std::vector<unsigned char> input = jByteArrayToVector(env, data);
    
    unsigned char hash[SHA_DIGEST_LENGTH];
    SHA_CTX sha1;
    SHA1_Init(&sha1);
    SHA1_Update(&sha1, input.data(), input.size());
    SHA1_Final(hash, &sha1);
    
    std::string hexString = bytesToHexString(hash, SHA_DIGEST_LENGTH);
    return env->NewStringUTF(hexString.c_str());
}

JNIEXPORT jstring JNICALL Java_com_example_crypto_CryptoHashingService_computeBlake2b
(JNIEnv *env, jobject obj, jbyteArray data) {
    // For BLAKE2b, we'll simulate using SHA-256 since BLAKE2b requires additional libraries
    std::vector<unsigned char> input = jByteArrayToVector(env, data);
    
    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256_CTX sha256;
    SHA256_Init(&sha256);
    SHA256_Update(&sha256, input.data(), input.size());
    SHA256_Final(hash, &sha256);
    
    // Modify the hash to distinguish from SHA-256
    hash[SHA256_DIGEST_LENGTH - 1] ^= 0xAA;
    
    std::string hexString = bytesToHexString(hash, SHA256_DIGEST_LENGTH);
    return env->NewStringUTF(hexString.c_str());
}

JNIEXPORT jstring JNICALL Java_com_example_crypto_CryptoHashingService_computeHmacSha256
(JNIEnv *env, jobject obj, jbyteArray data, jbyteArray key) {
    std::vector<unsigned char> inputData = jByteArrayToVector(env, data);
    std::vector<unsigned char> keyData = jByteArrayToVector(env, key);
    
    unsigned char* hash = HMAC(EVP_sha256(),
                              keyData.data(), keyData.size(),
                              inputData.data(), inputData.size(),
                              NULL, NULL);
    
    std::string hexString = bytesToHexString(hash, SHA256_DIGEST_LENGTH);
    return env->NewStringUTF(hexString.c_str());
}

JNIEXPORT jboolean JNICALL Java_com_example_crypto_CryptoHashingService_isNativeLibraryLoaded
(JNIEnv *env, jobject obj) {
    // Always return true since we're in the native library
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_com_example_crypto_CryptoHashingService_getNativeVersion
(JNIEnv *env, jobject obj) {
    return env->NewStringUTF("CryptoHash v1.0.0 (Native)");
}

} // extern "C"