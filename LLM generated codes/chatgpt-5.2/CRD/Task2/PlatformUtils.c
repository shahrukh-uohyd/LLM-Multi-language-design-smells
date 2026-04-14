#include <jni.h>
#include <time.h>
#include <unistd.h>
#include <stdlib.h>
#include "PlatformUtils.h"

/*
 * long getCurrentTimeMillis()
 */
JNIEXPORT jlong JNICALL
Java_PlatformUtils_getCurrentTimeMillis(JNIEnv *env, jobject obj) {
    return (jlong)time(NULL) * 1000;
}

/*
 * int getCpuCount()
 */
JNIEXPORT jint JNICALL
Java_PlatformUtils_getCpuCount(JNIEnv *env, jobject obj) {
    return (jint)sysconf(_SC_NPROCESSORS_ONLN);
}

/*
 * void nativeSleep(int millis)
 */
JNIEXPORT void JNICALL
Java_PlatformUtils_nativeSleep(JNIEnv *env, jobject obj, jint millis) {
    usleep((useconds_t)millis * 1000);
}

/*
 * byte[] compress(byte[] input)
 * Simple run-length encoding (RLE) compression
 */
JNIEXPORT jbyteArray JNICALL
Java_PlatformUtils_compress(JNIEnv *env,
                            jobject obj,
                            jbyteArray input) {

    jsize len = (*env)->GetArrayLength(env, input);
    jbyte *data = (*env)->GetByteArrayElements(env, input, NULL);

    // Worst-case size: 2 * len
    jbyte *out = (jbyte *)malloc(len * 2);
    int outLen = 0;

    for (int i = 0; i < len; i++) {
        jbyte value = data[i];
        int count = 1;

        while (i + 1 < len && data[i + 1] == value && count < 255) {
            count++;
            i++;
        }

        out[outLen++] = value;
        out[outLen++] = (jbyte)count;
    }

    (*env)->ReleaseByteArrayElements(env, input, data, JNI_ABORT);

    jbyteArray result = (*env)->NewByteArray(env, outLen);
    (*env)->SetByteArrayRegion(env, result, 0, outLen, out);

    free(out);
    return result;
}
