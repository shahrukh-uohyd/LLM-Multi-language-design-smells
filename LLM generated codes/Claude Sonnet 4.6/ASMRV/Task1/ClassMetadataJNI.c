#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/*
 * Native implementation of:
 *   com.jni.metadata.ClassMetadataJNI.getClassMetadata(String className)
 *
 * Steps:
 *  1. Convert the Java String className to a C string.
 *  2. Use FindClass() to locate the class in the JVM.
 *  3. Use JNI reflection APIs to get:
 *       - Class name
 *       - Superclass name
 *       - Declared methods (name + signature)
 *       - Declared fields  (name + type descriptor)
 *  4. Build a result string and return it as a jstring.
 */
JNIEXPORT jstring JNICALL
Java_com_jni_metadata_ClassMetadataJNI_getClassMetadata(JNIEnv *env,
                                                         jobject thisObj,
                                                         jstring jClassName)
{
    /* ------------------------------------------------------------------ */
    /* 0. Convert Java String -> C string                                  */
    /* ------------------------------------------------------------------ */
    const char *classNameCStr = (*env)->GetStringUTFChars(env, jClassName, NULL);
    if (classNameCStr == NULL) {
        return (*env)->NewStringUTF(env, "ERROR: Could not convert class name string.");
    }

    /* ------------------------------------------------------------------ */
    /* 1. Find the target class                                            */
    /* ------------------------------------------------------------------ */
    jclass targetClass = (*env)->FindClass(env, classNameCStr);
    (*env)->ReleaseStringUTFChars(env, jClassName, classNameCStr);

    if (targetClass == NULL) {
        return (*env)->NewStringUTF(env, "ERROR: Class not found.");
    }

    /* ------------------------------------------------------------------ */
    /* 2. Get java.lang.Class methods we need via JNI                      */
    /* ------------------------------------------------------------------ */
    /* We'll use java.lang.Class reflection methods via JNI calls.         */
    /* Cast the jclass to a jobject (Class instance) for method calls.     */

    jclass classClass       = (*env)->FindClass(env, "java/lang/Class");
    jclass methodClass      = (*env)->FindClass(env, "java/lang/reflect/Method");
    jclass fieldClass       = (*env)->FindClass(env, "java/lang/reflect/Field");
    jclass classLoaderClass = (*env)->FindClass(env, "java/lang/ClassLoader");

    /* Class.getName() -> String */
    jmethodID getNameMethod       = (*env)->GetMethodID(env, classClass, "getName", "()Ljava/lang/String;");
    /* Class.getSuperclass() -> Class */
    jmethodID getSuperclassMethod = (*env)->GetMethodID(env, classClass, "getSuperclass", "()Ljava/lang/Class;");
    /* Class.getDeclaredMethods() -> Method[] */
    jmethodID getDeclMethodsMethod= (*env)->GetMethodID(env, classClass, "getDeclaredMethods", "()[Ljava/lang/reflect/Method;");
    /* Class.getDeclaredFields()  -> Field[] */
    jmethodID getDeclFieldsMethod = (*env)->GetMethodID(env, classClass, "getDeclaredFields",  "()[Ljava/lang/reflect/Field;");

    /* Method.getName() -> String */
    jmethodID methodGetName       = (*env)->GetMethodID(env, methodClass, "getName", "()Ljava/lang/String;");
    /* Method.toGenericString() -> String (includes return type + params) */
    jmethodID methodToGenStr      = (*env)->GetMethodID(env, methodClass, "toGenericString", "()Ljava/lang/String;");

    /* Field.getName() -> String */
    jmethodID fieldGetName        = (*env)->GetMethodID(env, fieldClass,  "getName", "()Ljava/lang/String;");
    /* Field.getType() -> Class */
    jmethodID fieldGetType        = (*env)->GetMethodID(env, fieldClass,  "getType", "()Ljava/lang/Class;");

    /* ------------------------------------------------------------------ */
    /* 3. Build the result string                                           */
    /* ------------------------------------------------------------------ */
    char result[8192];
    result[0] = '\0';

    /* -- Class name -- */
    jstring classNameStr = (jstring)(*env)->CallObjectMethod(env, targetClass, getNameMethod);
    const char *classNameVal = (*env)->GetStringUTFChars(env, classNameStr, NULL);
    snprintf(result + strlen(result), sizeof(result) - strlen(result),
             "Class Name   : %s\n", classNameVal);
    (*env)->ReleaseStringUTFChars(env, classNameStr, classNameVal);

    /* -- Superclass name -- */
    jclass superClass = (jclass)(*env)->CallObjectMethod(env, targetClass, getSuperclassMethod);
    if (superClass != NULL) {
        jstring superNameStr = (jstring)(*env)->CallObjectMethod(env, superClass, getNameMethod);
        const char *superNameVal = (*env)->GetStringUTFChars(env, superNameStr, NULL);
        snprintf(result + strlen(result), sizeof(result) - strlen(result),
                 "Superclass   : %s\n", superNameVal);
        (*env)->ReleaseStringUTFChars(env, superNameStr, superNameVal);
    } else {
        strncat(result, "Superclass   : (none)\n", sizeof(result) - strlen(result) - 1);
    }

    /* -- Declared Methods -- */
    strncat(result, "\nDeclared Methods:\n", sizeof(result) - strlen(result) - 1);

    jobjectArray methods = (jobjectArray)(*env)->CallObjectMethod(
            env, targetClass, getDeclMethodsMethod);
    if (methods != NULL) {
        jsize methodCount = (*env)->GetArrayLength(env, methods);
        if (methodCount == 0) {
            strncat(result, "  (none)\n", sizeof(result) - strlen(result) - 1);
        }
        for (jsize i = 0; i < methodCount; i++) {
            jobject method = (*env)->GetObjectArrayElement(env, methods, i);
            jstring mSig   = (jstring)(*env)->CallObjectMethod(env, method, methodToGenStr);
            const char *mSigVal = (*env)->GetStringUTFChars(env, mSig, NULL);
            snprintf(result + strlen(result), sizeof(result) - strlen(result),
                     "  [%d] %s\n", (int)(i + 1), mSigVal);
            (*env)->ReleaseStringUTFChars(env, mSig, mSigVal);
            (*env)->DeleteLocalRef(env, method);
            (*env)->DeleteLocalRef(env, mSig);
        }
    } else {
        strncat(result, "  (none)\n", sizeof(result) - strlen(result) - 1);
    }

    /* -- Declared Fields -- */
    strncat(result, "\nDeclared Fields:\n", sizeof(result) - strlen(result) - 1);

    jobjectArray fields = (jobjectArray)(*env)->CallObjectMethod(
            env, targetClass, getDeclFieldsMethod);
    if (fields != NULL) {
        jsize fieldCount = (*env)->GetArrayLength(env, fields);
        if (fieldCount == 0) {
            strncat(result, "  (none)\n", sizeof(result) - strlen(result) - 1);
        }
        for (jsize i = 0; i < fieldCount; i++) {
            jobject field     = (*env)->GetObjectArrayElement(env, fields, i);
            jstring fName     = (jstring)(*env)->CallObjectMethod(env, field, fieldGetName);
            jclass  fTypeClass= (jclass) (*env)->CallObjectMethod(env, field, fieldGetType);
            jstring fTypeName = (jstring)(*env)->CallObjectMethod(env, fTypeClass, getNameMethod);

            const char *fNameVal = (*env)->GetStringUTFChars(env, fName,     NULL);
            const char *fTypeVal = (*env)->GetStringUTFChars(env, fTypeName, NULL);

            snprintf(result + strlen(result), sizeof(result) - strlen(result),
                     "  [%d] %s  (type: %s)\n", (int)(i + 1), fNameVal, fTypeVal);

            (*env)->ReleaseStringUTFChars(env, fName,     fNameVal);
            (*env)->ReleaseStringUTFChars(env, fTypeName, fTypeVal);
            (*env)->DeleteLocalRef(env, field);
            (*env)->DeleteLocalRef(env, fName);
            (*env)->DeleteLocalRef(env, fTypeClass);
            (*env)->DeleteLocalRef(env, fTypeName);
        }
    } else {
        strncat(result, "  (none)\n", sizeof(result) - strlen(result) - 1);
    }

    /* ------------------------------------------------------------------ */
    /* 4. Return the result string to Java                                 */
    /* ------------------------------------------------------------------ */
    return (*env)->NewStringUTF(env, result);
}