#include <jni.h>
#include <stdio.h>

// Existing C implementation for SystemMonitor
JNIEXPORT jdouble JNICALL Java_SystemMonitor_getCpuLoad(JNIEnv *env, jobject thisObj) {
    // OS-specific logic to get CPU load...
    return 25.5; // Mock return value
}

JNIEXPORT jlong JNICALL Java_SystemMonitor_getFreeMemory(JNIEnv *env, jobject thisObj) {
    // OS-specific logic to get free memory...
    return 2048576L; // Mock return value
}