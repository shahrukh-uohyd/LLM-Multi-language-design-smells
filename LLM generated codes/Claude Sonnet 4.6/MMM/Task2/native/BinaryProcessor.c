/*
 * BinaryProcessor.c
 *
 * JNI implementation for BinaryProcessor native methods.
 *
 * Methods implemented:
 *   Java_BinaryProcessor_inspectPayload   – XOR checksum + popcount
 *   Java_BinaryProcessor_transformPayload – byte-invert then byte-reverse
 *   Java_BinaryProcessor_xorCipherPayload – rolling-XOR encode/decode
 *
 * Compile flags expected:
 *   -I${JAVA_HOME}/include  -I${JAVA_HOME}/include/<platform>
 */

#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* ================================================================== */
/* Internal helpers                                                     */
/* ================================================================== */

/**
 * popcount8 — count the number of set bits in one byte.
 * Falls back to a portable loop if the compiler does not provide
 * __builtin_popcount.
 */
static int popcount8(uint8_t byte) {
#if defined(__GNUC__) || defined(__clang__)
    return __builtin_popcount((unsigned int)byte);
#else
    int n = 0;
    while (byte) { n += byte & 1; byte >>= 1; }
    return n;
#endif
}

/* ================================================================== */
/* 1. inspectPayload                                                     */
/*                                                                      */
/* Returns a jlongArray of length 3:                                    */
/*   [0] XOR checksum of all bytes                                      */
/*   [1] total number of set bits across all bytes                      */
/*   [2] payload length in bytes                                        */
/* ================================================================== */
JNIEXPORT jlongArray JNICALL
Java_BinaryProcessor_inspectPayload(JNIEnv *env,
                                     jobject  obj,
                                     jbyteArray payload)
{
    /* ── allocate the result array first (3 longs) ── */
    jlongArray result = (*env)->NewLongArray(env, 3);
    if (result == NULL) return NULL;   /* OOM already pending */

    jsize len = (*env)->GetArrayLength(env, payload);

    jlong stats[3] = {0, 0, (jlong)len};

    if (len > 0) {
        /* Pin the Java byte array — avoids a copy on many JVMs */
        jbyte *bytes = (*env)->GetByteArrayElements(env, payload, NULL);
        if (bytes == NULL) return NULL;

        uint8_t xorAcc  = 0;
        long    setBits = 0;

        for (jsize i = 0; i < len; i++) {
            uint8_t b  = (uint8_t)bytes[i];
            xorAcc    ^= b;
            setBits   += popcount8(b);
        }

        stats[0] = (jlong)(xorAcc);
        stats[1] = (jlong)(setBits);

        (*env)->ReleaseByteArrayElements(env, payload, bytes, JNI_ABORT);
    }

    (*env)->SetLongArrayRegion(env, result, 0, 3, stats);
    return result;
}

/* ================================================================== */
/* 2. transformPayload                                                   */
/*                                                                      */
/* Two-step pipeline:                                                   */
/*   Step A — Invert every byte  : out[i] = ~in[i]                     */
/*   Step B — Reverse byte order : out[0..n-1] → out[n-1..0]           */
/*                                                                      */
/* Applying the transform twice returns the original data               */
/* (self-inverse), which the Java side verifies as a round-trip check.  */
/* ================================================================== */
JNIEXPORT jbyteArray JNICALL
Java_BinaryProcessor_transformPayload(JNIEnv *env,
                                       jobject  obj,
                                       jbyteArray payload)
{
    jsize len = (*env)->GetArrayLength(env, payload);

    /* Create the output array */
    jbyteArray result = (*env)->NewByteArray(env, len);
    if (result == NULL) return NULL;

    if (len == 0) return result;

    jbyte *src = (*env)->GetByteArrayElements(env, payload, NULL);
    if (src == NULL) return NULL;

    /* Allocate a temporary buffer for the transformation */
    jbyte *tmp = (jbyte *)malloc((size_t)len * sizeof(jbyte));
    if (tmp == NULL) {
        (*env)->ReleaseByteArrayElements(env, payload, src, JNI_ABORT);
        (*env)->ThrowNew(env,
            (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
            "transformPayload: malloc failed");
        return NULL;
    }

    /* Step A: invert each byte */
    for (jsize i = 0; i < len; i++) {
        tmp[i] = (jbyte)(~((uint8_t)src[i]));
    }

    /* Step B: reverse byte order in-place */
    jsize lo = 0, hi = len - 1;
    while (lo < hi) {
        jbyte t  = tmp[lo];
        tmp[lo]  = tmp[hi];
        tmp[hi]  = t;
        lo++; hi--;
    }

    (*env)->ReleaseByteArrayElements(env, payload, src, JNI_ABORT);
    (*env)->SetByteArrayRegion(env, result, 0, len, tmp);
    free(tmp);

    return result;
}

/* ================================================================== */
/* 3. xorCipherPayload                                                  */
/*                                                                      */
/* Rolling-XOR cipher / decipher:                                       */
/*   out[i] = in[i] ^ ((seed + i) & 0xFF)                              */
/*                                                                      */
/* XOR is self-inverse: calling with the same seed on the ciphertext    */
/* recovers the original plaintext exactly.                             */
/* ================================================================== */
JNIEXPORT jbyteArray JNICALL
Java_BinaryProcessor_xorCipherPayload(JNIEnv *env,
                                       jobject  obj,
                                       jbyteArray payload,
                                       jint       seed)
{
    jsize len = (*env)->GetArrayLength(env, payload);

    jbyteArray result = (*env)->NewByteArray(env, len);
    if (result == NULL) return NULL;

    if (len == 0) return result;

    jbyte *src = (*env)->GetByteArrayElements(env, payload, NULL);
    if (src == NULL) return NULL;

    jbyte *tmp = (jbyte *)malloc((size_t)len * sizeof(jbyte));
    if (tmp == NULL) {
        (*env)->ReleaseByteArrayElements(env, payload, src, JNI_ABORT);
        (*env)->ThrowNew(env,
            (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
            "xorCipherPayload: malloc failed");
        return NULL;
    }

    uint8_t key = (uint8_t)(seed & 0xFF);

    for (jsize i = 0; i < len; i++) {
        uint8_t k  = (uint8_t)((key + (uint8_t)i) & 0xFF);
        tmp[i]     = (jbyte)((uint8_t)src[i] ^ k);
    }

    (*env)->ReleaseByteArrayElements(env, payload, src, JNI_ABORT);
    (*env)->SetByteArrayRegion(env, result, 0, len, tmp);
    free(tmp);

    return result;
}