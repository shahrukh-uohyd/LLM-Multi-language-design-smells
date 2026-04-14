#include <jni.h>
#include <iostream>

extern "C" {

JNIEXPORT jstring JNICALL Java_GetThenSetExample_getFieldAndInvokeMethod
(JNIEnv *env, jobject thisObj, jobject obj) {
    // Step 1: Retrieve the value of the 'count' field
    jclass cls = env->GetObjectClass(obj);
    
    // Check if GetObjectClass succeeded
    if (cls == nullptr) {
        // Exception already pending from GetObjectClass
        return nullptr;
    }
    
    // Get the field ID for the 'count' field
    jfieldID countFieldID = env->GetFieldID(cls, "count", "I");
    
    // Check if GetFieldID succeeded
    if (countFieldID == nullptr) {
        // Exception already pending from GetFieldID
        return nullptr;
    }
    
    // Get the value of the 'count' field
    jint countValue = env->GetIntField(obj, countFieldID);
    
    // Check if GetIntField succeeded (it throws exceptions rather than returning error codes)
    if (env->ExceptionCheck()) {
        // An exception occurred during GetIntField
        return nullptr;
    }
    
    // Step 2: Prepare to call the method with the retrieved value
    // Get the method ID for the 'processWithCount' method
    jmethodID methodID = env->GetMethodID(cls, "processWithCount", "(Ljava/lang/String;)Ljava/lang/String;");
    
    // Check if GetMethodID succeeded
    if (methodID == nullptr) {
        // Exception already pending from GetMethodID
        return nullptr;
    }
    
    // Create the prefix string to pass as an argument
    std::string prefix = "Retrieved count was: " + std::to_string(countValue);
    jstring prefixStr = env->NewStringUTF(prefix.c_str());
    
    // Check if NewStringUTF succeeded
    if (prefixStr == nullptr) {
        // Exception already pending from NewStringUTF
        return nullptr;
    }
    
    // Step 3: Invoke the method with the retrieved field value as part of the argument
    jstring result = (jstring)env->CallObjectMethod(obj, methodID, prefixStr);
    
    // Clean up local references
    env->DeleteLocalRef(prefixStr);
    env->DeleteLocalRef(cls);
    
    // Check if the method call threw an exception
    if (env->ExceptionCheck()) {
        // Clear the exception and return null
        env->ExceptionClear();
        return nullptr;
    }
    
    return result;
}

} // extern "C"