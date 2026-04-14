// src/headers/device_monitor.h
#ifndef DEVICE_MONITOR_H
#define DEVICE_MONITOR_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     DeviceFailureAnalyzer
 * Method:    identifyRepeatedFailures
 * Signature: ([LDeviceStatusUpdate;IJ)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_DeviceFailureAnalyzer_identifyRepeatedFailures
  (JNIEnv *, jobject, jobjectArray, jint, jlong);

/*
 * Class:     DeviceFailureAnalyzer
 * Method:    analyzeDeviceFailureFrequency
 * Signature: ([LDeviceStatusUpdate;)Ljava/util/HashMap;
 */
JNIEXPORT jobject JNICALL Java_DeviceFailureAnalyzer_analyzeDeviceFailureFrequency
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     DeviceFailureAnalyzer
 * Method:    getDevicesWithRecentFailures
 * Signature: ([LDeviceStatusUpdate;J)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_DeviceFailureAnalyzer_getDevicesWithRecentFailures
  (JNIEnv *, jobject, jobjectArray, jlong);

#ifdef __cplusplus
}
#endif

#endif // DEVICE_MONITOR_H