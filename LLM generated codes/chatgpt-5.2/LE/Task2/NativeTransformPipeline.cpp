#include <jni.h>
#include <vector>
#include "NativeTransformPipeline.h"

/*
 * int[] nativeReadAndInterpret(byte[] rawData)
 * Example: convert bytes to unsigned integer values
 */
JNIEXPORT jintArray JNICALL
Java_NativeTransformPipeline_nativeReadAndInterpret(JNIEnv* env,
                                                     jobject,
                                                     jbyteArray rawData) {

    jsize len = env->GetArrayLength(rawData);
    jbyte* data = env->GetByteArrayElements(rawData, nullptr);

    std::vector<jint> interpreted(len);
    for (jsize i = 0; i < len; i++) {
        interpreted[i] = static_cast<unsigned char>(data[i]);
    }

    env->ReleaseByteArrayElements(rawData, data, JNI_ABORT);

    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, interpreted.data());
    return result;
}

/*
 * int[] nativeApplyTransform(int[] interpretedData)
 * Example: apply scaling rule
 */
JNIEXPORT jintArray JNICALL
Java_NativeTransformPipeline_nativeApplyTransform(JNIEnv* env,
                                                   jobject,
                                                   jintArray interpretedData) {

    jsize len = env->GetArrayLength(interpretedData);
    jint* data = env->GetIntArrayElements(interpretedData, nullptr);

    std::vector<jint> transformed(len);
    for (jsize i = 0; i < len; i++) {
        transformed[i] = data[i] * 2;  // example rule
    }

    env->ReleaseIntArrayElements(interpretedData, data, JNI_ABORT);

    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, transformed.data());
    return result;
}

/*
 * byte[] nativeGenerateOutput(int[] transformedData)
 * Example: clamp and convert back to bytes
 */
JNIEXPORT jbyteArray JNICALL
Java_NativeTransformPipeline_nativeGenerateOutput(JNIEnv* env,
                                                   jobject,
                                                   jintArray transformedData) {

    jsize len = env->GetArrayLength(transformedData);
    jint* data = env->GetIntArrayElements(transformedData, nullptr);

    std::vector<jbyte> output(len);
    for (jsize i = 0; i < len; i++) {
        int value = data[i];
        if (value > 255) value = 255;
        output[i] = static_cast<jbyte>(value);
    }

    env->ReleaseIntArrayElements(transformedData, data, JNI_ABORT);

    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, output.data());
    return result;
}
