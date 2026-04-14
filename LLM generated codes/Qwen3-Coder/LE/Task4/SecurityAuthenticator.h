#ifndef SECURITY_AUTHENTICATOR_H
#define SECURITY_AUTHENTICATOR_H

#include <jni.h>
#include <string>
#include <vector>
#include <memory>

// Forward declarations
struct BiometricFeaturesInternal;
struct CryptographicSignatureInternal;

// Internal data structures
struct BiometricFeaturesInternal {
    std::vector<std::string> features;
    std::string extractionMethod;
    int qualityScore;
    
    BiometricFeaturesInternal() : qualityScore(0) {}
};

struct CryptographicSignatureInternal {
    std::vector<unsigned char> signatureData;
    std::string algorithm;
    std::string hashFunction;
    long creationTime;
    
    CryptographicSignatureInternal() : creationTime(0) {}
};

extern "C" {
    // JNI function declarations
    JNIEXPORT jlong JNICALL Java_SecurityAuthenticator_extractBiometricMinutiaeNative(JNIEnv *env, jobject obj, jbyteArray rawData);
    JNIEXPORT jlong JNICALL Java_SecurityAuthenticator_generateCryptographicSignatureNative(JNIEnv *env, jobject obj, jlong featuresHandle, jstring userId);
    JNIEXPORT jboolean JNICALL Java_SecurityAuthenticator_transmitToVaultNative(JNIEnv *env, jobject obj, jlong signatureHandle, jstring userId, jstring sessionId);
    JNIEXPORT void JNICALL Java_SecurityAuthenticator_cleanupHandleNative(JNIEnv *env, jobject obj, jlong handle);
}

// Helper functions
std::vector<std::string> extractBiometricFeatures(const std::vector<unsigned char>& rawData);
std::vector<unsigned char> generateDigitalSignature(const std::vector<std::string>& features, const std::string& userId);

#endif // SECURITY_AUTHENTICATOR_H