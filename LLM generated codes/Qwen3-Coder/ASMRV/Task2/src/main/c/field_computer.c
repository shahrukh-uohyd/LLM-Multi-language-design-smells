#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdint.h>

// Helper function to calculate string hash
long calculateStringHash(const char* str) {
    long hash = 7;
    for (int i = 0; str[i] != '\0'; i++) {
        hash = hash * 31 + str[i];
    }
    return hash;
}

// Helper function to calculate array hash
long calculateArrayHash(jdoubleArray arr, JNIEnv *env) {
    jsize len = (*env)->GetArrayLength(env, arr);
    jdouble *elements = (*env)->GetDoubleArrayElements(env, arr, NULL);
    
    long hash = 7;
    for (int i = 0; i < len; i++) {
        // Simple hash calculation for double values
        long elementBits = *(long*)&elements[i];
        hash = hash * 31 + elementBits;
    }
    
    (*env)->ReleaseDoubleArrayElements(env, arr, elements, 0);
    return hash;
}

JNIEXPORT jlong JNICALL Java_com_example_FieldComputer_computeFieldValueInfo
(JNIEnv *env, jobject obj, jobject object, jstring fieldName, jint computationType) {
    // Convert jstring to C string
    const char *fieldNameStr = (*env)->GetStringUTFChars(env, fieldName, NULL);
    
    // Get the class of the object
    jclass cls = (*env)->GetObjectClass(env, object);
    
    // Find the field based on name
    jfieldID fieldID = (*env)->GetFieldID(env, cls, fieldNameStr, NULL);
    if (fieldID == NULL) {
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
        return -1; // Field not found
    }
    
    // Get field type signature
    jclass reflectFieldClass = (*env)->FindClass(env, "java/lang/reflect/Field");
    jmethodID getGenericTypeMethod = (*env)->GetMethodID(env, reflectFieldClass, "getGenericType", "()Ljava/lang/reflect/Type;");
    jmethodID getDeclaringClassMethod = (*env)->GetMethodID(env, reflectFieldClass, "getDeclaringClass", "()Ljava/lang/Class;");
    
    // For simplicity, we'll determine the field type by trying to access it with different methods
    jclass fieldClass = (*env)->FindClass(env, "java/lang/Class");
    jmethodID getNameMethod = (*env)->GetMethodID(env, fieldClass, "getName", "()Ljava/lang/String;");
    jobject fieldDeclaringClass = (*env)->GetObjectClass(env, object);
    jstring fieldTypeName = (jstring)(*env)->CallObjectMethod(env, fieldDeclaringClass, getNameMethod);
    const char* fieldTypeNameStr = (*env)->GetStringUTFChars(env, fieldTypeName, NULL);
    
    // Determine field type by getting its signature
    jclass classForName = (*env)->FindClass(env, fieldTypeNameStr);
    jmethodID getFieldMethod = (*env)->GetMethodID(env, classForName, "getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
    jobject fieldObj = (*env)->CallObjectMethod(env, classForName, getFieldMethod, fieldName);
    
    if (fieldObj == NULL) {
        (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
        (*env)->ReleaseStringUTFChars(env, fieldTypeName, fieldTypeNameStr);
        return -2; // Could not get field object
    }
    
    jclass fieldReflectClass = (*env)->GetObjectClass(env, fieldObj);
    jmethodID getTypeMethod = (*env)->GetMethodID(env, fieldReflectClass, "getType", "()Ljava/lang/Class;");
    jobject fieldType = (*env)->CallObjectMethod(env, fieldObj, getTypeMethod);
    jstring fieldSig = (jstring)(*env)->CallObjectMethod(env, fieldType, getNameMethod);
    const char* fieldSigStr = (*env)->GetStringUTFChars(env, fieldSig, NULL);
    
    jlong result = 0;
    
    // Perform computation based on field type and computation type
    if (strcmp(fieldSigStr, "java.lang.String") == 0) {
        // Handle String field
        jstring fieldValue = (jstring)(*env)->GetObjectField(env, object, fieldID);
        if (fieldValue != NULL) {
            const char *strValue = (*env)->GetStringUTFChars(env, fieldValue, NULL);
            
            switch (computationType) {
                case 0: // Sum of character codes
                    for (int i = 0; strValue[i] != '\0'; i++) {
                        result += (unsigned char)strValue[i]; // Use unsigned char to avoid sign issues
                    }
                    break;
                case 1: // Length of string
                    result = strlen(strValue);
                    break;
                case 2: // Hash of string
                    result = calculateStringHash(strValue);
                    break;
                default:
                    result = -1;
            }
            
            (*env)->ReleaseStringUTFChars(env, fieldValue, strValue);
        } else {
            result = -1; // Null string
        }
    }
    else if (strcmp(fieldSigStr, "[D") == 0 || strcmp(fieldSigStr, "[Ljava.lang.Double;") == 0) {
        // Handle double array field
        jdoubleArray fieldValue = (jdoubleArray)(*env)->GetObjectField(env, object, fieldID);
        if (fieldValue != NULL) {
            jsize length = (*env)->GetArrayLength(env, fieldValue);
            
            switch (computationType) {
                case 0: // Sum of array elements (as character codes)
                    {
                        jdouble *elements = (*env)->GetDoubleArrayElements(env, fieldValue, NULL);
                        for (int i = 0; i < length; i++) {
                            // Take the integer part as character code
                            result += (int)elements[i];
                        }
                        (*env)->ReleaseDoubleArrayElements(env, fieldValue, elements, 0);
                    }
                    break;
                case 1: // Length of array
                    result = length;
                    break;
                case 2: // Hash of array
                    result = calculateArrayHash(fieldValue, env);
                    break;
                default:
                    result = -1;
            }
        } else {
            result = -1; // Null array
        }
    }
    else if (strcmp(fieldSigStr, "int") == 0) {
        // Handle int field
        jint fieldValue = (*env)->GetIntField(env, object, fieldID);
        
        switch (computationType) {
            case 0: // Treat as character code sum (just the value itself)
                result = fieldValue;
                break;
            case 1: // Length representation (always 1 for primitive)
                result = 1;
                break;
            case 2: // Hash of value
                result = (jlong)fieldValue * 31;
                break;
            default:
                result = -1;
        }
    }
    else if (strcmp(fieldSigStr, "boolean") == 0) {
        // Handle boolean field
        jboolean fieldValue = (*env)->GetBooleanField(env, object, fieldID);
        
        switch (computationType) {
            case 0: // Character code representation
                result = fieldValue ? 1 : 0;
                break;
            case 1: // Length representation
                result = 1;
                break;
            case 2: // Hash of value
                result = fieldValue ? 1231L : 1237L;
                break;
            default:
                result = -1;
        }
    }
    else if (strcmp(fieldSigStr, "char") == 0) {
        // Handle char field
        jchar fieldValue = (*env)->GetCharField(env, object, fieldID);
        
        switch (computationType) {
            case 0: // Character code
                result = fieldValue;
                break;
            case 1: // Length representation
                result = 1;
                break;
            case 2: // Hash of value
                result = (jlong)fieldValue;
                break;
            default:
                result = -1;
        }
    }
    else if (strcmp(fieldSigStr, "long") == 0) {
        // Handle long field
        jlong fieldValue = (*env)->GetLongField(env, object, fieldID);
        
        switch (computationType) {
            case 0: // Use the value directly
                result = fieldValue;
                break;
            case 1: // Length representation
                result = 1;
                break;
            case 2: // Hash of value
                result = fieldValue ^ (fieldValue >> 32);
                break;
            default:
                result = -1;
        }
    }
    else {
        // For other types, try to handle as general object
        jobject fieldValue = (*env)->GetObjectField(env, object, fieldID);
        if (fieldValue != NULL) {
            switch (computationType) {
                case 0: // Try to get string representation and sum characters
                    {
                        jclass objectClass = (*env)->GetObjectClass(env, fieldValue);
                        jmethodID toStringMethod = (*env)->GetMethodID(env, objectClass, "toString", "()Ljava/lang/String;");
                        jstring strValue = (jstring)(*env)->CallObjectMethod(env, fieldValue, toStringMethod);
                        if (strValue != NULL) {
                            const char *str = (*env)->GetStringUTFChars(env, strValue, NULL);
                            for (int i = 0; str[i] != '\0'; i++) {
                                result += (unsigned char)str[i];
                            }
                            (*env)->ReleaseStringUTFChars(env, strValue, str);
                        }
                    }
                    break;
                case 1: // Length of string representation
                    {
                        jclass objectClass = (*env)->GetObjectClass(env, fieldValue);
                        jmethodID toStringMethod = (*env)->GetMethodID(env, objectClass, "toString", "()Ljava/lang/String;");
                        jstring strValue = (jstring)(*env)->CallObjectMethod(env, fieldValue, toStringMethod);
                        if (strValue != NULL) {
                            const char *str = (*env)->GetStringUTFChars(env, strValue, NULL);
                            result = strlen(str);
                            (*env)->ReleaseStringUTFChars(env, strValue, str);
                        }
                    }
                    break;
                case 2: // Hash of string representation
                    {
                        jclass objectClass = (*env)->GetObjectClass(env, fieldValue);
                        jmethodID toStringMethod = (*env)->GetMethodID(env, objectClass, "toString", "()Ljava/lang/String;");
                        jstring strValue = (jstring)(*env)->CallObjectMethod(env, fieldValue, toStringMethod);
                        if (strValue != NULL) {
                            const char *str = (*env)->GetStringUTFChars(env, strValue, NULL);
                            result = calculateStringHash(str);
                            (*env)->ReleaseStringUTFChars(env, strValue, str);
                        }
                    }
                    break;
                default:
                    result = -1;
            }
        } else {
            result = -1; // Null object
        }
    }
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, fieldName, fieldNameStr);
    (*env)->ReleaseStringUTFChars(env, fieldTypeName, fieldTypeNameStr);
    (*env)->ReleaseStringUTFChars(env, fieldSig, fieldSigStr);
    
    return result;
}