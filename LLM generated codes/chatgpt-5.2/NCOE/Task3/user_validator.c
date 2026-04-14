#include <jni.h>

/*
 * Class:     com_example_batch_UserBatchValidator
 * Method:    validateUser
 * Signature: (Lcom/example/batch/User;)Z
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_batch_UserBatchValidator_validateUser(
        JNIEnv* env,
        jclass,
        jobject userObj
) {
    if (userObj == nullptr) {
        return JNI_FALSE;
    }

    jclass userClass = env->GetObjectClass(userObj);
    if (userClass == nullptr) {
        return JNI_FALSE;
    }

    // Field IDs
    jfieldID userIdField = env->GetFieldID(userClass, "userId", "I");
    jfieldID statusField = env->GetFieldID(
        userClass, "status", "Ljava/lang/String;"
    );
    jfieldID attemptsField = env->GetFieldID(userClass, "loginAttempts", "I");

    if (!userIdField || !statusField || !attemptsField) {
        env->DeleteLocalRef(userClass);
        return JNI_FALSE;
    }

    // Read fields
    jint userId = env->GetIntField(userObj, userIdField);
    jint loginAttempts = env->GetIntField(userObj, attemptsField);
    jstring statusStr =
        (jstring)env->GetObjectField(userObj, statusField);

    // Simple validation rule:
    // userId must be positive and loginAttempts <= 3
    bool valid = (userId > 0 && loginAttempts <= 3);

    // Update status field based on validation result
    const char* newStatus = valid ? "ACTIVE" : "LOCKED";
    jstring newStatusStr = env->NewStringUTF(newStatus);

    if (newStatusStr != nullptr) {
        env->SetObjectField(userObj, statusField, newStatusStr);
        env->DeleteLocalRef(newStatusStr);
    }

    if (statusStr != nullptr) {
        env->DeleteLocalRef(statusStr);
    }

    env->DeleteLocalRef(userClass);
    return valid ? JNI_TRUE : JNI_FALSE;
}
