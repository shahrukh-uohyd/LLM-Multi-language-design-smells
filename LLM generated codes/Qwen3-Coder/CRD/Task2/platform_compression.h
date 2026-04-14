// platform_compression.h
#include <jni.h>

#ifndef _PLATFORM_COMPRESSION_H
#define _PLATFORM_COMPRESSION_H

#ifdef __cplusplus
extern "C" {
#endif

// Platform utility methods
JNIEXPORT jstring JNICALL Java_PlatformUtils_getPlatformName(JNIEnv *, jobject);
JNIEXPORT jstring JNICALL Java_PlatformUtils_getSystemArchitecture(JNIEnv *, jobject);
JNIEXPORT jlong JNICALL Java_PlatformUtils_getAvailableMemory(JNIEnv *, jobject);
JNIEXPORT jstring JNICALL Java_PlatformUtils_getTempDirectory(JNIEnv *, jobject);
JNIEXPORT jboolean JNICALL Java_PlatformUtils_createProcess(JNIEnv *, jobject, jstring);
JNIEXPORT jstring JNICALL Java_PlatformUtils_getUserName(JNIEnv *, jobject);
JNIEXPORT jstring JNICALL Java_PlatformUtils_getMachineId(JNIEnv *, jobject);

// Compression methods
JNIEXPORT jbyteArray JNICALL Java_PlatformUtils_compressData(JNIEnv *, jobject, jbyteArray, jstring);
JNIEXPORT jbyteArray JNICALL Java_PlatformUtils_decompressData(JNIEnv *, jobject, jbyteArray, jstring);
JNIEXPORT jint JNICALL Java_PlatformUtils_getCompressionRatio(JNIEnv *, jobject, jbyteArray, jbyteArray);
JNIEXPORT jbyteArray JNICALL Java_PlatformUtils_compressWithLevel(JNIEnv *, jobject, jbyteArray, jint);
JNIEXPORT jboolean JNICALL Java_PlatformUtils_validateCompressedData(JNIEnv *, jobject, jbyteArray);

#ifdef __cplusplus
}
#endif

#endif