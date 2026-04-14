#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Helper function to convert Java Class array to JNI signature
char* buildSignature(JNIEnv *env, jclass* paramTypes, int paramCount) {
    if (paramCount == 0) {
        return "()V"; // For methods with no parameters
    }
    
    // Estimate initial size: each parameter could be up to 10 chars, plus return type
    char *signature = malloc(paramCount * 10 + 10);
    strcpy(signature, "(");
    
    for (int i = 0; i < paramCount; i++) {
        if (paramTypes[i] != NULL) {
            const char* className = (*env)->GetClassName(env, paramTypes[i]);
            
            if (strcmp(className, "java.lang.String") == 0) {
                strcat(signature, "Ljava/lang/String;");
            } else if (strcmp(className, "int") == 0 || strcmp(className, "java.lang.Integer") == 0) {
                strcat(signature, "I");
            } else if (strcmp(className, "double") == 0 || strcmp(className, "java.lang.Double") == 0) {
                strcat(signature, "D");
            } else if (strcmp(className, "float") == 0 || strcmp(className, "java.lang.Float") == 0) {
                strcat(signature, "F");
            } else if (strcmp(className, "long") == 0 || strcmp(className, "java.lang.Long") == 0) {
                strcat(signature, "J");
            } else if (strcmp(className, "boolean") == 0 || strcmp(className, "java.lang.Boolean") == 0) {
                strcat(signature, "Z");
            } else if (strcmp(className, "byte") == 0 || strcmp(className, "java.lang.Byte") == 0) {
                strcat(signature, "B");
            } else if (strcmp(className, "char") == 0 || strcmp(className, "java.lang.Character") == 0) {
                strcat(signature, "C");
            } else if (strcmp(className, "short") == 0 || strcmp(className, "java.lang.Short") == 0) {
                strcat(signature, "S");
            } else {
                // For other reference types, use L<ClassName>;
                strcat(signature, "L");
                strcat(signature, className);
                strcat(signature, ";");
            }
        }
    }
    
    strcat(signature, ")Ljava/lang/Object;"); // Generic return type
    
    return signature;
}

JNIEXPORT jobject JNICALL Java_MethodInvocation_invokeMethod
(JNIEnv *env, jobject thisObj, jobject obj, jstring methodName, jobjectArray paramTypes, jobjectArray args) {
    // Convert jstring to C string
    const char *methodNameStr = (*env)->GetStringUTFChars(env, methodName, NULL);
    
    // Get the class of the object
    jclass cls = (*env)->GetObjectClass(env, obj);
    
    // Determine number of parameters
    int paramCount = 0;
    if (paramTypes != NULL) {
        paramCount = (*env)->GetArrayLength(env, paramTypes);
    }
    
    // Build signature for the method
    jclass *types = NULL;
    char *signature = NULL;
    
    if (paramCount > 0) {
        types = malloc(paramCount * sizeof(jclass));
        for (int i = 0; i < paramCount; i++) {
            jobject typeObj = (*env)->GetObjectArrayElement(env, paramTypes, i);
            if (typeObj != NULL) {
                types[i] = (*env)->GetObjectClass(env, typeObj);
            } else {
                types[i] = NULL;
            }
        }
        
        // Build signature based on parameter types
        signature = buildSignature(env, types, paramCount);
    } else {
        signature = "()Ljava/lang/Object;";
    }
    
    // Get the method ID
    jmethodID methodID = (*env)->GetMethodID(env, cls, methodNameStr, signature);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, methodName, methodNameStr);
    
    if (methodID == NULL) {
        if (types) free(types);
        if (signature && paramCount > 0) free(signature);
        return NULL; // Method not found
    }
    
    // Prepare arguments for the method call
    jvalue *argsArray = NULL;
    if (paramCount > 0) {
        argsArray = malloc(paramCount * sizeof(jvalue));
        for (int i = 0; i < paramCount; i++) {
            jobject arg = (*env)->GetObjectArrayElement(env, args, i);
            argsArray[i].l = arg; // For object arguments
        }
    }
    
    // Call the method
    jobject result = NULL;
    if (paramCount > 0) {
        result = (*env)->CallObjectMethodA(env, obj, methodID, argsArray);
    } else {
        result = (*env)->CallObjectMethod(env, obj, methodID);  // No-arg version
    }
    
    // Clean up
    if (argsArray) {
        free(argsArray);
    }
    if (types) {
        free(types);
    }
    if (signature && paramCount > 0) {
        free(signature);
    }
    
    return result;
}

JNIEXPORT jobject JNICALL Java_MethodInvocation_invokeStaticMethod
(JNIEnv *env, jobject thisObj, jclass clazz, jstring methodName, jobjectArray paramTypes, jobjectArray args) {
    // Convert jstring to C string
    const char *methodNameStr = (*env)->GetStringUTFChars(env, methodName, NULL);
    
    // Determine number of parameters
    int paramCount = 0;
    if (paramTypes != NULL) {
        paramCount = (*env)->GetArrayLength(env, paramTypes);
    }
    
    // Build signature for the method
    jclass *types = NULL;
    char *signature = NULL;
    
    if (paramCount > 0) {
        types = malloc(paramCount * sizeof(jclass));
        for (int i = 0; i < paramCount; i++) {
            jobject typeObj = (*env)->GetObjectArrayElement(env, paramTypes, i);
            if (typeObj != NULL) {
                types[i] = (*env)->GetObjectClass(env, typeObj);
            } else {
                types[i] = NULL;
            }
        }
        
        // Build signature based on parameter types
        signature = buildSignature(env, types, paramCount);
    } else {
        signature = "()Ljava/lang/Object;";
    }
    
    // Get the static method ID
    jmethodID methodID = (*env)->GetStaticMethodID(env, clazz, methodNameStr, signature);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, methodName, methodNameStr);
    
    if (methodID == NULL) {
        if (types) free(types);
        if (signature && paramCount > 0) free(signature);
        return NULL; // Method not found
    }
    
    // Prepare arguments for the method call
    jvalue *argsArray = NULL;
    if (paramCount > 0) {
        argsArray = malloc(paramCount * sizeof(jvalue));
        for (int i = 0; i < paramCount; i++) {
            jobject arg = (*env)->GetObjectArrayElement(env, args, i);
            argsArray[i].l = arg; // For object arguments
        }
    }
    
    // Call the static method
    jobject result = NULL;
    if (paramCount > 0) {
        result = (*env)->CallStaticObjectMethodA(env, clazz, methodID, argsArray);
    } else {
        result = (*env)->CallStaticObjectMethod(env, clazz, methodID);  // No-arg version
    }
    
    // Clean up
    if (argsArray) {
        free(argsArray);
    }
    if (types) {
        free(types);
    }
    if (signature && paramCount > 0) {
        free(signature);
    }
    
    return result;
}