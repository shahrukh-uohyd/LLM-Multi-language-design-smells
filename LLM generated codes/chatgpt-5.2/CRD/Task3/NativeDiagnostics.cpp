#include <jni.h>
#include <string>
#include <unistd.h>
#include <iostream>
#include "NativeDiagnostics.h"

/*
 * long getProcessId()
 */
JNIEXPORT jlong JNICALL
Java_NativeDiagnostics_getProcessId(JNIEnv *, jobject) {
    return (jlong)getpid();
}

/*
 * void logToSystem(String message)
 */
JNIEXPORT void JNICALL
Java_NativeDiagnostics_logToSystem(JNIEnv *env,
                                   jobject,
                                   jstring message) {

    const char *msg = env->GetStringUTFChars(message, nullptr);
    std::cerr << "[native-log] " << msg << std::endl;
    env->ReleaseStringUTFChars(message, msg);
}

/*
 * byte[] encrypt(byte[] data, byte key)
 * Simple XOR encryption (illustrative)
 */
JNIEXPORT jbyteArray JNICALL
Java_NativeDiagnostics_encrypt(JNIEnv *env,
                               jobject,
                               jbyteArray input,
                               jbyte key) {

    jsize len = env->GetArrayLength(input);
    jbyte *data = env->GetByteArrayElements(input, nullptr);

    for (jsize i = 0; i < len; i++) {
        data[i] ^= key;
    }

    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, data);

    env->ReleaseByteArrayElements(input, data, JNI_ABORT);
    return result;
}

/*
 * byte[] decrypt(byte[] data, byte key)
 * XOR decryption (same operation)
 */
JNIEXPORT jbyteArray JNICALL
Java_NativeDiagnostics_decrypt(JNIEnv *env,
                               jobject,
                               jbyteArray input,
                               jbyte key) {

    // XOR decrypt is identical to encrypt
    return Java_NativeDiagnostics_encrypt(env, nullptr, input, key);
}
