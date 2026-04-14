#include <jni.h>

extern "C" JNIEXPORT jobject JNICALL Java_NativeObjectCreator_createDataContainer
(JNIEnv* env, jobject obj, jfloat value) {
    
    // Find the DataContainer class
    jclass dataContainerClass = env->FindClass("DataContainer");
    if (dataContainerClass == nullptr) {
        // Handle error: class not found
        return nullptr;
    }
    
    // Find the constructor that takes a float parameter
    jmethodID constructor = env->GetMethodID(dataContainerClass, "<init>", "(F)V");
    if (constructor == nullptr) {
        // Handle error: constructor not found
        env->ExceptionDescribe(); // Print any pending exception
        env->ExceptionClear();   // Clear the exception
        env->DeleteLocalRef(dataContainerClass);
        return nullptr;
    }
    
    // Create a new instance of DataContainer using the constructor
    jobject newDataContainer = env->NewObject(dataContainerClass, constructor, value);
    
    // Clean up local reference to the class
    env->DeleteLocalRef(dataContainerClass);
    
    // Return the new object to Java
    return newDataContainer;
}