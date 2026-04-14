#include <jni.h>
#include <vector>
#include <numeric>
#include "NativeAuthenticator.h"

/*
 * int[] nativeExtractMinutiae(byte[] rawData)
 * Simulated extraction logic (already exists in native library)
 */
JNIEXPORT jintArray JNICALL
Java_NativeAuthenticator_nativeExtractMinutiae(JNIEnv* env,
                                                jobject,
                                                jbyteArray rawData) {

    jsize len = env->GetArrayLength(rawData);
    jbyte* data = env->GetByteArrayElements(rawData, nullptr);

    // Example: convert raw bytes into integer features
    std::vector<jint> minutiae(len);
    for (jsize i = 0; i < len; i++) {
        minutiae[i] = static_cast<unsigned char>(data[i]);
    }

    env->ReleaseByteArrayElements(rawData, data, JNI_ABORT);

    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, minutiae.data());
    return result;
}

/*
 * byte[] nativeGenerateSignature(int[] minutiae)
 * Simulated cryptographic signature generation
 */
JNIEXPORT jbyteArray JNICALL
Java_NativeAuthenticator_nativeGenerateSignature(JNIEnv* env,
                                                  jobject,
                                                  jintArray minutiae) {

    jsize len = env->GetArrayLength(minutiae);
    jint* data = env->GetIntArrayElements(minutiae, nullptr);

    // Simple deterministic signature (illustrative)
    unsigned int hash = 2166136261u;
    for (jsize i = 0; i < len; i++) {
        hash ^= data[i];
        hash *= 16777619u;
    }

    env->ReleaseIntArrayElements(minutiae, data, JNI_ABORT);

    jbyteArray signature = env->NewByteArray(4);
    jbyte sig[4] = {
        (jbyte)((hash >> 24) & 0xFF),
        (jbyte)((hash >> 16) & 0xFF),
        (jbyte)((hash >> 8) & 0xFF),
        (jbyte)(hash & 0xFF)
    };

    env->SetByteArrayRegion(signature, 0, 4, sig);
    return signature;
}

/*
 * boolean nativeTransmitSignature(byte[] signature)
 * Simulated secure hardware transmission
 */
JNIEXPORT jboolean JNICALL
Java_NativeAuthenticator_nativeTransmitSignature(JNIEnv* env,
                                                  jobject,
                                                  jbyteArray signature) {

    // In a real system, this would communicate with secure hardware
    // Here we simulate successful transmission
    return JNI_TRUE;
}
