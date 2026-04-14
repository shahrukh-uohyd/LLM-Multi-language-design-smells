// File: metadata_inspector.c
#include <jni.h>
#include <stdio.h>
#include <string.h>

JNIEXPORT jstring JNICALL
Java_MetadataInspector_getClassMetadata(JNIEnv *env, jobject obj, jstring className) {

    const char *classNameUtf = (*env)->GetStringUTFChars(env, className, NULL);

    // Convert "TargetClass" → "TargetClass"
    jclass targetClass = (*env)->FindClass(env, classNameUtf);
    if (targetClass == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameUtf);
        return (*env)->NewStringUTF(env, "Class not found");
    }

    jclass classClass = (*env)->FindClass(env, "java/lang/Class");

    // getName()
    jmethodID getName = (*env)->GetMethodID(env, classClass, "getName", "()Ljava/lang/String;");
    jstring name = (jstring)(*env)->CallObjectMethod(env, targetClass, getName);

    // getDeclaredFields()
    jmethodID getFields = (*env)->GetMethodID(env, classClass,
        "getDeclaredFields", "()[Ljava/lang/reflect/Field;");
    jobjectArray fields = (jobjectArray)(*env)->CallObjectMethod(env, targetClass, getFields);

    // getDeclaredMethods()
    jmethodID getMethods = (*env)->GetMethodID(env, classClass,
        "getDeclaredMethods", "()[Ljava/lang/reflect/Method;");
    jobjectArray methods = (jobjectArray)(*env)->CallObjectMethod(env, targetClass, getMethods);

    char buffer[2048];
    strcpy(buffer, "Class Name: ");

    const char *classNameResult = (*env)->GetStringUTFChars(env, name, NULL);
    strcat(buffer, classNameResult);
    strcat(buffer, "\nFields:\n");

    jsize fieldCount = (*env)->GetArrayLength(env, fields);
    for (jsize i = 0; i < fieldCount; i++) {
        jobject field = (*env)->GetObjectArrayElement(env, fields, i);
        jclass fieldClass = (*env)->FindClass(env, "java/lang/reflect/Field");
        jmethodID getFieldName = (*env)->GetMethodID(env, fieldClass,
            "getName", "()Ljava/lang/String;");
        jstring fieldName = (jstring)(*env)->CallObjectMethod(env, field, getFieldName);
        const char *fieldNameUtf = (*env)->GetStringUTFChars(env, fieldName, NULL);

        strcat(buffer, "  ");
        strcat(buffer, fieldNameUtf);
        strcat(buffer, "\n");

        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameUtf);
    }

    strcat(buffer, "Methods:\n");

    jsize methodCount = (*env)->GetArrayLength(env, methods);
    for (jsize i = 0; i < methodCount; i++) {
        jobject method = (*env)->GetObjectArrayElement(env, methods, i);
        jclass methodClass = (*env)->FindClass(env, "java/lang/reflect/Method");
        jmethodID getMethodName = (*env)->GetMethodID(env, methodClass,
            "getName", "()Ljava/lang/String;");
        jstring methodName = (jstring)(*env)->CallObjectMethod(env, method, getMethodName);
        const char *methodNameUtf = (*env)->GetStringUTFChars(env, methodName, NULL);

        strcat(buffer, "  ");
        strcat(buffer, methodNameUtf);
        strcat(buffer, "\n");

        (*env)->ReleaseStringUTFChars(env, methodName, methodNameUtf);
    }

    (*env)->ReleaseStringUTFChars(env, className, classNameUtf);
    (*env)->ReleaseStringUTFChars(env, name, classNameResult);

    return (*env)->NewStringUTF(env, buffer);
}
