#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Helper function to get method signatures
jstring getMethodSignature(JNIEnv *env, jobject method) {
    jclass methodClass = (*env)->GetObjectClass(env, method);
    jmethodID getNameMethod = (*env)->GetMethodID(env, methodClass, "getName", "()Ljava/lang/String;");
    jmethodID getParameterTypesMethod = (*env)->GetMethodID(env, methodClass, "getParameterTypes", "()[Ljava/lang/Class;");
    jmethodID getReturnTypeMethod = (*env)->GetMethodID(env, methodClass, "getReturnType", "()Ljava/lang/Class;");
    
    jstring methodName = (jstring)(*env)->CallObjectMethod(env, method, getNameMethod);
    jobjectArray paramTypes = (jobjectArray)(*env)->CallObjectMethod(env, method, getParameterTypesMethod);
    jobject returnType = (*env)->CallObjectMethod(env, method, getReturnTypeMethod);
    
    const char* methodNameStr = (*env)->GetStringUTFChars(env, methodName, NULL);
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    jmethodID getNameClassMethod = (*env)->GetMethodID(env, classClass, "getName", "()Ljava/lang/String;");
    
    jstring returnTypeName = (jstring)(*env)->CallObjectMethod(env, returnType, getNameClassMethod);
    const char* returnTypeNameStr = (*env)->GetStringUTFChars(env, returnTypeName, NULL);
    
    // Build parameter list
    int paramCount = (*env)->GetArrayLength(env, paramTypes);
    char paramBuffer[1024] = "";
    for (int i = 0; i < paramCount; i++) {
        jobject paramType = (*env)->GetObjectArrayElement(env, paramTypes, i);
        jstring paramName = (jstring)(*env)->CallObjectMethod(env, paramType, getNameClassMethod);
        const char* paramNameStr = (*env)->GetStringUTFChars(env, paramName, NULL);
        
        if (i > 0) strcat(paramBuffer, ", ");
        strcat(paramBuffer, paramNameStr);
        
        (*env)->ReleaseStringUTFChars(env, paramName, paramNameStr);
    }
    
    // Create signature string
    char signature[2048];
    snprintf(signature, sizeof(signature), "%s %s(%s)", returnTypeNameStr, methodNameStr, paramBuffer);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, methodName, methodNameStr);
    (*env)->ReleaseStringUTFChars(env, returnTypeName, returnTypeNameStr);
    
    return (*env)->NewStringUTF(env, signature);
}

