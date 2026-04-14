#include <jni.h>
#include <string.h>
#include <iostream>

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_example_batch_UserBatchValidator_validateUser(
    JNIEnv *env, 
    jobject thiz,     // The UserBatchValidator instance
    jobject userObj)  // The User object passed in the loop
{
    if (userObj == nullptr) {
        return JNI_FALSE;
    }

    // 1. Get the class of the User object
    jclass userClass = env->GetObjectClass(userObj);
    if (userClass == nullptr) {
        return JNI_FALSE;
    }

    // 2. Look up Field IDs for all three fields
    // "Ljava/lang/String;" is the signature for String, "I" is the signature for int
    jfieldID userIdField = env->GetFieldID(userClass, "userId", "Ljava/lang/String;");
    jfieldID statusField = env->GetFieldID(userClass, "status", "Ljava/lang/String;");
    jfieldID attemptsField = env->GetFieldID(userClass, "loginAttempts", "I");

    if (userIdField == nullptr || statusField == nullptr || attemptsField == nullptr) {
        env->DeleteLocalRef(userClass);
        return JNI_FALSE; // A field wasn't found
    }

    // 3. Read the field values from the User object
    jstring jUserId = (jstring) env->GetObjectField(userObj, userIdField);
    jstring jStatus = (jstring) env->GetObjectField(userObj, statusField);
    jint loginAttempts = env->GetIntField(userObj, attemptsField);

    // Convert Java strings to C-style strings for comparison
    const char *statusCStr = jStatus != nullptr ? env->GetStringUTFChars(jStatus, nullptr) : "";

    bool isValid = true;

    // 4. Validation Logic
    // Example: User is invalid if not ACTIVE or if they have >= 3 login attempts
    if (strcmp(statusCStr, "ACTIVE") != 0 || loginAttempts >= 3) {
        isValid = false;

        // 5. Modification Logic
        // If the user failed due to login attempts, lock their account by modifying the status field
        if (loginAttempts >= 3) {
            jstring lockedStatus = env->NewStringUTF("LOCKED");
            
            // Overwrite the 'status' field in the Java object
            env->SetObjectField(userObj, statusField, lockedStatus);
            
            // Clean up the local reference for the new string
            env->DeleteLocalRef(lockedStatus);
        }
    }

    // 6. Memory Management: Release string chars and local references
    if (jStatus != nullptr) {
        env->ReleaseStringUTFChars(jStatus, statusCStr);
    }
    
    env->DeleteLocalRef(jUserId);
    env->DeleteLocalRef(jStatus);
    env->DeleteLocalRef(userClass);

    return isValid ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"