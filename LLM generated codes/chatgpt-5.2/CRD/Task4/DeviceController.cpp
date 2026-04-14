#include <jni.h>
#include <vector>
#include <numeric>
#include <iostream>
#include "DeviceController.h"

/*
 * void openDevice()
 */
JNIEXPORT void JNICALL
Java_DeviceController_openDevice(JNIEnv *, jobject) {
    std::cout << "[native] Device opened" << std::endl;
}

/*
 * void closeDevice()
 */
JNIEXPORT void JNICALL
Java_DeviceController_closeDevice(JNIEnv *, jobject) {
    std::cout << "[native] Device closed" << std::endl;
}

/*
 * int readRawSignal()
 */
JNIEXPORT jint JNICALL
Java_DeviceController_readRawSignal(JNIEnv *, jobject) {
    // Simulated hardware read
    return 42;
}

/*
 * double[] processSignal(int[] rawSignal)
 * Example signal processing: normalization
 */
JNIEXPORT jdoubleArray JNICALL
Java_DeviceController_processSignal(JNIEnv *env,
                                    jobject,
                                    jintArray input) {

    jsize len = env->GetArrayLength(input);
    jint *raw = env->GetIntArrayElements(input, nullptr);

    // Find max value for normalization
    int maxVal = 1;
    for (jsize i = 0; i < len; i++) {
        if (raw[i] > maxVal) maxVal = raw[i];
    }

    // Create output array
    jdoubleArray result = env->NewDoubleArray(len);
    std::vector<jdouble> normalized(len);

    for (jsize i = 0; i < len; i++) {
        normalized[i] = static_cast<double>(raw[i]) / maxVal;
    }

    env->SetDoubleArrayRegion(result, 0, len, normalized.data());
    env->ReleaseIntArrayElements(input, raw, JNI_ABORT);

    return result;
}
