#include <jni.h>
#include <string>
#include <thread>
#include <chrono>
#include "NativeCalculator.h"

JavaVM* g_vm = nullptr;

// Cache the JVM pointer
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm;
    return JNI_VERSION_1_8;
}

void notifyJava(jobject handlerGlobal,
                jmethodID callback,
                int code,
                const std::string& message) {

    JNIEnv* env = nullptr;
    bool didAttach = false;

    // Attach thread if needed
    if (g_vm->GetEnv((void**)&env, JNI_VERSION_1_8) != JNI_OK) {
        g_vm->AttachCurrentThread((void**)&env, nullptr);
        didAttach = true;
    }

    jstring jmsg = env->NewStringUTF(message.c_str());

    env->CallVoidMethod(handlerGlobal, callback, code, jmsg);

    env->DeleteLocalRef(jmsg);

    if (didAttach) {
        g_vm->DetachCurrentThread();
    }
}

JNIEXPORT void JNICALL
Java_NativeCalculator_startCalculation(JNIEnv* env,
                                       jobject,
                                       jobject handler) {

    // Create a global reference to the handler
    jobject handlerGlobal = env->NewGlobalRef(handler);

    jclass handlerClass = env->GetObjectClass(handler);
    jmethodID callback = env->GetMethodID(
        handlerClass,
        "onStatus",
        "(ILjava/lang/String;)V"
    );

    // Run long calculation in native thread
    std::thread worker([handlerGlobal, callback]() {

        for (int i = 1; i <= 5; i++) {
            notifyJava(handlerGlobal, callback,
                       i * 20,
                       "Calculation progress: step " + std::to_string(i));

            std::this_thread::sleep_for(std::chrono::seconds(1));
        }

        notifyJava(handlerGlobal, callback,
                   100,
                   "Calculation completed");

        // Clean up global reference
        JNIEnv* env = nullptr;
        g_vm->AttachCurrentThread((void**)&env, nullptr);
        env->DeleteGlobalRef(handlerGlobal);
        g_vm->DetachCurrentThread();
    });

    worker.detach();
}
