// device_signal.h
#include <jni.h>

#ifndef _DEVICE_SIGNAL_H
#define _DEVICE_SIGNAL_H

#ifdef __cplusplus
extern "C" {
#endif

// Device control methods
JNIEXPORT jboolean JNICALL Java_DeviceController_connectToDevice(JNIEnv *, jobject, jstring);
JNIEXPORT jboolean JNICALL Java_DeviceController_disconnectDevice(JNIEnv *, jobject, jstring);
JNIEXPORT jboolean JNICALL Java_DeviceController_sendCommand(JNIEnv *, jobject, jstring, jstring);
JNIEXPORT jstring JNICALL Java_DeviceController_readResponse(JNIEnv *, jobject, jstring);
JNIEXPORT jboolean JNICALL Java_DeviceController_configureDevice(JNIEnv *, jobject, jstring, jstring);
JNIEXPORT jboolean JNICALL Java_DeviceController_isDeviceConnected(JNIEnv *, jobject, jstring);
JNIEXPORT jstring JNICALL Java_DeviceController_getDeviceStatus(JNIEnv *, jobject, jstring);
JNIEXPORT jboolean JNICALL Java_DeviceController_resetDevice(JNIEnv *, jobject, jstring);

// Signal processing methods
JNIEXPORT jdoubleArray JNICALL Java_DeviceController_filterSignal(JNIEnv *, jobject, jdoubleArray, jstring, jdouble);
JNIEXPORT jdoubleArray JNICALL Java_DeviceController_fftTransform(JNIEnv *, jobject, jdoubleArray);
JNIEXPORT jdoubleArray JNICALL Java_DeviceController_correlateSignals(JNIEnv *, jobject, jdoubleArray, jdoubleArray);
JNIEXPORT jdouble JNICALL Java_DeviceController_findPeakValue(JNIEnv *, jobject, jdoubleArray);
JNIEXPORT jint JNICALL Java_DeviceController_findPeakIndex(JNIEnv *, jobject, jdoubleArray);
JNIEXPORT jdoubleArray JNICALL Java_DeviceController_applyWindow(JNIEnv *, jobject, jdoubleArray, jstring);
JNIEXPORT jdoubleArray JNICALL Java_DeviceController_convolveSignals(JNIEnv *, jobject, jdoubleArray, jdoubleArray);
JNIEXPORT jdouble JNICALL Java_DeviceController_calculateRMS(JNIEnv *, jobject, jdoubleArray);
JNIEXPORT jdoubleArray JNICALL Java_DeviceController_downsampleSignal(JNIEnv *, jobject, jdoubleArray, jint);

#ifdef __cplusplus
}
#endif

#endif