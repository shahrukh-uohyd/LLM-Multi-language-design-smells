// hardware_monitor.h
#ifndef HARDWARE_MONITOR_H
#define HARDWARE_MONITOR_H

#include <jni.h>
#include <string>

#ifdef __cplusplus
extern "C" {
#endif

// Individual metric reading functions
JNIEXPORT jdouble JNICALL Java_SystemProbe_readCPUTemperature
  (JNIEnv *, jobject, jlong);

JNIEXPORT jdouble JNICALL Java_SystemProbe_readGPUTemperature
  (JNIEnv *, jobject, jlong);

JNIEXPORT jdouble JNICALL Java_SystemProbe_readMotherboardTemperature
  (JNIEnv *, jobject, jlong);

JNIEXPORT jint JNICALL Java_SystemProbe_readFan1Speed
  (JNIEnv *, jobject, jlong);

JNIEXPORT jint JNICALL Java_SystemProbe_readFan2Speed
  (JNIEnv *, jobject, jlong);

JNIEXPORT jdouble JNICALL Java_SystemProbe_readBatteryVoltage
  (JNIEnv *, jobject, jlong);

// Optimized single-call function for all metrics
JNIEXPORT jobject JNICALL Java_SystemProbe_performDeepScanAllAtOnce
  (JNIEnv *, jobject, jlong);

// Cleanup function for HardwareHandle
JNIEXPORT void JNICALL Java_HardwareHandle_cleanupNativeResources
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif

#endif