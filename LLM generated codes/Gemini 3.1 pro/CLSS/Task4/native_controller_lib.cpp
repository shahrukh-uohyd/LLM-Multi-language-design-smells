#include <jni.h>
#include <iostream>

extern "C"
JNIEXPORT void JNICALL
Java_com_example_NativeModule_initializeAndRun(JNIEnv *env, jobject thiz) {
    
    // 1. Locate the Java component class
    jclass controllerClass = env->FindClass("com/example/SystemController");
    if (controllerClass == nullptr) {
        std::cerr << "Failed to find SystemController class." << std::endl;
        return;
    }

    // 2. Find the static getInstance() method
    // Signature: "()Lcom/example/SystemController;" 
    // Takes no arguments "()", returns a SystemController object "L...;"
    jmethodID getInstanceMethod = env->GetStaticMethodID(
        controllerClass, 
        "getInstance", 
        "()Lcom/example/SystemController;"
    );

    if (getInstanceMethod == nullptr) {
        std::cerr << "Failed to find getInstance method." << std::endl;
        env->DeleteLocalRef(controllerClass);
        return;
    }

    // 3. Invoke getInstance() to get the actual Java object reference
    jobject controllerInstance = env->CallStaticObjectMethod(controllerClass, getInstanceMethod);
    if (controllerInstance == nullptr) {
        std::cerr << "Failed to get SystemController instance." << std::endl;
        env->DeleteLocalRef(controllerClass);
        return;
    }

    // 4. Find the instance method we want to trigger
    // Signature: "(Ljava/lang/String;I)V"
    jmethodID executeCommandMethod = env->GetMethodID(
        controllerClass, 
        "executeCommand", 
        "(Ljava/lang/String;I)V"
    );

    if (executeCommandMethod == nullptr) {
        std::cerr << "Failed to find executeCommand method." << std::endl;
        env->DeleteLocalRef(controllerInstance);
        env->DeleteLocalRef(controllerClass);
        return;
    }

    // 5. Prepare the arguments
    jstring commandStr = env->NewStringUTF("START_BACKGROUND_SYNC");
    jint priority = 1;

    // 6. Trigger the functionality on the located Java component
    env->CallVoidMethod(controllerInstance, executeCommandMethod, commandStr, priority);

    // 7. Clean up local references to prevent memory leaks
    env->DeleteLocalRef(commandStr);
    env->DeleteLocalRef(controllerInstance);
    env->DeleteLocalRef(controllerClass);
}