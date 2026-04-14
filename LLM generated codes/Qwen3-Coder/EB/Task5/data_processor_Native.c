#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#include "SensitiveDataManager.h"

// Cache the method ID for the private logging method to improve performance
static jmethodID g_logMethodID = NULL;

/**
 * Helper function to get the private logInternalDiagnostic method ID
 */
static jmethodID getPrivateLogMethodID(JNIEnv *env, jobject obj) {
    if (g_logMethodID != NULL) {
        return g_logMethodID;
    }
    
    jclass clazz = (*env)->GetObjectClass(env, obj);
    if (clazz == NULL) {
        fprintf(stderr, "[NATIVE DATA PROCESSOR] Failed to get object class\n");
        return NULL;
    }
    
    // Find the private method signature: (Ljava/lang/String;Ljava/lang/String;)V
    g_logMethodID = (*env)->GetMethodID(env, clazz, "logInternalDiagnostic", 
                                       "(Ljava/lang/String;Ljava/lang/String;)V");
    
    if (g_logMethodID == NULL) {
        fprintf(stderr, "[NATIVE DATA PROCESSOR] Failed to find private logInternalDiagnostic method\n");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return NULL;
    }
    
    return g_logMethodID;
}

/**
 * Helper function to invoke the private Java logging method
 */
