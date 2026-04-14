#include <jni.h>
#include <vector>
#include <string>

extern "C" {

JNIEXPORT jobjectArray JNICALL Java_LoopUpcallExample_processObjects
(JNIEnv *env, jobject thisObj, jobjectArray objects) {
    // Get the length of the input array
    jsize arrayLength = env->GetArrayLength(objects);
    
    // Create a string class reference for our result array
    jclass stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr) {
        // Exception already pending
        return nullptr;
    }
    
    // Create result array to hold the processed strings
    jobjectArray resultArray = env->NewObjectArray(arrayLength, stringClass, nullptr);
    if (resultArray == nullptr) {
        // Exception already pending
        return nullptr;
    }
    
    // Get the DataObject class and method ID
    jclass dataObjectClass = env->FindClass("DataObject");
    if (dataObjectClass == nullptr) {
        // Exception already pending, clean up and return
        return nullptr;
    }
    
    jmethodID processMethodID = env->GetMethodID(dataObjectClass, "process", "()Ljava/lang/String;");
    if (processMethodID == nullptr) {
        // Exception already pending, clean up and return
        return nullptr;
    }
    
    // Iterate through the array and call process() on each object
    for (jsize i = 0; i < arrayLength; i++) {
        // Check for pending exceptions before each iteration
        if (env->ExceptionCheck()) {
            // If there was already an exception, clean up and return
            env->ExceptionDescribe(); // Print the exception details
            return nullptr;
        }
        
        // Get the object at index i
        jobject obj = env->GetObjectArrayElement(objects, i);
        if (obj == nullptr) {
            // Handle null element - store null in result array
            env->SetObjectArrayElement(resultArray, i, nullptr);
            continue; // Continue to next iteration
        }
        
        // Call the process() method on the object
        jstring result = (jstring)env->CallObjectMethod(obj, processMethodID);
        
        // Check if an exception occurred during the method call
        if (env->ExceptionCheck()) {
            // Log the exception and clear it
            env->ExceptionDescribe();
            env->ExceptionClear();
            
            // Store an error message in the result array instead of crashing
            jstring errorMsg = env->NewStringUTF("Error processing object at index");
            if (errorMsg != nullptr) {
                env->SetObjectArrayElement(resultArray, i, errorMsg);
                env->DeleteLocalRef(errorMsg);
            }
            // Continue to next iteration despite the error
            continue;
        }
        
        // Set the result in our output array
        env->SetObjectArrayElement(resultArray, i, result);
        
        // Clean up local reference to avoid memory leaks
        env->DeleteLocalRef(obj);
        if (result != nullptr) {
            env->DeleteLocalRef(result);
        }
    }
    
    // Clean up local references
    env->DeleteLocalRef(stringClass);
    env->DeleteLocalRef(dataObjectClass);
    
    return resultArray;
}

} // extern "C"