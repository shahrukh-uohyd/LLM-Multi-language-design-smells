#include <jni.h>
#include "SystemDiagnostics.h"

/*
 * double nativeReadCpuTemperature()
 * Simulated hardware metric
 */
JNIEXPORT jdouble JNICALL
Java_SystemDiagnostics_nativeReadCpuTemperature(JNIEnv*, jobject) {
    // Example: read from sensor in real system
    return 72.5;
}

/*
 * long nativeGetInterruptCount()
 * Simulated hardware metric
 */
JNIEXPORT jlong JNICALL
Java_SystemDiagnostics_nativeGetInterruptCount(JNIEnv*, jobject) {
    // Example: read from /proc or hardware registers
    return 420000;
}

/*
 * double nativeGetPowerConsumption()
 * Simulated hardware metric
 */
JNIEXPORT jdouble JNICALL
Java_SystemDiagnostics_nativeGetPowerConsumption(JNIEnv*, jobject) {
    // Example: read from power management controller
    return 95.0;
}
