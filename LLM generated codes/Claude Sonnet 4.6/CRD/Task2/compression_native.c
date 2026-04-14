/*
 * compression_native.c
 *
 * JNI implementation of com.app.compression.CompressionNative.
 *
 * Dependencies: zlib  (link with -lz)
 * Compile:  gcc -shared -fPIC -O2 -Wall -Wextra \
 *               -I"${JAVA_HOME}/include" \
 *               -I"${JAVA_HOME}/include/linux" \   <- or /darwin, /win32
 *               compression_native.c \
 *               -lz \
 *               -o libcompression_native.so
 */

#include <jni.h>
#include <zlib.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

/* -----------------------------------------------------------------------
 * Fully-qualified JNI class name (dots replaced by slashes)
 * --------------------------------------------------------------------- */
#define CLASS_PATH          "com/app/compression/CompressionNative"
#define EXCEPTION_CLASS     "com/app/compression/CompressionException"
#define IAE_CLASS           "java/lang/IllegalArgumentException"
#define OOM_CLASS           "java/lang/OutOfMemoryError"

/* -----------------------------------------------------------------------
 * Internal helper: throw a Java exception of the given class
 * --------------------------------------------------------------------- */
static void throw_exception(JNIEnv *env, const char *className, const char *message) {
    jclass exClass = (*env)->FindClass(env, className);
    if (exClass != NULL) {
        (*env)->ThrowNew(env, exClass, message);
        (*env)->DeleteLocalRef(env, exClass);
    }
}

/* -----------------------------------------------------------------------
 * Internal helper: throw CompressionException with a zlib error message
 * --------------------------------------------------------------------- */
static void throw_compression_exception(JNIEnv *env, const char *context, int zlibError,
                                        const z_stream *strm) {
    char msg[512];
    const char *zlibMsg = (strm && strm->msg) ? strm->msg : zError(zlibError);

    /* Build the two-arg CompressionException(String, int) manually */
    jclass exClass = (*env)->FindClass(env, EXCEPTION_CLASS);
    if (exClass == NULL) return; /* FindClass already threw NoClassDefFoundError */

    jmethodID ctor = (*env)->GetMethodID(env, exClass, "<init>", "(Ljava/lang/String;I)V");
    if (ctor == NULL) {
        (*env)->DeleteLocalRef(env, exClass);
        return;
    }

    snprintf(msg, sizeof(msg), "%s: %s", context, zlibMsg ? zlibMsg : "unknown zlib error");

    jstring jmsg = (*env)->NewStringUTF(env, msg);
    if (jmsg == NULL) {
        (*env)->DeleteLocalRef(env, exClass);
        return; /* OOM already pending */
    }

    jobject exception = (*env)->NewObject(env, exClass, ctor, jmsg, (jint)zlibError);
    if (exception != NULL) {
        (*env)->Throw(env, (jthrowable)exception);
        (*env)->DeleteLocalRef(env, exception);
    }

    (*env)->DeleteLocalRef(env, jmsg);
    (*env)->DeleteLocalRef(env, exClass);
}

/* ═══════════════════════════════════════════════════════════════════════
 * compress(byte[] data, int compressionLevel) -> byte[]
 *
 * JNI mangled name:
 *   Java_com_app_compression_CompressionNative_compress
 * ═══════════════════════════════════════════════════════════════════════ */
