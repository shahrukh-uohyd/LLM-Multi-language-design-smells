// NativeAccessor.c
#include <jni.h>
#include <stdio.h>
#include "NativeAccessor.h"

JNIEXPORT jint JNICALL
Java_NativeAccessor_getAgeFromNative(JNIEnv *env, jobject thisObj, jobject personObj) {

    // Get the Person class
    jclass personClass = (*env)->GetObjectClass(env, personObj);

    // Get the field ID for "age"
    jfieldID ageField = (*env)->GetFieldID(env, personClass, "age", "I");

    if (ageField == NULL) {
        // Field not found
        return -1;
    }

    // Retrieve the value of the "age" property
    jint age = (*env)->GetIntField(env, personObj, ageField);

    // Return the value to Java
    return age;
}
