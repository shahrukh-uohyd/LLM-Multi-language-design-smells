#include <jni.h>
#include <iostream>

extern "C"
JNIEXPORT void JNICALL Java_HardwareConnection_resetConnectionStatus(JNIEnv *env, jobject thiz) {
    
    std::cout << "[C++] Hardware timeout detected. Resetting connection status..." << std::endl;

    // 1. Get the class of the Java object that called this native method
    jclass connectionClass = env->GetObjectClass(thiz);
    if (connectionClass == nullptr) {
        std::cerr << "[C++] Error: Could not find the Java class." << std::endl;
        return;
    }

    // 2. Find the Field ID for the boolean flag
    // The name of the field in Java is "isActive"
    // The JNI signature for a Java boolean is "Z"
    jfieldID isActiveField = env->GetFieldID(connectionClass, "isActive", "Z");
    
    // Check if the field was found (JNI will throw a NoSuchFieldError in Java if not)
    if (isActiveField == nullptr) {
        std::cerr << "[C++] Error: Field 'isActive' not found in Java object." << std::endl;
        env->DeleteLocalRef(connectionClass);
        return;
    }

    // 3. Update the value of the field on the Java object
    // Set 'isActive' to false (JNI_FALSE)
    env->SetBooleanField(thiz, isActiveField, JNI_FALSE);

    std::cout << "[C++] Connection status successfully reset to inactive (false)." << std::endl;

    // 4. Clean up the local reference to avoid memory leaks
    env->DeleteLocalRef(connectionClass);
}