JNIEXPORT jbyteArray JNICALL
Java_com_app_compression_CompressionNative_compress(JNIEnv *env, jobject thiz,
                                                     jbyteArray jData,
                                                     jint compressionLevel) {
    (void)thiz;

    /* --- validate arguments (Java side already checked, but be defensive) --- */
    if (jData == NULL) {
        throw_exception(env, IAE_CLASS, "data must not be null");
        return NULL;
    }
    jsize inputLen = (*env)->GetArrayLength(env, jData);
    if (inputLen == 0) {
        throw_exception(env, IAE_CLASS, "data must not be empty");
        return NULL;
    }
    if (compressionLevel != Z_DEFAULT_COMPRESSION &&
        (compressionLevel < 0 || compressionLevel > 9)) {
        throw_exception(env, IAE_CLASS, "compressionLevel must be -1 or in [0, 9]");
        return NULL;
    }

    /* --- pin input array --- */
    jbyte *input = (*env)->GetByteArrayElements(env, jData, NULL);
    if (input == NULL) {
        throw_exception(env, OOM_CLASS, "failed to pin input byte array");
        return NULL;
    }

    /* --- allocate output buffer: compressBound gives a safe upper bound --- */
    uLong outBound = compressBound((uLong)inputLen);
    Bytef *output  = (Bytef *)malloc(outBound);
    if (output == NULL) {
        (*env)->ReleaseByteArrayElements(env, jData, input, JNI_ABORT);
        throw_exception(env, OOM_CLASS, "failed to allocate compression output buffer");
        return NULL;
    }

    /* --- compress --- */
    z_stream strm;
    memset(&strm, 0, sizeof(strm));

    int rc = deflateInit(&strm, compressionLevel);
    if (rc != Z_OK) {
        free(output);
        (*env)->ReleaseByteArrayElements(env, jData, input, JNI_ABORT);
        throw_compression_exception(env, "deflateInit failed", rc, &strm);
        return NULL;
    }

    strm.next_in   = (Bytef *)input;
    strm.avail_in  = (uInt)inputLen;
    strm.next_out  = output;
    strm.avail_out = (uInt)outBound;

    rc = deflate(&strm, Z_FINISH);
    uLong compressedLen = strm.total_out;
    deflateEnd(&strm);

    (*env)->ReleaseByteArrayElements(env, jData, input, JNI_ABORT);

    if (rc != Z_STREAM_END) {
        free(output);
        throw_compression_exception(env, "deflate failed", rc, NULL);
        return NULL;
    }

    /* --- copy result into a new Java byte[] --- */
    jbyteArray result = (*env)->NewByteArray(env, (jsize)compressedLen);
    if (result == NULL) {
        free(output);
        throw_exception(env, OOM_CLASS, "failed to allocate result byte array");
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)compressedLen, (const jbyte *)output);
    free(output);

    return result;
}

/* ═══════════════════════════════════════════════════════════════════════
 * decompress(byte[] compressedData, int originalSize) -> byte[]
 *
 * JNI mangled name:
 *   Java_com_app_compression_CompressionNative_decompress
 * ═══════════════════════════════════════════════════════════════════════ */
