// diagnostic_crypto.h
#include <jni.h>

#ifndef _DIAGNOSTIC_CRYPTO_H
#define _DIAGNOSTIC_CRYPTO_H

#ifdef __cplusplus
extern "C" {
#endif

// Diagnostic logging methods
JNIEXPORT void JNICALL Java_DiagnosticLogger_logDiagnosticMessage(JNIEnv *, jobject, jstring);
JNIEXPORT jstring JNICALL Java_DiagnosticLogger_getSystemDiagnostics(JNIEnv *, jobject);
JNIEXPORT jboolean JNICALL Java_DiagnosticLogger_writeLogToFile(JNIEnv *, jobject, jstring, jstring);
JNIEXPORT jstring JNICALL Java_DiagnosticLogger_getProcessInfo(JNIEnv *, jobject);
JNIEXPORT jlong JNICALL Java_DiagnosticLogger_getMemoryUsage(JNIEnv *, jobject);
JNIEXPORT jstring JNICALL Java_DiagnosticLogger_getSystemTime(JNIEnv *, jobject);
JNIEXPORT jboolean JNICALL Java_DiagnosticLogger_flushLogs(JNIEnv *, jobject);

// Encryption/decryption methods
JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_encryptData(JNIEnv *, jobject, jbyteArray, jbyteArray, jstring);
JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_decryptData(JNIEnv *, jobject, jbyteArray, jbyteArray, jstring);
JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_generateKey(JNIEnv *, jobject, jint);
JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_hashData(JNIEnv *, jobject, jbyteArray, jstring);
JNIEXPORT jboolean JNICALL Java_DiagnosticLogger_verifyHash(JNIEnv *, jobject, jbyteArray, jbyteArray, jstring);
JNIEXPORT jbyteArray JNICALL Java_DiagnosticLogger_signData(JNIEnv *, jobject, jbyteArray, jbyteArray);
JNIEXPORT jboolean JNICALL Java_DiagnosticLogger_verifySignature(JNIEnv *, jobject, jbyteArray, jbyteArray, jbyteArray);

#ifdef __cplusplus
}
#endif

#endif