#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Helper function to get field type as string
const char* getFieldType(JNIEnv *env, jclass fieldClass) {
    const char* className = (*env)->GetClassName(env, fieldClass);
    
    if (strcmp(className, "java.lang.String") == 0) {
        return "String";
    } else if (strcmp(className, "int") == 0 || strcmp(className, "java.lang.Integer") == 0) {
        return "int";
    } else if (strcmp(className, "double") == 0 || strcmp(className, "java.lang.Double") == 0) {
        return "double";
    } else if (strcmp(className, "boolean") == 0 || strcmp(className, "java.lang.Boolean") == 0) {
        return "boolean";
    } else if (strcmp(className, "java.util.List") == 0 || 
               strcmp(className, "java.util.ArrayList") == 0) {
        return "List";
    } else {
        return className; // Return full class name for custom objects
    }
}

JNIEXPORT jstring JNICALL Java_ObjectInspector_inspectFields
(JNIEnv *env, jobject thisObj, jobject obj) {
    // Get the class of the object
    jclass cls = (*env)->GetObjectClass(env, obj);
    
    // Get the class for java.lang.reflect.Field
    jclass fieldClass = (*env)->FindClass(env, "java/lang/reflect/Field");
    
    // Get the getDeclaredFields method
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    jmethodID getDeclaredFields = (*env)->GetMethodID(env, classClass, "getDeclaredFields", 
                                                      "()[Ljava/lang/reflect/Field;");
    
    // Get all declared fields
    jobjectArray fields = (jobjectArray)(*env)->CallObjectMethod(env, cls, getDeclaredFields);
    if (fields == NULL) {
        return (*env)->NewStringUTF(env, "Could not retrieve fields");
    }
    
    jsize fieldCount = (*env)->GetArrayLength(env, fields);
    
    // Prepare a buffer for the result
    char result[2048];
    strcpy(result, "Fields:\n");
    
    for (int i = 0; i < fieldCount; i++) {
        jobject field = (*env)->GetObjectArrayElement(env, fields, i);
        
        // Get field name
        jmethodID getName = (*env)->GetMethodID(env, fieldClass, "getName", "()Ljava/lang/String;");
        jstring fieldName = (jstring)(*env)->CallObjectMethod(env, field, getName);
        const char* fieldNameStr = (*env)->GetStringUTFChars(env, fieldName, NULL);
        
        // Get field type
        jmethodID getType = (*env)->GetMethodID(env, fieldClass, "getType", "()Ljava/lang/Class;");
        jclass fieldType = (jclass)(*env)->CallObjectMethod(env, field, getType);
        const char* fieldTypeName = getFieldType(env, fieldType);
        
        // Append field info to result
        char temp[256];
        snprintf(temp, sizeof(temp), "  %s %s\n", fieldTypeName, fieldNameStr);
        strcat(result, temp);
        
        // Clean up
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
    }
    
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT jstring JNICALL Java_ObjectInspector_inspectClass
(JNIEnv *env, jobject thisObj, jobject obj) {
    // Get the class of the object
    jclass cls = (*env)->GetObjectClass(env, obj);
    
    // Get class name
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    jmethodID getClassName = (*env)->GetMethodID(env, classClass, "getName", "()Ljava/lang/String;");
    jstring className = (jstring)(*env)->CallObjectMethod(env, cls, getClassName);
    const char* classNameStr = (*env)->GetStringUTFChars(env, className, NULL);
    
    // Get superclass
    jmethodID getSuperclass = (*env)->GetMethodID(env, classClass, "getSuperclass", "()Ljava/lang/Class;");
    jclass superClass = (jclass)(*env)->CallObjectMethod(env, cls, getSuperclass);
    const char* superClassName = "null";
    if (superClass != NULL) {
        jstring superClassNameStr = (jstring)(*env)->CallObjectMethod(env, superClass, getClassName);
        superClassName = (*env)->GetStringUTFChars(env, superClassNameStr, NULL);
    }
    
    // Prepare result
    char result[1024];
    snprintf(result, sizeof(result), 
             "Class: %s\nSuperclass: %s\n", 
             classNameStr, superClassName);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, className, classNameStr);
    if (superClass != NULL) {
        // Note: We don't release superClassNameStr here because we might have allocated it locally
        // In production code, proper memory management would be needed
    }
    
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT jstring JNICALL Java_ObjectInspector_getObjectDetails
(JNIEnv *env, jobject thisObj, jobject obj) {
    // Get the class of the object
    jclass cls = (*env)->GetObjectClass(env, obj);
    
    // Get the class for java.lang.reflect.Field
    jclass fieldClass = (*env)->FindClass(env, "java/lang/reflect/Field");
    
    // Get the getDeclaredFields method
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    jmethodID getDeclaredFields = (*env)->GetMethodID(env, classClass, "getDeclaredFields", 
                                                      "()[Ljava/lang/reflect/Field;");
    
    // Get all declared fields
    jobjectArray fields = (jobjectArray)(*env)->CallObjectMethod(env, cls, getDeclaredFields);
    if (fields == NULL) {
        return (*env)->NewStringUTF(env, "Could not retrieve fields");
    }
    
    jsize fieldCount = (*env)->GetArrayLength(env, fields);
    
    // Prepare a buffer for the result
    char result[4096];
    strcpy(result, "Object Details:\n");
    
    for (int i = 0; i < fieldCount; i++) {
        jobject field = (*env)->GetObjectArrayElement(env, fields, i);
        
        // Get field name
        jmethodID getName = (*env)->GetMethodID(env, fieldClass, "getName", "()Ljava/lang/String;");
        jstring fieldName = (jstring)(*env)->CallObjectMethod(env, field, getName);
        const char* fieldNameStr = (*env)->GetStringUTFChars(env, fieldName, NULL);
        
        // Get field type
        jmethodID getType = (*env)->GetMethodID(env, fieldClass, "getType", "()Ljava/lang/Class;");
        jclass fieldType = (jclass)(*env)->CallObjectMethod(env, field, getType);
        const char* fieldTypeName = getFieldType(env, fieldType);
        
        // Make the field accessible
        jmethodID setAccessible = (*env)->GetMethodID(env, fieldClass, "setAccessible", "(Z)V");
        (*env)->CallVoidMethod(env, field, setAccessible, JNI_TRUE);
        
        // Get field value based on type
        char valueStr[512] = "unknown";
        jmethodID get = (*env)->GetMethodID(env, fieldClass, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        jobject fieldValue = (*env)->CallObjectMethod(env, field, get, obj);
        
        if (fieldValue != NULL) {
            jclass fieldValueClass = (*env)->GetObjectClass(env, fieldValue);
            jmethodID toString = (*env)->GetMethodID(env, fieldValueClass, "toString", "()Ljava/lang/String;");
            jstring valueString = (jstring)(*env)->CallObjectMethod(env, fieldValue, toString);
            const char* valueCString = (*env)->GetStringUTFChars(env, valueString, NULL);
            
            // Handle special cases for collections
            if (strstr(fieldTypeName, "List") || strstr(fieldTypeName, "ArrayList")) {
                snprintf(valueStr, sizeof(valueStr), "%s", valueCString);
            } else {
                snprintf(valueStr, sizeof(valueStr), "%s", valueCString);
            }
            
            (*env)->ReleaseStringUTFChars(env, valueString, valueCString);
        } else {
            snprintf(valueStr, sizeof(valueStr), "null");
        }
        
        // Append field info to result
        char temp[512];
        snprintf(temp, sizeof(temp), "  %s %s = %s\n", fieldTypeName, fieldNameStr, valueStr);
        strcat(result, temp);
        
        // Clean up
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
    }
    
    return (*env)->NewStringUTF(env, result);
}