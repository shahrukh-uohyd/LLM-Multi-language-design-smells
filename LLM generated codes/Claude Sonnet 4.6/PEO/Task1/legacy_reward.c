#include <jni.h>
#include "UserAccount.h"  // generated via: javac -h . UserAccount.java

/* -----------------------------------------------------------------------
 * APPROACH A (Less Preferred):
 * Receives the full UserAccount object but accesses ONLY yearsOfService.
 * Risk: native code has a reference to the whole object — discipline required.
 * ----------------------------------------------------------------------- */
JNIEXPORT jboolean JNICALL
Java_UserAccount_isEligibleForLegacyReward(JNIEnv *env, jobject thisObj) {

    /* Step 1: Get the class reference */
    jclass userAccountClass = (*env)->GetObjectClass(env, thisObj);
    if (userAccountClass == NULL) {
        return JNI_FALSE; /* Class not found — fail safely */
    }

    /* Step 2: Get the field ID for 'yearsOfService' (type signature "I" = int) */
    jfieldID yearsOfServiceFieldID = (*env)->GetFieldID(
        env,
        userAccountClass,
        "yearsOfService",
        "I"           /* JNI type descriptor for 'int' */
    );
    if (yearsOfServiceFieldID == NULL) {
        return JNI_FALSE; /* Field not found — fail safely */
    }

    /* Step 3: Read ONLY the yearsOfService field — do NOT touch any other field */
    jint yearsOfService = (*env)->GetIntField(env, thisObj, yearsOfServiceFieldID);

    /* Step 4: Apply the Legacy Reward eligibility logic */
    return (yearsOfService > 10) ? JNI_TRUE : JNI_FALSE;
}


/* -----------------------------------------------------------------------
 * APPROACH B (RECOMMENDED — Principle of Least Privilege):
 * Java passes only the int it needs. Native code never sees the object.
 * This completely eliminates the risk of accidentally accessing sensitive fields.
 * ----------------------------------------------------------------------- */
JNIEXPORT jboolean JNICALL
Java_UserAccount_isEligibleForLegacyRewardSecure(JNIEnv *env, jobject thisObj, jint yearsOfService) {

    /* No object field access needed at all.
     * 'thisObj' is the Java 'this' reference — we deliberately ignore it.
     * The logic is simple, clear, and has no access to sensitive data. */

    return (yearsOfService > 10) ? JNI_TRUE : JNI_FALSE;
}