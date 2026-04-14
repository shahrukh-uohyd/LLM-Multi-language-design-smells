// src/headers/sensor_inspector.h
#ifndef SENSOR_INSPECTOR_H
#define SENSOR_INSPECTOR_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     SensorInspector
 * Method:    findExceedingReadings
 * Signature: ([LSensorReading;[LSensorThreshold;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_SensorInspector_findExceedingReadings
  (JNIEnv *, jobject, jobjectArray, jobjectArray);

/*
 * Class:     SensorInspector
 * Method:    analyzeMinMaxValues
 * Signature: ([LSensorReading;)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_SensorInspector_analyzeMinMaxValues
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     SensorInspector
 * Method:    identifyCriticalReadings
 * Signature: ([LSensorReading;DLjava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_SensorInspector_identifyCriticalReadings
  (JNIEnv *, jobject, jobjectArray, jdouble, jstring);

#ifdef __cplusplus
}
#endif

#endif // SENSOR_INSPECTOR_H