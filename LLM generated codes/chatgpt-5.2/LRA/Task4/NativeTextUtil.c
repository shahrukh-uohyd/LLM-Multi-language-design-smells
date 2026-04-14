#include <jni.h>
#include <ctype.h>
#include <string.h>
#include <stdlib.h>

JNIEXPORT jobjectArray JNICALL
Java_com_example_text_NativeTextUtil_toUpperCaseBatch(
        JNIEnv* env,
        jclass,
        jobjectArray inputArray) {

    if (inputArray == NULL) {
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, inputArray);

    // Find String class
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass == NULL) {
        return NULL;
    }

    // Create output String[]
    jobjectArray result =
        (*env)->NewObjectArray(env, length, stringClass, NULL);
    if (result == NULL) {
        return NULL;
    }

    for (jsize i = 0; i < length; i++) {

        jstring inputStr =
            (jstring)(*env)->GetObjectArrayElement(env, inputArray, i);

        if (inputStr == NULL) {
            // Preserve nulls
            (*env)->SetObjectArrayElement(env, result, i, NULL);
            continue;
        }

        // Convert Java String → UTF-8 C string
        const char* utfChars =
            (*env)->GetStringUTFChars(env, inputStr, NULL);
        if (utfChars == NULL) {
            (*env)->DeleteLocalRef(env, inputStr);
            continue;
        }

        size_t len = strlen(utfChars);
        char* upper = (char*)malloc(len + 1);
        if (upper == NULL) {
            (*env)->ReleaseStringUTFChars(env, inputStr, utfChars);
            (*env)->DeleteLocalRef(env, inputStr);
            continue;
        }

        // Convert to uppercase
        for (size_t j = 0; j < len; j++) {
            upper[j] = (char)toupper((unsigned char)utfChars[j]);
        }
        upper[len] = '\0';

        // Create new Java String
        jstring outputStr =
            (*env)->NewStringUTF(env, upper);

        // Store in result array
        (*env)->SetObjectArrayElement(env, result, i, outputStr);

        // Cleanup
        free(upper);
        (*env)->ReleaseStringUTFChars(env, inputStr, utfChars);
        (*env)->DeleteLocalRef(env, inputStr);
        (*env)->DeleteLocalRef(env, outputStr);
    }

    return result;
}
