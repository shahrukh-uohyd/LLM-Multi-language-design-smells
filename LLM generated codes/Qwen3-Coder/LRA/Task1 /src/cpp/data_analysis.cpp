// src/cpp/data_analysis.cpp
#include "../headers/data_analysis.h"
#include <jni.h>
#include <iostream>

// Global references for caching (for performance)
static jclass g_userRecordClass = nullptr;
static jmethodID g_getScoreMethod = nullptr;
static jmethodID g_getIdMethod = nullptr;

// Initialize global references
void initializeGlobalReferences(JNIEnv* env) {
    if (g_userRecordClass == nullptr) {
        jclass tempClass = env->FindClass("UserRecord");
        if (tempClass != nullptr) {
            g_userRecordClass = (jclass)env->NewGlobalRef(tempClass);
            env->DeleteLocalRef(tempClass);
            
            // Cache method IDs
            g_getScoreMethod = env->GetMethodID(g_userRecordClass, "getScore", "()I");
            g_getIdMethod = env->GetMethodID(g_userRecordClass, "getId", "()I");
        }
    }
}

// Main function to calculate sum of scores
JNIEXPORT jint JNICALL Java_DataAnalysisTool_processUserRecords
  (JNIEnv *env, jobject thiz, jobjectArray userRecords) {
    
    jsize arrayLength = env->GetArrayLength(userRecords);
    
    if (arrayLength == 0) {
        return 0;
    }
    
    // Initialize global references if not already done
    initializeGlobalReferences(env);
    
    if (g_userRecordClass == nullptr || g_getScoreMethod == nullptr) {
        return -1; // Error: Could not find class or method
    }
    
    jint totalSum = 0;
    
    // Iterate through the array
    for (jsize i = 0; i < arrayLength; i++) {
        jobject userRecordObj = env->GetObjectArrayElement(userRecords, i);
        if (userRecordObj != nullptr) {
            // Extract the score field
            jint score = env->CallIntMethod(userRecordObj, g_getScoreMethod);
            
            // Check for exceptions during method call
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                env->DeleteLocalRef(userRecordObj);
                continue;
            }
            
            totalSum += score;
            env->DeleteLocalRef(userRecordObj);
        }
    }
    
    return totalSum;
}

// Optimized version with cached references
JNIEXPORT jlong JNICALL Java_DataAnalysisTool_processUserRecordsOptimized
  (JNIEnv *env, jobject thiz, jobjectArray userRecords) {
    
    jsize arrayLength = env->GetArrayLength(userRecords);
    
    if (arrayLength == 0) {
        return 0;
    }
    
    // Initialize global references if not already done
    initializeGlobalReferences(env);
    
    if (g_userRecordClass == nullptr || g_getScoreMethod == nullptr) {
        return -1;
    }
    
    jlong totalSum = 0;
    
    // Process array elements efficiently
    for (jsize i = 0; i < arrayLength; i++) {
        jobject userRecordObj = env->GetObjectArrayElement(userRecords, i);
        if (userRecordObj != nullptr) {
            jint score = env->CallIntMethod(userRecordObj, g_getScoreMethod);
            
            if (!env->ExceptionCheck()) {
                totalSum += score;
            } else {
                env->ExceptionClear();
            }
            
            env->DeleteLocalRef(userRecordObj);
        }
    }
    
    return totalSum;
}

// Enhanced version that returns multiple statistics
JNIEXPORT jintArray JNICALL Java_DataAnalysisTool_processUserRecordsWithStats
  (JNIEnv *env, jobject thiz, jobjectArray userRecords) {
    
    jsize arrayLength = env->GetArrayLength(userRecords);
    
    if (arrayLength == 0) {
        // Return empty array
        jintArray result = env->NewIntArray(3);
        jint values[3] = {0, 0, 0};
        env->SetIntArrayRegion(result, 0, 3, values);
        return result;
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_userRecordClass == nullptr || g_getScoreMethod == nullptr || g_getIdMethod == nullptr) {
        return nullptr;
    }
    
    jint totalScore = 0;
    jint totalId = 0;
    jint validRecords = 0;
    
    for (jsize i = 0; i < arrayLength; i++) {
        jobject userRecordObj = env->GetObjectArrayElement(userRecords, i);
        if (userRecordObj != nullptr) {
            jint score = env->CallIntMethod(userRecordObj, g_getScoreMethod);
            jint id = env->CallIntMethod(userRecordObj, g_getIdMethod);
            
            if (!env->ExceptionCheck()) {
                totalScore += score;
                totalId += id;
                validRecords++;
            } else {
                env->ExceptionClear();
            }
            
            env->DeleteLocalRef(userRecordObj);
        }
    }
    
    // Create result array [totalScore, totalId, validRecordCount]
    jintArray result = env->NewIntArray(3);
    if (result != nullptr) {
        jint values[3] = {totalScore, totalId, validRecords};
        env->SetIntArrayRegion(result, 0, 3, values);
    }
    
    return result;
}

// Cleanup function (call this when JVM shuts down)
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_OK) {
        if (g_userRecordClass != nullptr) {
            env->DeleteGlobalRef(g_userRecordClass);
            g_userRecordClass = nullptr;
        }
    }
}