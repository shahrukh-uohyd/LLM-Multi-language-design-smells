#include <jni.h>
#include <iostream>
#include <cstring>

extern "C" {

JNIEXPORT jint JNICALL Java_com_example_FieldAccessor_retrieveIntegerField
(JNIEnv *env, jobject instance, jobject obj, jstring fieldName) {
    // Check if the passed object is null
    if (obj == nullptr) {
        // Log error but don't throw exception, return a sentinel value
        return -1; // Using -1 as error indicator since field could be 0
    }
    
    // Check if the field name is null
    if (fieldName == nullptr) {
        return -1;
    }
    
    // Get the class of the object
    jclass objClass = env->GetObjectClass(obj);
    if (objClass == nullptr) {
        // Clear any pending exception and return error indicator
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return -1;
    }
    
    // Get the field name as C string
    const char* fieldNameStr = env->GetStringUTFChars(fieldName, nullptr);
    if (fieldNameStr == nullptr) {
        // Out of memory error
        return -1;
    }
    
    // Locate the field - this might fail if field doesn't exist
    jfieldID fieldID = env->GetFieldID(objClass, fieldNameStr, "I"); // "I" for integer type
    
    // Release the string since we're done with it
    env->ReleaseStringUTFChars(fieldName, fieldNameStr);
    
    // CRITICAL VALIDATION: Check if jfieldID is valid before using it
    if (fieldID == nullptr) {
        // Field not found or wrong type, clear any pending exception
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return -1; // Indicate error
    }
    
    // Now safely retrieve the integer value using the validated field ID
    jint value = env->GetIntField(obj, fieldID);
    
    // Check for exceptions that might have occurred during field access
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe(); // Print exception info
        env->ExceptionClear();    // Clear the exception
        return -1; // Indicate error
    }
    
    return value;
}

JNIEXPORT jstring JNICALL Java_com_example_FieldAccessor_retrieveMultipleFields
(JNIEnv *env, jobject instance, jobject obj) {
    if (obj == nullptr) {
        return env->NewStringUTF("Error: Object is null");
    }
    
    jclass objClass = env->GetObjectClass(obj);
    if (objClass == nullptr) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return env->NewStringUTF("Error: Could not get object class");
    }
    
    // Validate and retrieve multiple fields safely
    jfieldID dataValueFieldID = env->GetFieldID(objClass, "dataValue", "I");
    if (dataValueFieldID == nullptr) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return env->NewStringUTF("Error: Could not find dataValue field");
    }
    
    jfieldID isValidFieldID = env->GetFieldID(objClass, "isValid", "Z"); // Z for boolean
    if (isValidFieldID == nullptr) {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return env->NewStringUTF("Error: Could not find isValid field");
    }
    
    // Safe to access fields now that IDs are validated
    jint dataValue = env->GetIntField(obj, dataValueFieldID);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return env->NewStringUTF("Error: Exception occurred while accessing dataValue field");
    }
    
    jboolean isValid = env->GetBooleanField(obj, isValidFieldID);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return env->NewStringUTF("Error: Exception occurred while accessing isValid field");
    }
    
    // Build result string
    char result[512];
    snprintf(result, sizeof(result), 
             "Successfully retrieved fields - dataValue: %d, isValid: %s", 
             dataValue, isValid ? "true" : "false");
    
    return env->NewStringUTF(result);
}

} // extern "C"