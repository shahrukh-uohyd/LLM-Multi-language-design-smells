#include <jni.h>
#include <iostream>

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_DataAnalyzer_calculateUserRecordSum(JNIEnv *env, jobject thiz, jobjectArray recordsArray) {
    
    // 1. Check if the input array is null
    if (recordsArray == nullptr) {
        return 0;
    }

    // 2. Get the length of the array
    jsize arrayLength = env->GetArrayLength(recordsArray);
    if (arrayLength == 0) {
        return 0;
    }

    // 3. Look up the Class and Field ID ONCE before the loop (Performance Optimization)
    // Replace "com/example/UserRecord" with your actual fully qualified class path
    jclass userRecordClass = env->FindClass("com/example/UserRecord");
    if (userRecordClass == nullptr) {
        // Class not found, JNI will throw NoClassDefFoundError automatically
        return 0; 
    }

    // Replace "score" with your actual field name, and "I" stands for integer primitive type
    jfieldID scoreFieldId = env->GetFieldID(userRecordClass, "score", "I");
    if (scoreFieldId == nullptr) {
        // Field not found, JNI will throw NoSuchFieldError automatically
        env->DeleteLocalRef(userRecordClass);
        return 0;
    }

    jint sum = 0;

    // 4. Iterate through the array
    for (jsize i = 0; i < arrayLength; ++i) {
        // Get the element at index i
        jobject recordObj = env->GetObjectArrayElement(recordsArray, i);
        
        if (recordObj != nullptr) {
            // Extract the integer field value from the object
            jint value = env->GetIntField(recordObj, scoreFieldId);
            sum += value;
            
            // 5. CRITICAL: Delete the local reference to avoid Local Reference Table Overflow
            // JNI only guarantees 16 local refs by default.
            env->DeleteLocalRef(recordObj);
        }
    }

    // Clean up the class reference
    env->DeleteLocalRef(userRecordClass);

    return sum;
}