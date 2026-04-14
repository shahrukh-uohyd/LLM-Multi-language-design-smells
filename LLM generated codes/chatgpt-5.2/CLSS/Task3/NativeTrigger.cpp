#include <jni.h>
#include <string>
#include "NativeTrigger.h"

JNIEXPORT void JNICALL
Java_NativeTrigger_triggerOperation(JNIEnv* env, jobject) {

    // Locate Java class in package
    jclass handlerClass = env->FindClass(
        "com/example/logic/OperationHandler"
    );
    if (handlerClass == nullptr) {
        env->ExceptionClear();
        return;
    }

    // Locate static method:
    // performOperation(int, String)
    jmethodID method = env->GetStaticMethodID(
        handlerClass,
        "performOperation",
        "(ILjava/lang/String;)V"
    );
    if (method == nullptr) {
        env->ExceptionClear();
        return;
    }

    // Create Java String argument
    jstring message = env->NewStringUTF(
        "Triggered from native C++ code"
    );

    // Invoke Java logic
    env->CallStaticVoidMethod(
        handlerClass,
        method,
        42,
        message
    );

    // Cleanup local references
    env->DeleteLocalRef(message);
}
