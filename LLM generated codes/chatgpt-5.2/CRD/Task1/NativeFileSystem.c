#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include "NativeFileSystem.h"

/*
 * boolean fileExists(String path)
 */
JNIEXPORT jboolean JNICALL
Java_NativeFileSystem_fileExists(JNIEnv *env,
                                 jobject obj,
                                 jstring path) {

    const char *filePath = (*env)->GetStringUTFChars(env, path, NULL);
    FILE *file = fopen(filePath, "rb");

    if (file) {
        fclose(file);
        (*env)->ReleaseStringUTFChars(env, path, filePath);
        return JNI_TRUE;
    }

    (*env)->ReleaseStringUTFChars(env, path, filePath);
    return JNI_FALSE;
}

/*
 * long getFileSize(String path)
 */
JNIEXPORT jlong JNICALL
Java_NativeFileSystem_getFileSize(JNIEnv *env,
                                  jobject obj,
                                  jstring path) {

    const char *filePath = (*env)->GetStringUTFChars(env, path, NULL);
    FILE *file = fopen(filePath, "rb");

    if (!file) {
        (*env)->ReleaseStringUTFChars(env, path, filePath);
        return -1;
    }

    fseek(file, 0, SEEK_END);
    long size = ftell(file);
    fclose(file);

    (*env)->ReleaseStringUTFChars(env, path, filePath);
    return (jlong)size;
}

/*
 * byte[] computeHash(byte[] data)
 * Simple native hash (deterministic, illustrative)
 */
JNIEXPORT jbyteArray JNICALL
Java_NativeFileSystem_computeHash(JNIEnv *env,
                                  jobject obj,
                                  jbyteArray input) {

    jsize len = (*env)->GetArrayLength(env, input);
    jbyte *data = (*env)->GetByteArrayElements(env, input, NULL);

    unsigned int hash = 2166136261u;
    for (jsize i = 0; i < len; i++) {
        hash ^= (unsigned char)data[i];
        hash *= 16777619u;
    }

    (*env)->ReleaseByteArrayElements(env, input, data, JNI_ABORT);

    jbyteArray result = (*env)->NewByteArray(env, 4);
    jbyte out[4];
    out[0] = (hash >> 24) & 0xFF;
    out[1] = (hash >> 16) & 0xFF;
    out[2] = (hash >> 8) & 0xFF;
    out[3] = hash & 0xFF;

    (*env)->SetByteArrayRegion(env, result, 0, 4, out);
    return result;
}
