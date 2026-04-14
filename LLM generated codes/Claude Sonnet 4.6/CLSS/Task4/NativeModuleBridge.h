#ifndef NATIVE_MODULE_BRIDGE_H
#define NATIVE_MODULE_BRIDGE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Locates a Java component via ComponentRegistry and triggers it.
 *
 * Java signature : public native String triggerComponent(
 *                      String componentId, String params, int flags);
 * JNI return type: jstring
 */
JNIEXPORT jstring JNICALL
Java_com_example_app_NativeModuleBridge_triggerComponent(
    JNIEnv* env,
    jobject thiz,
    jstring componentId,
    jstring params,
    jint    flags
);

/**
 * Signals an app lifecycle event via AppLifecycleManager.
 *
 * Java signature : public native void signalLifecycleEvent(
 *                      String event, String reason);
 */
JNIEXPORT void JNICALL
Java_com_example_app_NativeModuleBridge_signalLifecycleEvent(
    JNIEnv* env,
    jobject thiz,
    jstring event,
    jstring reason
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_MODULE_BRIDGE_H