// ObjectInspector.c
#include <jni.h>
#include <stdio.h>
#include <string.h>
#include "ObjectInspector.h"

JNIEXPORT jstring JNICALL
Java_ObjectInspector_inspectObject(JNIEnv *env, jobject thisObj, jobject obj) {

    jclass objClass = (*env)->GetObjectClass(env, obj);

    // Get Class.getName()
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    jmethodID getNameMethod =
            (*env)->GetMethodID(env, classClass, "getName", "()Ljava/lang/String;");

    jstring className =
            (jstring)(*env)->CallObjectMethod(env, objClass, getNameMethod);

    const char *classNameChars =
            (*env)->GetStringUTFChars(env, className, NULL);

    char buffer[1024];
    snprintf(buffer, sizeof(buffer),
             "Class Name: %s\n", classNameChars);

    (*env)->ReleaseStringUTFChars(env, className, classNameChars);

    // Inspect fields
    jclass reflectClass = (*env)->FindClass(env, "java/lang/Class");
    jmethodID getFieldsMethod =
            (*env)->GetMethodID(env, reflectClass,
                                "getDeclaredFields",
                                "()[Ljava/lang/reflect/Field;");

    jobjectArray fields =
            (jobjectArray)(*env)->CallObjectMethod(env, objClass, getFieldsMethod);

    jsize fieldCount = (*env)->GetArrayLength(env, fields);

    strcat(buffer, "Fields:\n");

    for (jsize i = 0; i < fieldCount; i++) {
        jobject fieldObj = (*env)->GetObjectArrayElement(env, fields, i);

        jclass fieldClass = (*env)->FindClass(env, "java/lang/reflect/Field");
        jmethodID getName =
                (*env)->GetMethodID(env, fieldClass,
                                    "getName",
                                    "()Ljava/lang/String;");

        jstring fieldName =
                (jstring)(*env)->CallObjectMethod(env, fieldObj, getName);

        const char *fieldNameChars =
                (*env)->GetStringUTFChars(env, fieldName, NULL);

        strcat(buffer, "  - ");
        strcat(buffer, fieldNameChars);
        strcat(buffer, "\n");

        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameChars);
    }

    // Inspect methods
    jmethodID getMethodsMethod =
            (*env)->GetMethodID(env, reflectClass,
                                "getDeclaredMethods",
                                "()[Ljava/lang/reflect/Method;");

    jobjectArray methods =
            (jobjectArray)(*env)->CallObjectMethod(env, objClass, getMethodsMethod);

    jsize methodCount = (*env)->GetArrayLength(env, methods);

    strcat(buffer, "Methods:\n");

    for (jsize i = 0; i < methodCount; i++) {
        jobject methodObj = (*env)->GetObjectArrayElement(env, methods, i);

        jclass methodClass = (*env)->FindClass(env, "java/lang/reflect/Method");
        jmethodID getMethodName =
                (*env)->GetMethodID(env, methodClass,
                                    "getName",
                                    "()Ljava/lang/String;");

        jstring methodName =
                (jstring)(*env)->CallObjectMethod(env, methodObj, getMethodName);

        const char *methodNameChars =
                (*env)->GetStringUTFChars(env, methodName, NULL);

        strcat(buffer, "  - ");
        strcat(buffer, methodNameChars);
        strcat(buffer, "\n");

        (*env)->ReleaseStringUTFChars(env, methodName, methodNameChars);
    }

    return (*env)->NewStringUTF(env, buffer);
}
