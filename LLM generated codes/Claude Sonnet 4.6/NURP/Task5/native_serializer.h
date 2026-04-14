#ifndef NATIVE_SERIALIZER_H
#define NATIVE_SERIALIZER_H

/*
 * native_serializer.h
 *
 * JNI function declarations for com.serializer.NativeSerializer.
 * All function names follow the JNI mangling convention:
 *   Java_<package_underscored>_<ClassName>_<methodName>
 */

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ── Single-record operations ─────────────────────────────────────────── */

JNIEXPORT jbyteArray JNICALL
Java_com_serializer_NativeSerializer_nativeSerialize(
    JNIEnv *env, jobject obj,
    jlong id, jstring name, jdouble score, jboolean active,
    jobjectArray tags, jbyteArray payload);

JNIEXPORT jobjectArray JNICALL
Java_com_serializer_NativeSerializer_nativeDeserialize(
    JNIEnv *env, jobject obj,
    jbyteArray data, jint offset, jint length);

/* ── Batch operations ─────────────────────────────────────────────────── */

JNIEXPORT jbyteArray JNICALL
Java_com_serializer_NativeSerializer_nativeSerializeBatch(
    JNIEnv *env, jobject obj,
    jlongArray ids, jobjectArray names, jdoubleArray scores,
    jbooleanArray actives, jobjectArray tags, jobjectArray payloads);

JNIEXPORT jobjectArray JNICALL
Java_com_serializer_NativeSerializer_nativeDeserializeBatch(
    JNIEnv *env, jobject obj,
    jbyteArray data, jint length);

/* ── Integrity ────────────────────────────────────────────────────────── */

JNIEXPORT jboolean JNICALL
Java_com_serializer_NativeSerializer_nativeValidateChecksum(
    JNIEnv *env, jobject obj,
    jbyteArray data, jint length);

/* ── Compression ──────────────────────────────────────────────────────── */

JNIEXPORT jbyteArray JNICALL
Java_com_serializer_NativeSerializer_nativeCompressPayload(
    JNIEnv *env, jobject obj,
    jbyteArray data, jint length);

JNIEXPORT jbyteArray JNICALL
Java_com_serializer_NativeSerializer_nativeDecompressPayload(
    JNIEnv *env, jobject obj,
    jbyteArray data, jint length);

#ifdef __cplusplus
}
#endif
#endif /* NATIVE_SERIALIZER_H */