static void callPrivateJavaLogger(JNIEnv *env, jobject obj, const char* message, const char* level) {
    jmethodID logMethodID = getPrivateLogMethodID(env, obj);
    if (logMethodID == NULL) {
        fprintf(stderr, "[NATIVE DATA PROCESSOR] Cannot call logger - method ID not found\n");
        return;
    }
    
    // Convert C strings to Java strings
    jstring jmessage = (*env)->NewStringUTF(env, message);
    jstring jlevel = (*env)->NewStringUTF(env, level);
    
    if (jmessage == NULL || jlevel == NULL) {
        fprintf(stderr, "[NATIVE DATA PROCESSOR] Failed to create Java strings for logging\n");
        if (jmessage) (*env)->DeleteLocalRef(env, jmessage);
        if (jlevel) (*env)->DeleteLocalRef(env, jlevel);
        return;
    }
    
    // Call the private method
    (*env)->CallVoidMethod(env, obj, logMethodID, jmessage, jlevel);
    
    // Check for exceptions
    if ((*env)->ExceptionCheck(env)) {
        fprintf(stderr, "[NATIVE DATA PROCESSOR] Exception occurred while calling private logger\n");
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    
    // Clean up local references
    (*env)->DeleteLocalRef(env, jmessage);
    (*env)->DeleteLocalRef(env, jlevel);
}

/**
 * Function to log native errors to the Java diagnostic logger
 */
static void logNativeError(JNIEnv *env, jobject obj, const char* errorMessage) {
    time_t rawtime;
    char timestamp[20];
    struct tm *timeinfo;
    
    time(&rawtime);
    timeinfo = localtime(&rawtime);
    strftime(timestamp, sizeof(timestamp), "%H:%M:%S", timeinfo);
    
    char fullMessage[512];
    snprintf(fullMessage, sizeof(fullMessage), 
             "[NATIVE] Error at %s: %s", timestamp, errorMessage);
    
    callPrivateJavaLogger(env, obj, fullMessage, "ERROR");
}

/**
 * Function to log native diagnostic information
 */
static void logNativeDiagnostic(JNIEnv *env, jobject obj, const char* message, const char* level) {
    char fullMessage[512];
    snprintf(fullMessage, sizeof(fullMessage), 
             "[NATIVE] %s", message);
    
    callPrivateJavaLogger(env, obj, fullMessage, level);
}

/*
 * Class:     SensitiveDataManager
 * Method:    processSensitiveData
 * Signature: ([B)Z
 */
JNIEXPORT jboolean JNICALL Java_SensitiveDataManager_processSensitiveData
  (JNIEnv *env, jobject obj, jbyteArray inputData) {
    
    printf("[NATIVE DATA PROCESSOR] Starting sensitive data processing\n");
    
    // Log start of processing
    logNativeDiagnostic(env, obj, "Beginning sensitive data processing", "INFO");
    
    if (inputData == NULL) {
        logNativeError(env, obj, "Input data array is null");
        return JNI_FALSE;
    }
    
    jsize inputLength = (*env)->GetArrayLength(env, inputData);
    if (inputLength <= 0) {
        logNativeError(env, obj, "Input data length is invalid");
        return JNI_FALSE;
    }
    
    // Get the input data
    jbyte* inputBytes = (*env)->GetByteArrayElements(env, inputData, NULL);
    if (inputBytes == NULL) {
        logNativeError(env, obj, "Failed to get input byte array elements");
        return JNI_FALSE;
    }
    
    // Simulate some processing that might fail
    int processingResult = 1; // 1 = success, 0 = failure
    
    // Simulate potential error conditions
    if (inputLength > 10000) { // Arbitrary limit for demo
        processingResult = 0;
        logNativeError(env, obj, "Input data exceeds maximum allowed size");
    } else if (inputLength == 0) {
        processingResult = 0;
        logNativeError(env, obj, "Input data is empty");
    } else {
        // Simulate successful processing
        logNativeDiagnostic(env, obj, "Processing completed successfully", "INFO");
        
        // Simulate some processing steps
        for (int i = 0; i < inputLength && i < 100; i++) {
            // Process each byte (dummy operation)
            inputBytes[i] = inputBytes[i] ^ 0xAA; // Simple XOR operation
        }
    }
    
    // Release the array elements
    (*env)->ReleaseByteArrayElements(env, inputData, inputBytes, JNI_ABORT);
    
    // Log completion
    if (processingResult) {
        logNativeDiagnostic(env, obj, "Data processing completed successfully", "INFO");
    } else {
        logNativeError(env, obj, "Data processing failed due to validation error");
    }
    
    printf("[NATIVE DATA PROCESSOR] Data processing completed (result: %s)\n", 
           processingResult ? "SUCCESS" : "FAILED");
    
    return processingResult ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     SensitiveDataManager
 * Method:    triggerNativeError
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_SensitiveDataManager_triggerNativeError
  (JNIEnv *env, jobject obj) {
    
    printf("[NATIVE DATA PROCESSOR] Triggering simulated native error\n");
    
    // Log the error trigger event
    logNativeDiagnostic(env, obj, "Simulated error scenario initiated", "WARNING");
    
    // Simulate various error conditions
    logNativeError(env, obj, "Simulated cryptographic operation failed");
    logNativeError(env, obj, "Simulated memory allocation error in native code");
    logNativeError(env, obj, "Simulated hardware interface timeout");
    
    // Log final status
    logNativeDiagnostic(env, obj, "Multiple simulated errors logged successfully", "INFO");
    
    printf("[NATIVE DATA PROCESSOR] Native error simulation completed\n");
    
    return JNI_TRUE;
}

/*
 * Class:     SensitiveDataManager
 * Method:    logDiagnosticMessage
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_SensitiveDataManager_logDiagnosticMessage
  (JNIEnv *env, jobject obj, jstring message, jstring level) {
    
    if (message == NULL) {
        logNativeError(env, obj, "logDiagnosticMessage called with null message");
        return;
    }
    
    const char* msgStr = (*env)->GetStringUTFChars(env, message, NULL);
    if (msgStr == NULL) {
        fprintf(stderr, "[NATIVE DATA PROCESSOR] Failed to convert message to UTF string\n");
        return;
    }
    
    const char* levelStr = "INFO"; // Default level
    if (level != NULL) {
        levelStr = (*env)->GetStringUTFChars(env, level, NULL);
        if (levelStr == NULL) {
            levelStr = "INFO";
        }
    }
    
    printf("[NATIVE DATA PROCESSOR] External log call: [%s] %s\n", levelStr, msgStr);
    
    // Forward to the private Java logger
    callPrivateJavaLogger(env, obj, msgStr, levelStr);
    
    // Clean up
    (*env)->ReleaseStringUTFChars(env, message, msgStr);
    if (level != NULL) {
        (*env)->ReleaseStringUTFChars(env, level, levelStr);
    }
}