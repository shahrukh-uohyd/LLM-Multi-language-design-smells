#ifndef NATIVE_OPERATION_BRIDGE_H
#define NATIVE_OPERATION_BRIDGE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Triggers: OperationHandler.performOperation(String operationType, int priority)
 *
 * Java signature : public native void triggerInstanceOperation(
 *                      OperationHandler handler, String operationType, int priority);
 * JNI descriptor : (Lcom/example/operations/OperationHandler;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL
Java_com_example_operations_NativeOperationBridge_triggerInstanceOperation(
    JNIEnv* env,
    jobject thiz,
    jobject handler,
    jstring operationType,
    jint    priority
);

/**
 * Triggers: OperationHandler.handleNativeEvent(int eventCode, String payload)
 *
 * Java signature : public native void triggerStaticEvent(int eventCode, String payload);
 * JNI descriptor : (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_example_operations_NativeOperationBridge_triggerStaticEvent(
    JNIEnv* env,
    jobject thiz,
    jint    eventCode,
    jstring payload
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_OPERATION_BRIDGE_H