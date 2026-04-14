#ifndef NATIVE_CALCULATION_BRIDGE_H
#define NATIVE_CALCULATION_BRIDGE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * JNI implementation for NativeCalculationBridge.startCalculation().
 *
 * Signature (Java): public native int startCalculation(CalculationStatusHandler handler);
 *
 * @param env        The JNI environment pointer.
 * @param thiz       The Java NativeCalculationBridge object instance.
 * @param handler    The Java CalculationStatusHandler object to call back into.
 * @return           0 on success, non-zero error code on failure.
 */
JNIEXPORT jint JNICALL
Java_com_example_jni_NativeCalculationBridge_startCalculation(
    JNIEnv* env,
    jobject thiz,
    jobject handler
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_CALCULATION_BRIDGE_H