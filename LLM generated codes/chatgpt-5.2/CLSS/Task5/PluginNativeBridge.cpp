#include <jni.h>
#include "PluginNativeBridge.h"

JNIEXPORT void JNICALL
Java_PluginNativeBridge_invokePlugin(JNIEnv* env,
                                     jobject,
                                     jobject pluginObj) {

    if (pluginObj == nullptr) {
        return;
    }

    // Get the plugin's runtime class
    jclass pluginClass = env->GetObjectClass(pluginObj);
    if (pluginClass == nullptr) {
        env->ExceptionClear();
        return;
    }

    // Locate the execute() method
    jmethodID executeMethod = env->GetMethodID(
        pluginClass,
        "execute",
        "()V"
    );

    if (executeMethod == nullptr) {
        env->ExceptionClear();
        return;
    }

    // Invoke plugin behavior
    env->CallVoidMethod(pluginObj, executeMethod);
}
