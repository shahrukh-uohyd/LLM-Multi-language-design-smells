#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>

JNIEXPORT jobjectArray JNICALL
Java_com_example_TextTransformer_toUpperCaseBatch(JNIEnv *env, jobject thiz, jobjectArray inputArray) {
    
    // 1. Check for null input
    if (inputArray == NULL) {
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, inputArray);

    // 2. Look up the Java String class
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass == NULL) {
        return NULL; // JVM throws NoClassDefFoundError
    }

    // 3. Create the result array to hold the new strings
    jobjectArray resultArray = (*env)->NewObjectArray(env, length, stringClass, NULL);
    if (resultArray == NULL) {
        (*env)->DeleteLocalRef(env, stringClass);
        return NULL; // JVM throws OutOfMemoryError
    }

    // 4. Iterate through the large array
    for (jsize i = 0; i < length; ++i) {
        // Fetch the string at the current index
        jstring inStr = (jstring) (*env)->GetObjectArrayElement(env, inputArray, i);

        if (inStr != NULL) {
            // Convert Java string to a C-style string (UTF-8)
            const char *cStr = (*env)->GetStringUTFChars(env, inStr, NULL);
            
            if (cStr != NULL) {
                size_t strLen = strlen(cStr);
                
                // Allocate a mutable buffer for the uppercase conversion
                char *upperStr = (char *)malloc(strLen + 1);
                
                if (upperStr != NULL) {
                    // Convert characters to uppercase
                    // (unsigned char) cast prevents undefined behavior with negative char values
                    for (size_t j = 0; j <= strLen; ++j) {
                        upperStr[j] = toupper((unsigned char)cStr[j]);
                    }

                    // Create a new Java String from the modified C buffer
                    jstring outStr = (*env)->NewStringUTF(env, upperStr);

                    // Place the new String into the result array
                    if (outStr != NULL) {
                        (*env)->SetObjectArrayElement(env, resultArray, i, outStr);
                        
                        // CRITICAL: Delete local ref to the newly created string
                        (*env)->DeleteLocalRef(env, outStr);
                    }

                    // Free the malloc'd buffer
                    free(upperStr);
                }
                
                // CRITICAL: Release the memory pinned by GetStringUTFChars
                (*env)->ReleaseStringUTFChars(env, inStr, cStr);
            }
            
            // CRITICAL: Delete local ref to the input string to prevent table overflow
            (*env)->DeleteLocalRef(env, inStr);
        }
    }

    // Clean up the class reference
    (*env)->DeleteLocalRef(env, stringClass);

    return resultArray;
}