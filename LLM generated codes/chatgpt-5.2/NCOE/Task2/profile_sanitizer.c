#include <jni.h>
#include <string.h>

#define MAX_BIO_LENGTH 100

/*
 * Class:     com_example_security_ProfileSanitizerNative
 * Method:    sanitizeProfileNative
 * Signature: (Lcom/example/security/UserProfile;)V
 */
JNIEXPORT void JNICALL
Java_com_example_security_ProfileSanitizerNative_sanitizeProfileNative(
        JNIEnv *env,
        jclass /* clazz */,
        jobject profileObj
) {
    if (profileObj == NULL) {
        return;
    }

    jclass profileClass = (*env)->GetObjectClass(env, profileObj);
    if (profileClass == NULL) {
        return;
    }

    /* ---------- First lookup: read bio and check length ---------- */
    jfieldID bioFieldID = (*env)->GetFieldID(
        env,
        profileClass,
        "bio",
        "Ljava/lang/String;"
    );

    if (bioFieldID == NULL) {
        (*env)->DeleteLocalRef(env, profileClass);
        return;
    }

    jstring bioString = (jstring)(*env)->GetObjectField(
        env,
        profileObj,
        bioFieldID
    );

    if (bioString == NULL) {
        (*env)->DeleteLocalRef(env, profileClass);
        return;
    }

    jsize bioLength = (*env)->GetStringLength(env, bioString);

    if (bioLength <= MAX_BIO_LENGTH) {
        // Nothing to sanitize
        (*env)->DeleteLocalRef(env, bioString);
        (*env)->DeleteLocalRef(env, profileClass);
        return;
    }

    /* ---------- Second lookup: truncate and write back ---------- */

    // Look up the field again (explicitly, as requested)
    jfieldID bioFieldID2 = (*env)->GetFieldID(
        env,
        profileClass,
        "bio",
        "Ljava/lang/String;"
    );

    if (bioFieldID2 == NULL) {
        (*env)->DeleteLocalRef(env, bioString);
        (*env)->DeleteLocalRef(env, profileClass);
        return;
    }

    const jchar *chars = (*env)->GetStringChars(env, bioString, NULL);
    if (chars == NULL) {
        (*env)->DeleteLocalRef(env, bioString);
        (*env)->DeleteLocalRef(env, profileClass);
        return;
    }

    jstring truncatedBio = (*env)->NewString(
        env,
        chars,
        MAX_BIO_LENGTH
    );

    (*env)->ReleaseStringChars(env, bioString, chars);

    if (truncatedBio != NULL) {
        (*env)->SetObjectField(
            env,
            profileObj,
            bioFieldID2,
            truncatedBio
        );
        (*env)->DeleteLocalRef(env, truncatedBio);
    }

    (*env)->DeleteLocalRef(env, bioString);
    (*env)->DeleteLocalRef(env, profileClass);
}