JNIEXPORT jstring JNICALL Java_com_example_ClassMetadataRetriever_getClassMetadata
(JNIEnv *env, jobject obj, jstring className) {
    const char *classNameStr = (*env)->GetStringUTFChars(env, className, NULL);
    
    // Find the class
    jclass targetClass = (*env)->FindClass(env, classNameStr);
    if (targetClass == NULL) {
        (*env)->ReleaseStringUTFChars(env, className, classNameStr);
        return (*env)->NewStringUTF(env, "Error: Class not found");
    }
    
    // Get class name
    jclass classClass = (*env)->GetObjectClass(env, targetClass);
    jmethodID getNameMethod = (*env)->GetMethodID(env, classClass, "getName", "()Ljava/lang/String;");
    jstring actualClassName = (jstring)(*env)->CallObjectMethod(env, targetClass, getNameMethod);
    const char* actualClassNameStr = (*env)->GetStringUTFChars(env, actualClassName, NULL);
    
    // Get superclass
    jmethodID getSuperclassMethod = (*env)->GetMethodID(env, classClass, "getSuperclass", "()Ljava/lang/Class;");
    jclass superClass = (jclass)(*env)->CallObjectMethod(env, targetClass, getSuperclassMethod);
    jstring superClassName = NULL;
    const char* superClassNameStr = "null";
    if (superClass != NULL) {
        superClassName = (jstring)(*env)->CallObjectMethod(env, superClass, getNameMethod);
        superClassNameStr = (*env)->GetStringUTFChars(env, superClassName, NULL);
    }
    
    // Check if it's an interface
    jmethodID isInterfaceMethod = (*env)->GetMethodID(env, classClass, "isInterface", "()Z");
    jboolean isInterface = (*env)->CallBooleanMethod(env, targetClass, isInterfaceMethod);
    
    // Check if it's abstract
    jclass modifierClass = (*env)->FindClass(env, "java/lang/reflect/Modifier");
    jmethodID getModifiersMethod = (*env)->GetMethodID(env, classClass, "getModifiers", "()I");
    jmethodID isAbstractMethod = (*env)->GetStaticMethodID(env, modifierClass, "isAbstract", "(I)Z");
    jint modifiers = (*env)->CallIntMethod(env, targetClass, getModifiersMethod);
    jboolean isAbstract = (*env)->CallStaticBooleanMethod(env, modifierClass, isAbstractMethod, modifiers);
    
    // Get fields
    jmethodID getDeclaredFieldsMethod = (*env)->GetMethodID(env, classClass, "getDeclaredFields", "()[Ljava/lang/reflect/Field;");
    jobjectArray fields = (jobjectArray)(*env)->CallObjectMethod(env, targetClass, getDeclaredFieldsMethod);
    int fieldCount = (*env)->GetArrayLength(env, fields);
    
    // Get methods
    jmethodID getDeclaredMethodsMethod = (*env)->GetMethodID(env, classClass, "getDeclaredMethods", "()[Ljava/lang/reflect/Method;");
    jobjectArray methods = (jobjectArray)(*env)->CallObjectMethod(env, targetClass, getDeclaredMethodsMethod);
    int methodCount = (*env)->GetArrayLength(env, methods);
    
    // Build result string
    char result[4096];
    snprintf(result, sizeof(result),
             "Class Name: %s\n"
             "Superclass: %s\n"
             "Is Interface: %s\n"
             "Is Abstract: %s\n"
             "Field Count: %d\n"
             "Method Count: %d\n\n",
             actualClassNameStr,
             superClassNameStr,
             isInterface ? "true" : "false",
             isAbstract ? "true" : "false",
             fieldCount,
             methodCount);
    
    // Add field details
    strcat(result, "Fields:\n");
    jclass fieldClass = (*env)->GetObjectClass(env, (*env)->GetObjectArrayElement(env, fields, 0));
    jmethodID getFieldNameMethod = (*env)->GetMethodID(env, fieldClass, "getName", "()Ljava/lang/String;");
    jmethodID getFieldTypeMethod = (*env)->GetMethodID(env, fieldClass, "getType", "()Ljava/lang/Class;");
    
    for (int i = 0; i < fieldCount; i++) {
        jobject field = (*env)->GetObjectArrayElement(env, fields, i);
        jstring fieldName = (jstring)(*env)->CallObjectMethod(env, field, getFieldNameMethod);
        jobject fieldType = (*env)->CallObjectMethod(env, field, getFieldTypeMethod);
        jstring fieldTypeName = (jstring)(*env)->CallObjectMethod(env, fieldType, getNameMethod);
        
        const char* fieldNameStr = (*env)->GetStringUTFChars(env, fieldName, NULL);
        const char* fieldTypeNameStr = (*env)->GetStringUTFChars(env, fieldTypeName, NULL);
        
        char fieldInfo[512];
        snprintf(fieldInfo, sizeof(fieldInfo), "  %s %s\n", fieldTypeNameStr, fieldNameStr);
        strcat(result, fieldInfo);
        
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
        (*env)->ReleaseStringUTFChars(env, fieldTypeName, fieldTypeNameStr);
    }
    
    // Add method details
    strcat(result, "\nMethods:\n");
    for (int i = 0; i < methodCount; i++) {
        jobject method = (*env)->GetObjectArrayElement(env, methods, i);
        jstring methodSig = getMethodSignature(env, method);
        const char* methodSigStr = (*env)->GetStringUTFChars(env, methodSig, NULL);
        
        char methodInfo[1024];
        snprintf(methodInfo, sizeof(methodInfo), "  %s\n", methodSigStr);
        strcat(result, methodInfo);
        
        (*env)->ReleaseStringUTFChars(env, methodSig, methodSigStr);
    }
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, className, classNameStr);
    (*env)->ReleaseStringUTFChars(env, actualClassName, actualClassNameStr);
    if (superClass != NULL) {
        (*env)->ReleaseStringUTFChars(env, superClassName, superClassNameStr);
    }
    
    return (*env)->NewStringUTF(env, result);
}