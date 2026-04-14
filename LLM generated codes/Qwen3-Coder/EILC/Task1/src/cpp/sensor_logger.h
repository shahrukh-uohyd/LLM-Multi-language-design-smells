// sensor_logger.h
#ifndef SENSOR_LOGGER_H
#define SENSOR_LOGGER_H

#include <jni.h>
#include <string>
#include <vector>
#include <mutex>
#include <memory>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL Java_NativeDatabaseSync_initializeDatabase
  (JNIEnv *, jobject, jstring);

JNIEXPORT jboolean JNICALL Java_NativeDatabaseSync_syncLogs
  (JNIEnv *, jobject, jobjectArray);

JNIEXPORT jboolean JNICALL Java_NativeDatabaseSync_closeDatabase
  (JNIEnv *, jobject);

JNIEXPORT jint JNICALL Java_NativeDatabaseSync_getPendingSyncCount
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif

#endif