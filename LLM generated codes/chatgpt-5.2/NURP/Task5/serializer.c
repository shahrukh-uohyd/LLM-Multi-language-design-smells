#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include "NativeSerializer.h"

JNIEXPORT jbyteArray JNICALL
Java_NativeSerializer_serialize(JNIEnv *env, jclass cls, jstring input) {

    const char *utf = (*env)->GetStringUTFChars(env, input, NULL);
    size_t len = strlen(utf);

    jbyteArray result = (*env)->NewByteArray(env, (jsize)len);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)len, (jbyte *)utf);

    (*env)->ReleaseStringUTFChars(env, input, utf);
    return result;
}

JNIEXPORT jstring JNICALL
Java_NativeSerializer_deserialize(JNIEnv *env, jclass cls, jbyteArray bytes) {

    jsize len = (*env)->GetArrayLength(env, bytes);
    jbyte *data = (*env)->GetByteArrayElements(env, bytes, NULL);

    char *buffer = (char *)malloc(len + 1);
    memcpy(buffer, data, len);
    buffer[len] = '\0';

    jstring result = (*env)->NewStringUTF(env, buffer);

    free(buffer);
    (*env)->ReleaseByteArrayElements(env, bytes, data, JNI_ABORT);

    return result;
}
