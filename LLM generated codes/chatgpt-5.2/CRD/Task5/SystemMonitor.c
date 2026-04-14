#include <jni.h>
#include <time.h>
#include <unistd.h>
#include <stdio.h>
#include "SystemMonitor.h"

/*
 * long getUptimeMillis()
 */
JNIEXPORT jlong JNICALL
Java_SystemMonitor_getUptimeMillis(JNIEnv *env, jobject obj) {
    return (jlong)time(NULL) * 1000;
}

/*
 * int getAvailableProcessors()
 */
JNIEXPORT jint JNICALL
Java_SystemMonitor_getAvailableProcessors(JNIEnv *env, jobject obj) {
    return (jint)sysconf(_SC_NPROCESSORS_ONLN);
}

/*
 * void loadConfiguration(String configPath)
 * Simulated native configuration loading
 */
JNIEXPORT void JNICALL
Java_SystemMonitor_loadConfiguration(JNIEnv *env,
                                     jobject obj,
                                     jstring configPath) {

    const char *path = (*env)->GetStringUTFChars(env, configPath, NULL);

    // Simulate native configuration handling
    printf("[native] Loading configuration from: %s\n", path);

    (*env)->ReleaseStringUTFChars(env, configPath, path);
}
