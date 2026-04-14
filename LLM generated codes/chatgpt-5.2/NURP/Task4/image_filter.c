#include <jni.h>
#include <stdint.h>
#include "NativeImageFilter.h"

JNIEXPORT jintArray JNICALL
Java_NativeImageFilter_grayscale(JNIEnv *env, jclass cls,
                                 jintArray pixels,
                                 jint width,
                                 jint height) {

    jint *src = (*env)->GetIntArrayElements(env, pixels, NULL);
    jsize length = (*env)->GetArrayLength(env, pixels);

    jintArray result = (*env)->NewIntArray(env, length);
    jint *dst = (*env)->GetIntArrayElements(env, result, NULL);

    for (int i = 0; i < length; i++) {
        uint32_t argb = (uint32_t) src[i];

        uint8_t a = (argb >> 24) & 0xFF;
        uint8_t r = (argb >> 16) & 0xFF;
        uint8_t g = (argb >> 8) & 0xFF;
        uint8_t b = argb & 0xFF;

        uint8_t gray = (uint8_t)(0.299*r + 0.587*g + 0.114*b);

        dst[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
    }

    (*env)->ReleaseIntArrayElements(env, pixels, src, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, result, dst, 0);

    return result;
}
