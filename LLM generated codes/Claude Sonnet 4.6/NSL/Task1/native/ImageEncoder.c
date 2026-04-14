/*
 * JNI implementation of image encoding operations (no-package build).
 *
 * Encoding strategies:
 *   Base64  – RFC 4648 standard encoding
 *   RLE     – [count byte][value byte] pairs, run capped at 255
 *   Hex     – lowercase two hex digits per byte
 *
 * Pure ANSI C – no third-party libraries required.
 */

#include "ImageEncoder.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

/* ======================================================================
 * Base64 helpers
 * ====================================================================== */

static const char BASE64_TABLE[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

/**
 * Encode `src_len` bytes from `src` into the caller-allocated `dst`.
 * `dst` must hold at least ((src_len + 2) / 3 * 4 + 1) bytes.
 */
static void base64_encode(const unsigned char *src, size_t src_len, char *dst) {
    size_t i;
    size_t out = 0;
    for (i = 0; i < src_len; i += 3) {
        unsigned int b0 =  src[i];
        unsigned int b1 = (i + 1 < src_len) ? (unsigned int)src[i + 1] : 0u;
        unsigned int b2 = (i + 2 < src_len) ? (unsigned int)src[i + 2] : 0u;

        dst[out++] = BASE64_TABLE[ b0 >> 2                       ];
        dst[out++] = BASE64_TABLE[(b0 & 0x03) << 4 | (b1 >> 4)  ];
        dst[out++] = (i + 1 < src_len)
                   ? BASE64_TABLE[(b1 & 0x0f) << 2 | (b2 >> 6)] : '=';
        dst[out++] = (i + 2 < src_len)
                   ? BASE64_TABLE[ b2 & 0x3f                   ] : '=';
    }
    dst[out] = '\0';
}

/* ======================================================================
 * JNI: encodeToBase64
 * ====================================================================== */

JNIEXPORT jstring JNICALL
Java_ImageEncoder_encodeToBase64(JNIEnv *env, jobject obj,
                                  jbyteArray pixelData,
                                  jint width, jint height, jint channels) {
    (void)obj; (void)width; (void)height; (void)channels;

    jsize  len  = (*env)->GetArrayLength(env, pixelData);
    jbyte *data = (*env)->GetByteArrayElements(env, pixelData, NULL);
    if (!data) return NULL;

    size_t out_len = ((size_t)len + 2) / 3 * 4 + 1;
    char  *buf     = (char *)malloc(out_len);
    if (!buf) {
        (*env)->ReleaseByteArrayElements(env, pixelData, data, JNI_ABORT);
        (*env)->ThrowNew(env,
            (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
            "base64 output buffer allocation failed");
        return NULL;
    }

    base64_encode((const unsigned char *)data, (size_t)len, buf);
    (*env)->ReleaseByteArrayElements(env, pixelData, data, JNI_ABORT);

    jstring result = (*env)->NewStringUTF(env, buf);
    free(buf);
    return result;
}

/* ======================================================================
 * JNI: encodeWithRLE
 * ====================================================================== */

JNIEXPORT jbyteArray JNICALL
Java_ImageEncoder_encodeWithRLE(JNIEnv *env, jobject obj,
                                 jbyteArray pixelData,
                                 jint width, jint height, jint channels) {
    (void)obj; (void)width; (void)height; (void)channels;

    jsize  len  = (*env)->GetArrayLength(env, pixelData);
    jbyte *data = (*env)->GetByteArrayElements(env, pixelData, NULL);
    if (!data) return NULL;

    /* Worst case: every byte differs → 2 × len output bytes */
    jbyte *out = (jbyte *)malloc((size_t)len * 2);
    if (!out) {
        (*env)->ReleaseByteArrayElements(env, pixelData, data, JNI_ABORT);
        (*env)->ThrowNew(env,
            (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
            "RLE output buffer allocation failed");
        return NULL;
    }

    jsize out_pos = 0;
    jsize i       = 0;
    while (i < len) {
        jbyte val   = data[i];
        jsize count = 1;
        while (i + count < len && data[i + count] == val && count < 255)
            count++;
        out[out_pos++] = (jbyte)count;
        out[out_pos++] = val;
        i += count;
    }

    (*env)->ReleaseByteArrayElements(env, pixelData, data, JNI_ABORT);

    jbyteArray result = (*env)->NewByteArray(env, out_pos);
    if (result)
        (*env)->SetByteArrayRegion(env, result, 0, out_pos, out);

    free(out);
    return result;
}

/* ======================================================================
 * JNI: encodeToHex
 * ====================================================================== */

JNIEXPORT jstring JNICALL
Java_ImageEncoder_encodeToHex(JNIEnv *env, jobject obj,
                               jbyteArray pixelData,
                               jint width, jint height, jint channels) {
    (void)obj; (void)width; (void)height; (void)channels;

    jsize  len  = (*env)->GetArrayLength(env, pixelData);
    jbyte *data = (*env)->GetByteArrayElements(env, pixelData, NULL);
    if (!data) return NULL;

    /* 2 hex chars per byte + NUL terminator */
    char *buf = (char *)malloc((size_t)len * 2 + 1);
    if (!buf) {
        (*env)->ReleaseByteArrayElements(env, pixelData, data, JNI_ABORT);
        (*env)->ThrowNew(env,
            (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
            "hex output buffer allocation failed");
        return NULL;
    }

    jsize i;
    for (i = 0; i < len; i++)
        sprintf(buf + i * 2, "%02x", (unsigned char)data[i]);
    buf[len * 2] = '\0';

    (*env)->ReleaseByteArrayElements(env, pixelData, data, JNI_ABORT);

    jstring result = (*env)->NewStringUTF(env, buf);
    free(buf);
    return result;
}

/* ======================================================================
 * JNI: getNativeLibraryInfo
 * ====================================================================== */

JNIEXPORT jstring JNICALL
Java_ImageEncoder_getNativeLibraryInfo(JNIEnv *env, jobject obj) {
    (void)obj;
    return (*env)->NewStringUTF(env,
        "ImageEncoder Native Library v1.0.0 | "
        "Encodings: Base64 (RFC 4648), RLE, Hex | "
        "Built with ANSI C (no third-party deps)");
}