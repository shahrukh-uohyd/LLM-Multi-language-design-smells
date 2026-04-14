#include <jni.h>
#include "UserAccount.h"
#include <stdio.h>

JNIEXPORT jboolean JNICALL Java_UserAccount_isEligibleForLegacyReward
  (JNIEnv *env, jobject thisObj) {
    
    jclass userAccountClass = NULL;
    jfieldID yearsOfServiceFieldID = NULL;
    jint yearsOfService = 0;
    jboolean isEligible = JNI_FALSE;
    
    // Validate input parameters
    if (env == NULL || thisObj == NULL) {
        return JNI_FALSE;
    }
    
    // Get the UserAccount class
    userAccountClass = (*env)->GetObjectClass(env, thisObj);
    if (userAccountClass == NULL) {
        // Exception already thrown by GetObjectClass
        return JNI_FALSE;
    }
    
    // Find the field ID for yearsOfService
    yearsOfServiceFieldID = (*env)->GetFieldID(env, userAccountClass, 
                                              "yearsOfService", "I");
    
    if (yearsOfServiceFieldID == NULL) {
        // Field not found - GetFieldID will have thrown an exception
        (*env)->ExceptionDescribe(env);
        return JNI_FALSE;
    }
    
    // Get the value of yearsOfService field
    yearsOfService = (*env)->GetIntField(env, thisObj, yearsOfServiceFieldID);
    
    // Check for potential exception during GetIntField
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return JNI_FALSE;
    }
    
    // Determine eligibility: user is eligible if yearsOfService > 10
    isEligible = (yearsOfService > 10) ? JNI_TRUE : JNI_FALSE;
    
    return isEligible;
}