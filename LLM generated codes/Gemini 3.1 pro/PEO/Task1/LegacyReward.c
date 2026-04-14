#include <jni.h>
#include <stddef.h>

// Note: Replace 'com_example_RewardService' with your actual Java package and class name where the native method is declared.
JNIEXPORT jboolean JNICALL Java_com_example_RewardService_isEligibleForLegacyReward(JNIEnv *env, jobject thisObj, jobject userAccountObj) {
    
    // Safety check for null object
    if (userAccountObj == NULL) {
        return JNI_FALSE;
    }

    // 1. Get the class of the UserAccount object passed from Java
    jclass userAccountClass = (*env)->GetObjectClass(env, userAccountObj);
    if (userAccountClass == NULL) {
        return JNI_FALSE; // Class not found
    }

    // 2. Get the Field ID for the 'yearsOfService' variable.
    // The "I" represents the JNI signature for a Java primitive 'int'.
    jfieldID yearsOfServiceField = (*env)->GetFieldID(env, userAccountClass, "yearsOfService", "I");
    if (yearsOfServiceField == NULL) {
        // Field not found, JNI will automatically throw a NoSuchFieldError in Java
        return JNI_FALSE; 
    }

    // 3. Extract the actual integer value from the object
    jint yearsOfService = (*env)->GetIntField(env, userAccountObj, yearsOfServiceField);

    // 4. Apply the business logic: strictly greater than 10 years
    if (yearsOfService > 10) {
        return JNI_TRUE;
    }

    return JNI_FALSE;
}