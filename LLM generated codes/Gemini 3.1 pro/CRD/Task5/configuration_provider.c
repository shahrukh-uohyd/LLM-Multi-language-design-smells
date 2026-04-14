#include <jni.h>
#include <stdio.h>
#include <string.h>

/*
 * Note: No 'extern "C"' block is needed here because this is compiled as pure C.
 * C compilers do not mangle function names the way C++ compilers do.
 */

JNIEXPORT jstring JNICALL Java_ConfigurationProvider_fetchConfigValue(JNIEnv *env, jobject thisObj, jstring key) {
    // 1. Convert the Java String to a C-style string
    const char *key_str = (*env)->GetStringUTFChars(env, key, NULL);
    
    if (key_str == NULL) {
        return NULL; // OutOfMemoryError already thrown by JVM
    }

    /* 
     * ========================================================
     * [Perform ACTUAL configuration read logic here]
     * e.g., Read from Windows Registry or Linux config files.
     * ========================================================
     */
    printf("Native: Fetching configuration for key '%s'\n", key_str);
    
    // Mocking the retrieved native value
    const char *mock_retrieved_value = "prod-db.internal.network:5432";

    // 2. Release the C-string back to the JVM to prevent memory leaks
    (*env)->ReleaseStringUTFChars(env, key, key_str);

    // 3. Create a new Java String from the C-style string and return it
    return (*env)->NewStringUTF(env, mock_retrieved_value);
}

JNIEXPORT jboolean JNICALL Java_ConfigurationProvider_updateConfigValue(JNIEnv *env, jobject thisObj, jstring key, jstring value) {
    // 1. Convert both Java Strings to C-style strings
    const char *key_str = (*env)->GetStringUTFChars(env, key, NULL);
    const char *value_str = (*env)->GetStringUTFChars(env, value, NULL);
    
    if (key_str == NULL || value_str == NULL) {
        if (key_str != NULL) (*env)->ReleaseStringUTFChars(env, key, key_str);
        if (value_str != NULL) (*env)->ReleaseStringUTFChars(env, value, value_str);
        return JNI_FALSE;
    }

    /* 
     * ========================================================
     * [Perform ACTUAL configuration write logic here]
     * ========================================================
     */
    printf("Native: Updating configuration key '%s' to value '%s'\n", key_str, value_str);

    // 2. Always release strings after using them
    (*env)->ReleaseStringUTFChars(env, key, key_str);
    (*env)->ReleaseStringUTFChars(env, value, value_str);

    // 3. Return JNI_TRUE (1) to indicate success
    return JNI_TRUE;
}