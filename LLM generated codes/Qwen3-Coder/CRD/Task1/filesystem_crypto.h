// filesystem_crypto.h
#include <jni.h>

#ifndef _FILESYSTEM_CRYPTO_H
#define _FILESYSTEM_CRYPTO_H

#ifdef __cplusplus
extern "C" {
#endif

// File system operations
JNIEXPORT jboolean JNICALL Java_FileSystemManager_createFile(JNIEnv *, jobject, jstring);
JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_readFile(JNIEnv *, jobject, jstring);
JNIEXPORT jboolean JNICALL Java_FileSystemManager_writeFile(JNIEnv *, jobject, jstring, jbyteArray);
JNIEXPORT jboolean JNICALL Java_FileSystemManager_deleteFile(JNIEnv *, jobject, jstring);
JNIEXPORT jlong JNICALL Java_FileSystemManager_getFileSize(JNIEnv *, jobject, jstring);

// Cryptographic operations
JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_encryptData(JNIEnv *, jobject, jbyteArray, jbyteArray, jstring);
JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_decryptData(JNIEnv *, jobject, jbyteArray, jbyteArray, jstring);
JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_generateHash(JNIEnv *, jobject, jbyteArray, jstring);
JNIEXPORT jbyteArray JNICALL Java_FileSystemManager_generateKey(JNIEnv *, jobject, jint);
JNIEXPORT jboolean JNICALL Java_FileSystemManager_verifySignature(JNIEnv *, jobject, jbyteArray, jbyteArray, jbyteArray);

#ifdef __cplusplus
}
#endif

#endif