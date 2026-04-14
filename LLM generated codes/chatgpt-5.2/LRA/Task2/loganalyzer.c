#include <jni.h>

JNIEXPORT jintArray JNICALL
Java_com_example_logs_NativeLogAnalyzer_countBySeverity(
        JNIEnv *env,
        jclass,
        jobjectArray events,
        jint severityCount) {

    if (events == NULL || severityCount <= 0) {
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, events);

    // Allocate result array
    jintArray result = (*env)->NewIntArray(env, severityCount);
    if (result == NULL) {
        return NULL; // Out of memory
    }

    jint *counts = (*env)->GetIntArrayElements(env, result, NULL);
    if (counts == NULL) {
        return NULL;
    }

    // Zero-initialize
    for (int i = 0; i < severityCount; i++) {
        counts[i] = 0;
    }

    // Look up classes and field IDs once
    jclass logEventClass =
            (*env)->FindClass(env, "com/example/logs/LogEvent");
    if (logEventClass == NULL) goto cleanup;

    jfieldID severityField =
            (*env)->GetFieldID(env, logEventClass,
                               "severity",
                               "Lcom/example/logs/Severity;");
    if (severityField == NULL) goto cleanup;

    jclass severityClass =
            (*env)->FindClass(env, "com/example/logs/Severity");
    if (severityClass == NULL) goto cleanup;

    jmethodID ordinalMethod =
            (*env)->GetMethodID(env, severityClass,
                                "ordinal", "()I");
    if (ordinalMethod == NULL) goto cleanup;

    for (jsize i = 0; i < length; i++) {
        jobject event =
                (*env)->GetObjectArrayElement(env, events, i);
        if (event == NULL) continue;

        jobject severity =
                (*env)->GetObjectField(env, event, severityField);
        if (severity != NULL) {
            jint ordinal =
                (*env)->CallIntMethod(env, severity, ordinalMethod);

            if (ordinal >= 0 && ordinal < severityCount) {
                counts[ordinal]++;
            }
            (*env)->DeleteLocalRef(env, severity);
        }

        (*env)->DeleteLocalRef(env, event);
    }

cleanup:
    (*env)->ReleaseIntArrayElements(env, result, counts, 0);
    return result;
}
