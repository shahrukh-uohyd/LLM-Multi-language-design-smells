#ifndef NATIVE_PLUGIN_BRIDGE_H
#define NATIVE_PLUGIN_BRIDGE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Locates a Java plugin via PluginRegistry, builds a PluginContext,
 * invokes plugin.execute(context), reads the PluginResult, and
 * publishes the outcome to PluginEventBus.
 *
 * Java signature:
 *   public native int executePlugin(String   pluginId,
 *                                   String   operationName,
 *                                   String[] paramKeys,
 *                                   String[] paramValues,
 *                                   int      flags);
 * JNI return type: jint (the PluginResult code)
 */
JNIEXPORT jint JNICALL
Java_com_example_plugins_NativePluginBridge_executePlugin(
    JNIEnv*      env,
    jobject      thiz,
    jstring      pluginId,
    jstring      operationName,
    jobjectArray paramKeys,
    jobjectArray paramValues,
    jint         flags
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_PLUGIN_BRIDGE_H