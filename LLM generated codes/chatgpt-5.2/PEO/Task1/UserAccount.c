#include <jni.h>
#include "UserAccount.h"

/*
 * boolean isEligibleForLegacyReward()
 */
JNIEXPORT jboolean JNICALL
Java_UserAccount_isEligibleForLegacyReward(JNIEnv *env,
                                           jobject obj) {

    // Get the UserAccount class
    jclass cls = (*env)->GetObjectClass(env, obj);
    if (cls == NULL) {
        return JNI_FALSE;
    }

    // Locate the field: int yearsOfService
    jfieldID yearsField = (*env)->GetFieldID(
        env,
        cls,
        "yearsOfService",
        "I"
    );

    if (yearsField == NULL) {
        (*env)->ExceptionClear(env);
        return JNI_FALSE;
    }

    // Read the value
    jint years = (*env)->GetIntField(env, obj, yearsField);

    // Eligibility logic
    return (years > 10) ? JNI_TRUE : JNI_FALSE;
}
