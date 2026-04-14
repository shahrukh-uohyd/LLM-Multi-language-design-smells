#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "TextProcessingPipeline.h"

/*
 * int[] nativeParse(String input)
 * Parses comma-separated integers
 */
JNIEXPORT jintArray JNICALL
Java_TextProcessingPipeline_nativeParse(JNIEnv *env,
                                        jobject obj,
                                        jstring input) {

    const char *text = (*env)->GetStringUTFChars(env, input, NULL);

    // Count elements
    int count = 1;
    for (const char *p = text; *p; p++) {
        if (*p == ',') count++;
    }

    jintArray result = (*env)->NewIntArray(env, count);
    jint *values = (jint *)malloc(sizeof(jint) * count);

    char *copy = strdup(text);
    char *token = strtok(copy, ",");

    int i = 0;
    while (token != NULL) {
        values[i++] = atoi(token);
        token = strtok(NULL, ",");
    }

    (*env)->SetIntArrayRegion(env, result, 0, count, values);

    free(values);
    free(copy);
    (*env)->ReleaseStringUTFChars(env, input, text);

    return result;
}

/*
 * int[] nativeProcess(int[] elements)
 * Example logic: square each element
 */
JNIEXPORT jintArray JNICALL
Java_TextProcessingPipeline_nativeProcess(JNIEnv *env,
                                          jobject obj,
                                          jintArray elements) {

    jsize len = (*env)->GetArrayLength(env, elements);
    jint *data = (*env)->GetIntArrayElements(env, elements, NULL);

    jintArray result = (*env)->NewIntArray(env, len);
    jint *processed = (jint *)malloc(sizeof(jint) * len);

    for (jsize i = 0; i < len; i++) {
        processed[i] = data[i] * data[i];
    }

    (*env)->SetIntArrayRegion(env, result, 0, len, processed);

    free(processed);
    (*env)->ReleaseIntArrayElements(env, elements, data, JNI_ABORT);

    return result;
}

/*
 * int nativeGenerate(int[] processed)
 * Example: sum all elements
 */
JNIEXPORT jint JNICALL
Java_TextProcessingPipeline_nativeGenerate(JNIEnv *env,
                                           jobject obj,
                                           jintArray processed) {

    jsize len = (*env)->GetArrayLength(env, processed);
    jint *data = (*env)->GetIntArrayElements(env, processed, NULL);

    int sum = 0;
    for (jsize i = 0; i < len; i++) {
        sum += data[i];
    }

    (*env)->ReleaseIntArrayElements(env, processed, data, JNI_ABORT);
    return sum;
}
