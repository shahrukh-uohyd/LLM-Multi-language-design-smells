#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Helper function to get class modifiers
jstring getClassModifiers(JNIEnv *env, jclass clazz) {
    jclass classClass = (*env)->GetObjectClass(env, clazz);
    jmethodID getModifiersMethod = (*env)->GetMethodID(env, classClass, "getModifiers", "()I");
    jint modifiers = (*env)->CallIntMethod(env, clazz, getModifiersMethod);
    
    jclass modifierClass = (*env)->FindClass(env, "java/lang/reflect/Modifier");
    jmethodID toStringMethod = (*env)->GetStaticMethodID(env, modifierClass, "toString", "(I)Ljava/lang/String;");
    jstring modifiersStr = (jstring)(*env)->CallStaticObjectMethod(env, modifierClass, toStringMethod, modifiers);
    
    return modifiersStr;
}

// Helper function to convert Java class name to JNI signature
char* convertToJNISignature(const char* javaClassName) {
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

// Helper function to build method signature
char* buildMethodSignature(JNIEnv *env, jobjectArray parameterTypes) {
    jsize paramCount = (*env)->GetArrayLength(env, parameterTypes);
    
    char* signature = malloc(512);
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
    
    strcat(signature, ")Ljava/lang/Object;"); // We'll use Object as return type for simplicity
    return signature;
}

JNIEXPORT jstring JNICALL Java_com_example_ReflectionSupport_getClassInfo
(JNIEnv *env, jobject obj, jstring className) {
    const char *classNameStr = (*env)->GetStringUTFChars(env, className, NULL);
    
    // Find the class
    jclass clazz = (*env)->FindClass(env, classNameStr);
    if (clazz == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        return (*env)->NewStringUTF(env, "Error: Class not found");
    }
    
    // Get class name
    jclass classClass = (*env)->GetObjectClass(env, clazz);
    jmethodID getNameMethod = (*env)->GetMethodID(env, classClass, "getName", "()Ljava/lang/String;");
    jstring actualClassName = (jstring)(*env)->CallObjectMethod(env, clazz, getNameMethod);
    const char* actualClassNameStr = (*env)->GetStringUTFChars(env, actualClassName, NULL);
    
    // Get superclass
    jmethodID getSuperclassMethod = (*env)->GetMethodID(env, classClass, "getSuperclass", "()Ljava/lang/Class;");
    jclass superClass = (jclass)(*env)->CallObjectMethod(env, clazz, getSuperclassMethod);
    jstring superClassName = NULL;
    const char* superClassNameStr = "null";
    if (superClass != NULL) {
        superClassName = (jstring)(*env)->CallObjectMethod(env, superClass, getNameMethod);
        superClassNameStr = (*env)->GetStringUTFChars(env, superClassName, NULL);
    }
    
    // Get modifiers
    jstring modifiersStr = getClassModifiers(env, clazz);
    const char* modifiers = (*env)->GetStringUTFChars(env, modifiersStr, NULL);
    
    // Check if it's an interface
    jmethodID isInterfaceMethod = (*env)->GetMethodID(env, classClass, "isInterface", "()Z");
    jboolean isInterface = (*env)->CallBooleanMethod(env, clazz, isInterfaceMethod);
    
    // Count fields and methods
    jmethodID getDeclaredFieldsMethod = (*env)->GetMethodID(env, classClass, "getDeclaredFields", "()[Ljava/lang/reflect/Field;");
    jobjectArray fields = (jobjectArray)(*env)->CallObjectMethod(env, clazz, getDeclaredFieldsMethod);
    int fieldCount = (*env)->GetArrayLength(env, fields);
    
    jmethodID getDeclaredMethodsMethod = (*env)->GetMethodID(env, classClass, "getDeclaredMethods", "()[Ljava/lang/reflect/Method;");
    jobjectArray methods = (jobjectArray)(*env)->CallObjectMethod(env, clazz, getDeclaredMethodsMethod);
    int methodCount = (*env)->GetArrayLength(env, methods);
    
    // Build result
    char result[2048];
    snprintf(result, sizeof(result),
             "Class: %s\n"
             "Superclass: %s\n"
             "Is Interface: %s\n"
             "Modifiers: %s\n"
             "Field Count: %d\n"
             "Method Count: %d\n",
             actualClassNameStr,
             superClassNameStr,
             isInterface ? "true" : "false",
             modifiers,
             fieldCount,
             methodCount);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, className, classNameStr);
    (*env)->ReleaseStringUTFChars(env, actualClassName, actualClassNameStr);
    if (superClass != NULL) {
        (*env)->ReleaseStringUTFChars(env, superClassName, superClassNameStr);
    }
    (*env)->ReleaseStringUTFChars(env, modifiersStr, modifiers);
    
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT jstring JNICALL Java_com_example_ReflectionSupport_getFieldInfo
(JNIEnv *env, jobject obj, jstring className, jstring fieldName) {
    const char *classNameStr = (*env)->GetStringUTFChars(env, className, NULL);
    const char *fieldNameStr = (*env)->GetStringUTFChars(env, fieldName, NULL);
    
    // Find the class
    jclass clazz = (*env)->FindClass(env, classNameStr);
    if (clazz == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
        return (*env)->NewStringUTF(env, "Error: Class not found");
    }
    
    // Find the field
    jfieldID fieldID = (*env)->GetFieldID(env, clazz, fieldNameStr, NULL);
    if (fieldID == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
        // Clear the pending exception
        (*env)->ExceptionClear(env);
        char errorMsg[256];
        snprintf(errorMsg, sizeof(errorMsg), "Error: Field '%s' not found", fieldNameStr);
        return (*env)->NewStringUTF(env, errorMsg);
    }
    
    // Get field class
    jclass fieldClass = (*env)->FindClass(env, "java/lang/reflect/Field");
    jmethodID getFieldMethod = (*env)->GetMethodID(env, clazz, "getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
    jobject fieldObj = (*env)->CallObjectMethod(env, clazz, getFieldMethod, fieldName);
    
    if (fieldObj == NULL) {
        // Try getDeclaredField instead
        jmethodID getDeclaredFieldMethod = (*env)->GetMethodID(env, clazz, "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
        fieldObj = (*env)->CallObjectMethod(env, clazz, getDeclaredFieldMethod, fieldName);
    }
    
    if (fieldObj == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
        (*env)->ExceptionClear(env);
        char errorMsg[256];
        snprintf(errorMsg, sizeof(errorMsg), "Error: Cannot get Field object for '%s'", fieldNameStr);
        return (*env)->NewStringUTF(env, errorMsg);
    }
    
    // Get field name
    jmethodID getNameMethod = (*env)->GetMethodID(env, fieldClass, "getName", "()Ljava/lang/String;");
    jstring actualFieldName = (jstring)(*env)->CallObjectMethod(env, fieldObj, getNameMethod);
    const char* actualFieldNameStr = (*env)->GetStringUTFChars(env, actualFieldName, NULL);
    
    // Get field type
    jmethodID getTypeMethod = (*env)->GetMethodID(env, fieldClass, "getType", "()Ljava/lang/Class;");
    jobject fieldType = (*env)->CallObjectMethod(env, fieldObj, getTypeMethod);
    jstring fieldTypeName = (jstring)(*env)->CallObjectMethod(env, fieldType, getNameMethod);
    const char* fieldTypeNameStr = (*env)->GetStringUTFChars(env, fieldTypeName, NULL);
    
    // Get field modifiers
    jclass modifierClass = (*env)->FindClass(env, "java/lang/reflect/Modifier");
    jmethodID getModifiersMethod = (*env)->GetMethodID(env, fieldClass, "getModifiers", "()I");
    jint modifiers = (*env)->CallIntMethod(env, fieldObj, getModifiersMethod);
    jmethodID toStringMethod = (*env)->GetStaticMethodID(env, modifierClass, "toString", "(I)Ljava/lang/String;");
    jstring modifiersStr = (jstring)(*env)->CallStaticObjectMethod(env, modifierClass, toStringMethod, modifiers);
    const char* modifiersText = (*env)->GetStringUTFChars(env, modifiersStr, NULL);
    
    // Build result
    char result[1024];
    snprintf(result, sizeof(result),
             "Field: %s\n"
             "Type: %s\n"
             "Modifiers: %s\n"
             "Declaring Class: %s\n",
             actualFieldNameStr,
             fieldTypeNameStr,
             modifiersText,
             classNameStr);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, className, classNameStr);
    (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
    (*env)->ReleaseStringUTFChars(env, actualFieldName, actualFieldNameStr);
    (*env)->ReleaseStringUTFChars(env, fieldTypeName, fieldTypeNameStr);
    (*env)->ReleaseStringUTFChars(env, modifiersStr, modifiersText);
    
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT jstring JNICALL Java_com_example_ReflectionSupport_getMethodInfo
(JNIEnv *env, jobject obj, jstring className, jstring methodName, jobjectArray parameterTypes) {
    const char *classNameStr = (*env)->GetStringUTFChars(env, className, NULL);
    const char *methodNameStr = (*env)->GetStringUTFChars(env, methodName, NULL);
    
    // Find the class
    jclass clazz = (*env)->FindClass(env, classNameStr);
    if (clazz == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        (*env)->ReleaseStringUTFChars(env, methodName, methodNameStr);
        return (*env)->NewStringUTF(env, "Error: Class not found");
    }
    
    // Build method signature
    char* signature = buildMethodSignature(env, parameterTypes);
    
    // Find the method
    jmethodID methodID = (*env)->GetMethodID(env, clazz, methodNameStr, signature);
    if (methodID == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        (*env)->ReleaseStringUTFChars(env, methodName, methodNameStr);
        free(signature);
        (*env)->ExceptionClear(env);
        char errorMsg[256];
        snprintf(errorMsg, sizeof(errorMsg), "Error: Method '%s' not found with signature '%s'", methodNameStr, signature);
        return (*env)->NewStringUTF(env, errorMsg);
    }
    
    // Get method class
    jclass methodClass = (*env)->FindClass(env, "java/lang/reflect/Method");
    jmethodID getMethodMethod = (*env)->GetMethodID(env, clazz, "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
    
    // Get parameter count
    jsize paramCount = (*env)->GetArrayLength(env, parameterTypes);
    
    // Build parameter class array for getMethod call
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    jobjectArray paramClasses = (*env)->NewObjectArray(env, paramCount, classClass, NULL);
    
    for (int i = 0; i < paramCount; i++) {
        jstring paramType = (jstring)(*env)->GetObjectArrayElement(env, parameterTypes, i);
        const char* paramTypeStr = (*env)->GetStringUTFChars(env, paramType, NULL);
        
        jclass paramClazz = (*env)->FindClass(env, paramTypeStr);
        (*env)->SetObjectArrayElement(env, paramClasses, i, paramClazz);
        
        (*env)->ReleaseStringUTFChars(env, paramType, paramTypeStr);
    }
    
    jobject methodObj = (*env)->CallObjectMethod(env, clazz, getMethodMethod, methodName, paramClasses);
    if (methodObj == NULL) {
        // Try getDeclaredMethod instead
        jmethodID getDeclaredMethodMethod = (*env)->GetMethodID(env, clazz, "getDeclaredMethod", 
                                                               "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
        methodObj = (*env)->CallObjectMethod(env, clazz, getDeclaredMethodMethod, methodName, paramClasses);
    }
    
    if (methodObj == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        (*env)->ReleaseStringUTFChars(env, methodName, methodNameStr);
        free(signature);
        (*env)->ExceptionClear(env);
        char errorMsg[256];
        snprintf(errorMsg, sizeof(errorMsg), "Error: Cannot get Method object for '%s'", methodNameStr);
        return (*env)->NewStringUTF(env, errorMsg);
    }
    
    // Get method name
    jmethodID getNameMethod = (*env)->GetMethodID(env, methodClass, "getName", "()Ljava/lang/String;");
    jstring actualMethodName = (jstring)(*env)->CallObjectMethod(env, methodObj, getNameMethod);
    const char* actualMethodNameStr = (*env)->GetStringUTFChars(env, actualMethodName, NULL);
    
    // Get return type
    jmethodID getReturnTypeMethod = (*env)->GetMethodID(env, methodClass, "getReturnType", "()Ljava/lang/Class;");
    jobject returnType = (*env)->CallObjectMethod(env, methodObj, getReturnTypeMethod);
    jstring returnTypeName = (jstring)(*env)->CallObjectMethod(env, returnType, getNameMethod);
    const char* returnTypeNameStr = (*env)->GetStringUTFChars(env, returnTypeName, NULL);
    
    // Get method modifiers
    jclass modifierClass = (*env)->FindClass(env, "java/lang/reflect/Modifier");
    jmethodID getModifiersMethod = (*env)->GetMethodID(env, methodClass, "getModifiers", "()I");
    jint modifiers = (*env)->CallIntMethod(env, methodObj, getModifiersMethod);
    jmethodID toStringMethod = (*env)->GetStaticMethodID(env, modifierClass, "toString", "(I)Ljava/lang/String;");
    jstring modifiersStr = (jstring)(*env)->CallStaticObjectMethod(env, modifierClass, toStringMethod, modifiers);
    const char* modifiersText = (*env)->GetStringUTFChars(env, modifiersStr, NULL);
    
    // Build result
    char result[1024];
    snprintf(result, sizeof(result),
             "Method: %s\n"
             "Return Type: %s\n"
             "Parameter Count: %d\n"
             "Modifiers: %s\n"
             "Declaring Class: %s\n",
             actualMethodNameStr,
             returnTypeNameStr,
             paramCount,
             modifiersText,
             classNameStr);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, className, classNameStr);
    (*env)->ReleaseStringUTFChars(env, methodName, methodNameStr);
    (*env)->ReleaseStringUTFChars(env, actualMethodName, actualMethodNameStr);
    (*env)->ReleaseStringUTFChars(env, returnTypeName, returnTypeNameStr);
    (*env)->ReleaseStringUTFChars(env, modifiersStr, modifiersText);
    free(signature);
    
    return (*env)->NewStringUTF(env, result);
}

JNIEXPORT jstring JNICALL Java_com_example_ReflectionSupport_getAllMembers
(JNIEnv *env, jobject obj, jstring className) {
    const char *classNameStr = (*env)->GetStringUTFChars(env, className, NULL);
    
    // Find the class
    jclass clazz = (*env)->FindClass(env, classNameStr);
    if (clazz == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        return (*env)->NewStringUTF(env, "Error: Class not found");
    }
    
    // Get fields
    jclass classClass = (*env)->GetObjectClass(env, clazz);
    jmethodID getDeclaredFieldsMethod = (*env)->GetMethodID(env, classClass, "getDeclaredFields", "()[Ljava/lang/reflect/Field;");
    jobjectArray fields = (jobjectArray)(*env)->CallObjectMethod(env, clazz, getDeclaredFieldsMethod);
    int fieldCount = (*env)->GetArrayLength(env, fields);
    
    // Get methods
    jmethodID getDeclaredMethodsMethod = (*env)->GetMethodID(env, classClass, "getDeclaredMethods", "()[Ljava/lang/reflect/Method;");
    jobjectArray methods = (jobjectArray)(*env)->CallObjectMethod(env, clazz, getDeclaredMethodsMethod);
    int methodCount = (*env)->GetArrayLength(env, methods);
    
    // Build result
    char *result = malloc(4096); // Allocate sufficient space
    strcpy(result, "=== ALL MEMBERS ===\n");
    
    // Add field info
    strcat(result, "\nFIELDS:\n");
    jclass fieldClass = (*env)->GetObjectClass(env, (*env)->GetObjectArrayElement(env, fields, 0));
    jmethodID fieldNameMethod = (*env)->GetMethodID(env, fieldClass, "getName", "()Ljava/lang/String;");
    jmethodID fieldTypeMethod = (*env)->GetMethodID(env, fieldClass, "getType", "()Ljava/lang/Class;");
    jmethodID fieldGetNameMethod = (*env)->GetMethodID(env, (*env)->GetObjectClass(env, (*env)->CallObjectMethod(env, 
        (*env)->GetObjectArrayElement(env, fields, 0), fieldTypeMethod)), "getName", "()Ljava/lang/String;");
    
    for (int i = 0; i < fieldCount; i++) {
        jobject field = (*env)->GetObjectArrayElement(env, fields, i);
        jstring fieldName = (jstring)(*env)->CallObjectMethod(env, field, fieldNameMethod);
        jobject fieldType = (*env)->CallObjectMethod(env, field, fieldTypeMethod);
        jstring fieldTypeName = (jstring)(*env)->CallObjectMethod(env, fieldType, fieldGetNameMethod);
        
        const char* fieldNameStr = (*env)->GetStringUTFChars(env, fieldName, NULL);
        const char* fieldTypeNameStr = (*env)->GetStringUTFChars(env, fieldTypeName, NULL);
        
        char fieldInfo[256];
        snprintf(fieldInfo, sizeof(fieldInfo), "  %s %s\n", fieldTypeNameStr, fieldNameStr);
        strcat(result, fieldInfo);
        
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
        (*env)->ReleaseStringUTFChars(env, fieldTypeName, fieldTypeNameStr);
    }
    
    // Add method info
    strcat(result, "\nMETHODS:\n");
    jclass methodClass = (*env)->GetObjectClass(env, (*env)->GetObjectArrayElement(env, methods, 0));
    jmethodID methodNameMethod = (*env)->GetMethodID(env, methodClass, "getName", "()Ljava/lang/String;");
    jmethodID getParameterTypesMethod = (*env)->GetMethodID(env, methodClass, "getParameterTypes", "()[Ljava/lang/Class;");
    jmethodID getReturnMethod = (*env)->GetMethodID(env, methodClass, "getReturnType", "()Ljava/lang/Class;");
    
    for (int i = 0; i < methodCount; i++) {
        jobject method = (*env)->GetObjectArrayElement(env, methods, i);
        jstring methodName = (jstring)(*env)->CallObjectMethod(env, method, methodNameMethod);
        jobject returnType = (*env)->CallObjectMethod(env, method, getReturnMethod);
        jstring returnTypeName = (jstring)(*env)->CallObjectMethod(env, returnType, fieldGetNameMethod); // Reusing the getName method
        
        const char* methodNameStr = (*env)->GetStringUTFChars(env, methodName, NULL);
        const char* returnTypeNameStr = (*env)->GetStringUTFChars(env, returnTypeName, NULL);
        
        // Get parameter types
        jobjectArray paramTypes = (jobjectArray)(*env)->CallObjectMethod(env, method, getParameterTypesMethod);
        int paramCount = (*env)->GetArrayLength(env, paramTypes);
        
        char paramList[512] = "";
        for (int j = 0; j < paramCount; j++) {
            jobject paramType = (*env)->GetObjectArrayElement(env, paramTypes, j);
            jstring paramName = (jstring)(*env)->CallObjectMethod(env, paramType, fieldGetNameMethod);
            const char* paramNameStr = (*env)->GetStringUTFChars(env, paramName, NULL);
            
            if (j > 0) strcat(paramList, ", ");
            strcat(paramList, paramNameStr);
            
            (*env)->ReleaseStringUTFChars(env, paramName, paramNameStr);
        }
        
        char methodInfo[512];
        snprintf(methodInfo, sizeof(methodInfo), "  %s %s(%s)\n", returnTypeNameStr, methodNameStr, paramList);
        strcat(result, methodInfo);
        
        (*env)->ReleaseStringUTFChars(env, methodName, methodNameStr);
        (*env)->ReleaseStringUTFChars(env, returnTypeName, returnTypeNameStr);
    }
    
    jstring resultStr = (*env)->NewStringUTF(env, result);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, className, classNameStr);
    free(result);
    
    return resultStr;
}