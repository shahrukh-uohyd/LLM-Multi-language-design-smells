#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

// Helper function to print JNI errors (optional but helpful)
void checkException(JNIEnv *env) {
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
}

JNIEXPORT jstring JNICALL Java_PropertyAccess_getNameFromNative
(JNIEnv *env, jobject thisObj, jobject obj) {
    // Get the class of the passed object
    jclass cls = (*env)->GetObjectClass(env, obj);
    
    // Get the field ID for the 'name' field
    jfieldID fid = (*env)->GetFieldID(env, cls, "name", "Ljava/lang/String;");
    
    // Check if field exists
    if (fid == NULL) {
        checkException(env);
        return NULL;
    }
    
    // Get the value of the field
    jstring value = (jstring)(*env)->GetObjectField(env, obj, fid);
    
    return value;
}

JNIEXPORT jint JNICALL Java_PropertyAccess_getAgeFromNative
(JNIEnv *env, jobject thisObj, jobject obj) {
    // Get the class of the passed object
    jclass cls = (*env)->GetObjectClass(env, obj);
    
    // Get the field ID for the 'age' field
    jfieldID fid = (*env)->GetFieldID(env, cls, "age", "I");
    
    // Check if field exists
    if (fid == NULL) {
        checkException(env);
        return -1;
    }
    
    // Get the value of the field
    jint value = (*env)->GetIntField(env, obj, fid);
    
    return value;
}

JNIEXPORT jdouble JNICALL Java_PropertyAccess_getSalaryFromNative
(JNIEnv *env, jobject thisObj, jobject obj) {
    // Get the class of the passed object
    jclass cls = (*env)->GetObjectClass(env, obj);
    
    // Get the field ID for the 'salary' field
    jfieldID fid = (*env)->GetFieldID(env, cls, "salary", "D");
    
    // Check if field exists
    if (fid == NULL) {
        checkException(env);
        return -1.0;
    }
    
    // Get the value of the field
    jdouble value = (*env)->GetDoubleField(env, obj, fid);
    
    return value;
}

JNIEXPORT jboolean JNICALL Java_PropertyAccess_getEmployedFromNative
(JNIEnv *env, jobject thisObj, jobject obj) {
    // Get the class of the passed object
    jclass cls = (*env)->GetObjectClass(env, obj);
    
    // Get the field ID for the 'employed' field
    jfieldID fid = (*env)->GetFieldID(env, cls, "employed", "Z");
    
    // Check if field exists
    if (fid == NULL) {
        checkException(env);
        return JNI_FALSE;
    }
    
    // Get the value of the field
    jboolean value = (*env)->GetBooleanField(env, obj, fid);
    
    return value;
}