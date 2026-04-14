// validation_engine.cpp
#include <jni.h>
#include <iostream>
#include <string>

// Validation rules constants
const int MAX_LOGIN_ATTEMPTS = 7;
const std::string VALID_STATUS = "ACTIVE";

JNIEXPORT jboolean JNICALL Java_UserBatchValidator_validateUser(JNIEnv *env, jobject validatorObj, jobject userObj) {
    if (userObj == nullptr) {
        return JNI_FALSE;
    }
    
    // Get the User class
    jclass userClass = env->GetObjectClass(userObj);
    
    // Get field IDs for all three fields
    jfieldID userIdFieldID = env->GetFieldID(userClass, "userId", "I");
    jfieldID statusFieldID = env->GetFieldID(userClass, "status", "Ljava/lang/String;");
    jfieldID loginAttemptsFieldID = env->GetFieldID(userClass, "loginAttempts", "I");
    
    // Check if any field ID is null (error occurred)
    if (userIdFieldID == nullptr || statusFieldID == nullptr || loginAttemptsFieldID == nullptr) {
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exceptionClass, "Could not find required fields in User class");
        return JNI_FALSE;
    }
    
    // Read userId
    jint userId = env->GetIntField(userObj, userIdFieldID);
    
    // Read status
    jstring statusObj = (jstring)env->GetObjectField(userObj, statusFieldID);
    const char* statusStr = nullptr;
    std::string statusCpp;
    
    if (statusObj != nullptr) {
        statusStr = env->GetStringUTFChars(statusObj, nullptr);
        if (statusStr != nullptr) {
            statusCpp = std::string(statusStr);
            env->ReleaseStringUTFChars(statusObj, statusStr);
        }
    }
    
    // Read loginAttempts
    jint loginAttempts = env->GetIntField(userObj, loginAttemptsFieldID);
    
    // Apply validation logic
    bool isValid = true;
    
    // Rule 1: User must have valid status (ACTIVE)
    if (statusCpp != VALID_STATUS) {
        isValid = false;
    }
    
    // Rule 2: Login attempts should not exceed maximum
    if (loginAttempts > MAX_LOGIN_ATTEMPTS) {
        isValid = false;
    }
    
    // Rule 3: User ID should be positive
    if (userId <= 0) {
        isValid = false;
    }
    
    // Additional validation: If user has too many failed login attempts, they're invalid regardless of status
    if (loginAttempts >= 5 && statusCpp == "SUSPENDED") {
        isValid = false;
    }
    
    return isValid ? JNI_TRUE : JNI_FALSE;
}