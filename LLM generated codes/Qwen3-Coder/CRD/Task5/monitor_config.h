// monitor_config.h
#include <jni.h>

#ifndef _MONITOR_CONFIG_H
#define _MONITOR_CONFIG_H

#ifdef __cplusplus
extern "C" {
#endif

// System monitoring methods
JNIEXPORT jlong JNICALL Java_SystemMonitor_getFreeMemory(JNIEnv *, jobject);
JNIEXPORT jlong JNICALL Java_SystemMonitor_getTotalMemory(JNIEnv *, jobject);
JNIEXPORT jdouble JNICALL Java_SystemMonitor_getCpuUsage(JNIEnv *, jobject);
JNIEXPORT jstring JNICALL Java_SystemMonitor_getSystemLoadAverage(JNIEnv *, jobject);
JNIEXPORT jlong JNICALL Java_SystemMonitor_getDiskUsage(JNIEnv *, jobject);
JNIEXPORT jstring JNICALL Java_SystemMonitor_getNetworkStatus(JNIEnv *, jobject);
JNIEXPORT jint JNICALL Java_SystemMonitor_getRunningProcesses(JNIEnv *, jobject);
JNIEXPORT jstring JNICALL Java_SystemMonitor_getSystemUptime(JNIEnv *, jobject);

// Configuration management methods
JNIEXPORT jboolean JNICALL Java_SystemMonitor_saveConfiguration(JNIEnv *, jobject, jstring, jstring);
JNIEXPORT jstring JNICALL Java_SystemMonitor_loadConfiguration(JNIEnv *, jobject, jstring);
JNIEXPORT jboolean JNICALL Java_SystemMonitor_updateConfigurationValue(JNIEnv *, jobject, jstring, jstring, jstring);
JNIEXPORT jstring JNICALL Java_SystemMonitor_getConfigurationValue(JNIEnv *, jobject, jstring, jstring);
JNIEXPORT jobjectArray JNICALL Java_SystemMonitor_listConfigurationKeys(JNIEnv *, jobject, jstring);
JNIEXPORT jboolean JNICALL Java_SystemMonitor_validateConfiguration(JNIEnv *, jobject, jstring);
JNIEXPORT jboolean JNICALL Java_SystemMonitor_backupConfiguration(JNIEnv *, jobject, jstring, jstring);
JNIEXPORT jboolean JNICALL Java_SystemMonitor_restoreConfiguration(JNIEnv *, jobject, jstring, jstring);
JNIEXPORT jboolean JNICALL Java_SystemMonitor_encryptConfiguration(JNIEnv *, jobject, jstring, jstring);
JNIEXPORT jboolean JNICALL Java_SystemMonitor_decryptConfiguration(JNIEnv *, jobject, jstring, jstring);

#ifdef __cplusplus
}
#endif

#endif