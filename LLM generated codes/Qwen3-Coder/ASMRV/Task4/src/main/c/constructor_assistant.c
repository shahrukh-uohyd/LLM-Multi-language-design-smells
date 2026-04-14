#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Helper function to convert Java class name to JNI signature format
char* convertToJNISignature(const char* javaClassName) {
    // Allocate memory for the signature (add space for L and ;)
    char* signature = malloc(strlen(javaClassName) + 3);
    strcpy(signature, "L");
    strcat(signature, javaClassName);
    strcat(signature, ";");
    
    // Replace dots with forward slashes
    for (int i = 0; signature[i] != '\0'; i++) {
        if (signature[i] == '.') {
            signature[i] = '/';
        }
    }
    
    return signature;
}

// Helper function to build constructor signature
char* buildConstructorSignature(JNIEnv *env, jobjectArray parameterTypes) {
    jsize paramCount = (*env)->GetArrayLength(env, parameterTypes);
    
    // Allocate initial memory for signature
    char* signature = malloc(512); // Max reasonable size
    strcpy(signature, "(");
    
    for (int i = 0; i < paramCount; i++) {
        jstring paramType = (jstring)(*env)->GetObjectArrayElement(env, parameterTypes, i);
        const char* paramTypeStr = (*env)->GetStringUTFChars(env, paramType, NULL);
        
        // Convert Java type to JNI signature
        if (strcmp(paramTypeStr, "int") == 0) {
            strcat(signature, "I");
        } else if (strcmp(paramTypeStr, "long") == 0) {
            strcat(signature, "J");
        } else if (strcmp(paramTypeStr, "float") == 0) {
            strcat(signature, "F");
        } else if (strcmp(paramTypeStr, "double") == 0) {
            strcat(signature, "D");
        } else if (strcmp(paramTypeStr, "boolean") == 0) {
            strcat(signature, "Z");
        } else if (strcmp(paramTypeStr, "char") == 0) {
            strcat(signature, "C");
        } else if (strcmp(paramTypeStr, "byte") == 0) {
            strcat(signature, "B");
        } else if (strcmp(paramTypeStr, "short") == 0) {
            strcat(signature, "S");
        } else {
            // Handle object types
            char* convertedType = convertToJNISignature(paramTypeStr);
            strcat(signature, convertedType);
            free(convertedType);
        }
        
        (*env)->ReleaseStringUTFChars(env, paramType, paramTypeStr);
    }
    
    strcat(signature, ")V"); // Constructor always returns void
    return signature;
}

JNIEXPORT jstring JNICALL Java_com_example_ConstructorAssistant_prepareConstructorData
(JNIEnv *env, jobject obj, jstring className, jobjectArray parameterTypes, jobjectArray parameterValues) {
    const char *classNameStr = (*env)->GetStringUTFChars(env, className, NULL);
    
    // Build constructor signature
    char* signature = buildConstructorSignature(env, parameterTypes);
    
    // Find the class
    jclass clazz = (*env)->FindClass(env, classNameStr);
    if (clazz == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        free(signature);
        return (*env)->NewStringUTF(env, "Error: Class not found");
    }
    
    // Find the constructor using GetMethodID (as required by the detection alignment)
    jmethodID constructorID = (*env)->GetMethodID(env, clazz, "<init>", signature);
    if (constructorID == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        free(signature);
        // Clear the pending exception
        (*env)->ExceptionClear(env);
        char errorMsg[256];
        snprintf(errorMsg, sizeof(errorMsg), "Error: Constructor not found with signature: %s", signature);
        return (*env)->NewStringUTF(env, errorMsg);
    }
    
    // Prepare return message
    char result[1024];
    snprintf(result, sizeof(result), 
             "Constructor found for class '%s' with signature '%s'. "
             "Parameter count: %d. Ready for object construction.",
             classNameStr, signature, (*env)->GetArrayLength(env, parameterTypes));
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, className, classNameStr);
    free(signature);
    
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT jboolean JNICALL Java_com_example_ConstructorAssistant_validateConstructor
(JNIEnv *env, jobject obj, jstring className, jobjectArray parameterTypes) {
    const char *classNameStr = (*env)->GetStringUTFChars(env, className, NULL);
    
    // Build constructor signature
    char* signature = buildConstructorSignature(env, parameterTypes);
    
    // Find the class
    jclass clazz = (*env)->FindClass(env, classNameStr);
    if (clazz == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        free(signature);
        return JNI_FALSE;
    }
    
    // Find the constructor using GetMethodID (as required by the detection alignment)
    jmethodID constructorID = (*env)->GetMethodID(env, clazz, "<init>", signature);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, className, classNameStr);
    free(signature);
    
    // Return whether constructor was found (NULL means not found)
    return (constructorID != NULL) ? JNI_TRUE : JNI_FALSE;
}