JNIEXPORT jbyteArray JNICALL
Java_com_app_compression_CompressionNative_decompress(JNIEnv *env, jobject thiz,
                                                       jbyteArray jCompressedData,
                                                       jint originalSize) {
    (void)thiz;

    /* --- validate --- */
    if (jCompressedData == NULL) {
        throw_exception(env, IAE_CLASS, "compressedData must not be null");
        return NULL;
    }
    jsize inputLen = (*env)->GetArrayLength(env, jCompressedData);
    if (inputLen == 0) {
        throw_exception(env, IAE_CLASS, "compressedData must not be empty");
        return NULL;
    }

    /* --- pin input --- */
    jbyte *input = (*env)->GetByteArrayElements(env, jCompressedData, NULL);
    if (input == NULL) {
        throw_exception(env, OOM_CLASS, "failed to pin compressedData byte array");
        return NULL;
    }

    /*
     * Output buffer strategy:
     *  - If originalSize > 0 use it directly.
     *  - Otherwise start at 4× the compressed size and grow if needed.
     */
    uLong outCapacity = (originalSize > 0)
                            ? (uLong)originalSize
                            : (uLong)(inputLen * 4);
    if (outCapacity < 1024) outCapacity = 1024; /* minimum sensible buffer */

    Bytef *output = (Bytef *)malloc(outCapacity);
    if (output == NULL) {
        (*env)->ReleaseByteArrayElements(env, jCompressedData, input, JNI_ABORT);
        throw_exception(env, OOM_CLASS, "failed to allocate decompression output buffer");
        return NULL;
    }

    /* --- decompress, growing the buffer if Z_BUF_ERROR --- */
    z_stream strm;
    memset(&strm, 0, sizeof(strm));

    int rc = inflateInit(&strm);
    if (rc != Z_OK) {
        free(output);
        (*env)->ReleaseByteArrayElements(env, jCompressedData, input, JNI_ABORT);
        throw_compression_exception(env, "inflateInit failed", rc, &strm);
        return NULL;
    }

    strm.next_in  = (Bytef *)input;
    strm.avail_in = (uInt)inputLen;

    uLong totalOut = 0;

    for (;;) {
        strm.next_out  = output + totalOut;
        strm.avail_out = (uInt)(outCapacity - totalOut);

        rc = inflate(&strm, Z_FINISH);
        totalOut = strm.total_out;

        if (rc == Z_STREAM_END) {
            break; /* success */
        }

        if (rc == Z_BUF_ERROR || strm.avail_out == 0) {
            /* Output buffer too small — double it */
            if (originalSize > 0) {
                /*
                 * Caller supplied an originalSize hint that was wrong;
                 * switch to auto-grow mode.
                 */
            }
            uLong newCapacity = outCapacity * 2;
            Bytef *bigger = (Bytef *)realloc(output, newCapacity);
            if (bigger == NULL) {
                inflateEnd(&strm);
                free(output);
                (*env)->ReleaseByteArrayElements(env, jCompressedData, input, JNI_ABORT);
                throw_exception(env, OOM_CLASS,
                                "out of memory growing decompression buffer");
                return NULL;
            }
            output      = bigger;
            outCapacity = newCapacity;
            /* Reset stream output pointers and continue */
            continue;
        }

        /* Any other error is fatal */
        inflateEnd(&strm);
        free(output);
        (*env)->ReleaseByteArrayElements(env, jCompressedData, input, JNI_ABORT);
        throw_compression_exception(env, "inflate failed", rc, &strm);
        return NULL;
    }

    inflateEnd(&strm);
    (*env)->ReleaseByteArrayElements(env, jCompressedData, input, JNI_ABORT);

    /* --- copy result into Java byte[] --- */
    jbyteArray result = (*env)->NewByteArray(env, (jsize)totalOut);
    if (result == NULL) {
        free(output);
        throw_exception(env, OOM_CLASS, "failed to allocate result byte array");
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)totalOut, (const jbyte *)output);
    free(output);

    return result;
}

/* ═══════════════════════════════════════════════════════════════════════
 * compressBound(long inputSize) -> long
 *
 * JNI mangled name:
 *   Java_com_app_compression_CompressionNative_compressBound
 * ═══════════════════════════════════════════════════════════════════════ */
JNIEXPORT jlong JNICALL
Java_com_app_compression_CompressionNative_compressBound(JNIEnv *env, jobject thiz,
                                                          jlong inputSize) {
    (void)thiz;
    if (inputSize < 0) {
        throw_exception(env, IAE_CLASS, "inputSize must be >= 0");
        return -1L;
    }
    return (jlong)compressBound((uLong)inputSize);
}

/* ═══════════════════════════════════════════════════════════════════════
 * zlibVersion() -> String
 *
 * JNI mangled name:
 *   Java_com_app_compression_CompressionNative_zlibVersion
 * ═══════════════════════════════════════════════════════════════════════ */
JNIEXPORT jstring JNICALL
Java_com_app_compression_CompressionNative_zlibVersion(JNIEnv *env, jobject thiz) {
    (void)thiz;
    return (*env)->NewStringUTF(env, zlibVersion());
}