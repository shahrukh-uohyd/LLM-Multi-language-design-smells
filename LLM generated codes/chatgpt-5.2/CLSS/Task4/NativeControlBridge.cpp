#include <jni.h>
#include <string>
#include "NativeControlBridge.h"

JNIEXPORT void JNICALL
Java_NativeControlBridge_executeNativeControl(JNIEnv* env, jobject) {

    // Locate Java component class
    jclass controllerClass = env->FindClass(
        "com/example/app/ServiceController"
    );
    if (controllerClass == nullptr) {
        env->ExceptionClear();
        return;
    }

    // Locate startService(int)
    jmethodID startMethod = env->GetStaticMethodID(
        controllerClass,
        "startService",
        "(I)V"
    );

    // Locate stopService(String)
    jmethodID stopMethod = env->GetStaticMethodID(
        controllerClass,
        "stopService",
        "(Ljava/lang/String;)V"
    );

    if (startMethod == nullptr || stopMethod == nullptr) {
        env->ExceptionClear();
        return;
    }

    // Trigger Java functionality
    env->CallStaticVoidMethod(
        controllerClass,
        startMethod,
        3
    );

    jstring reason = env->NewStringUTF(
        "Native module completed work"
    );

    env->CallStaticVoidMethod(
        controllerClass,
        stopMethod,
        reason
    );

    env->DeleteLocalRef(reason);
}
