#ifndef SERVER_DIAGNOSTICS_H
#define SERVER_DIAGNOSTICS_H

#include <jni.h>
#include <string>

extern "C" {
    // JNI function declarations for hardware metrics
    JNIEXPORT jdouble JNICALL Java_ServerDiagnostics_readCpuCoreTemperatureNative(JNIEnv *env, jobject obj);
    JNIEXPORT jint JNICALL Java_ServerDiagnostics_fetchActiveHardwareInterruptsNative(JNIEnv *env, jobject obj);
    JNIEXPORT jdouble JNICALL Java_ServerDiagnostics_retrievePowerConsumptionNative(JNIEnv *env, jobject obj);
}

// Helper functions for reading hardware metrics
double readCPUTemperature();
int readActiveInterrupts();
double readPowerConsumption();

#endif // SERVER_DIAGNOSTICS_H