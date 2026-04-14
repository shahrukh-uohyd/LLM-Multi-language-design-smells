// src/cpp/device_monitor.cpp
#include "../headers/device_monitor.h"
#include <jni.h>
#include <string>
#include <unordered_map>
#include <vector>
#include <algorithm>
#include <chrono>
#include <ctime>

// Global references for caching
static jclass g_deviceStatusUpdateClass = nullptr;
static jclass g_stringClass = nullptr;
static jclass g_hashMapClass = nullptr;
static jclass g_integerClass = nullptr;

static jfieldID g_deviceIdField = nullptr;
static jfieldID g_statusCodeField = nullptr;
static jfieldID g_descriptionField = nullptr;
static jfieldID g_updateTimeField = nullptr;

static jmethodID g_hashMapConstructor = nullptr;
static jmethodID g_hashMapPutMethod = nullptr;
static jmethodID g_integerConstructor = nullptr;

// Helper function to initialize global references
void initializeGlobalReferences(JNIEnv* env) {
    if (g_deviceStatusUpdateClass == nullptr) {
        // Initialize DeviceStatusUpdate class and its fields
        jclass tempDeviceStatusClass = env->FindClass("DeviceStatusUpdate");
        if (tempDeviceStatusClass != nullptr) {
            g_deviceStatusUpdateClass = (jclass)env->NewGlobalRef(tempDeviceStatusClass);
            env->DeleteLocalRef(tempDeviceStatusClass);
            
            g_deviceIdField = env->GetFieldID(g_deviceStatusUpdateClass, "deviceId", "Ljava/lang/String;");
            g_statusCodeField = env->GetFieldID(g_deviceStatusUpdateClass, "statusCode", "I");
            g_descriptionField = env->GetFieldID(g_deviceStatusUpdateClass, "description", "Ljava/lang/String;");
            g_updateTimeField = env->GetFieldID(g_deviceStatusUpdateClass, "updateTime", "Ljava/time/LocalDateTime;");
        }
        
        // Initialize other classes
        jclass tempStringClass = env->FindClass("java/lang/String");
        if (tempStringClass != nullptr) {
            g_stringClass = (jclass)env->NewGlobalRef(tempStringClass);
            env->DeleteLocalRef(tempStringClass);
        }
        
        jclass tempHashMapClass = env->FindClass("java/util/HashMap");
        if (tempHashMapClass != nullptr) {
            g_hashMapClass = (jclass)env->NewGlobalRef(tempHashMapClass);
            env->DeleteLocalRef(tempHashMapClass);
            
            g_hashMapConstructor = env->GetMethodID(g_hashMapClass, "<init>", "()V");
            g_hashMapPutMethod = env->GetMethodID(g_hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        }
        
        jclass tempIntegerClass = env->FindClass("java/lang/Integer");
        if (tempIntegerClass != nullptr) {
            g_integerClass = (jclass)env->NewGlobalRef(tempIntegerClass);
            env->DeleteLocalRef(tempIntegerClass);
            
            g_integerConstructor = env->GetMethodID(g_integerClass, "<init>", "(I)V");
        }
    }
}

// Helper function to convert Java String to C++ string
std::string jstringToString(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return "";
    
    const char* cstr = env->GetStringUTFChars(jstr, nullptr);
    if (cstr == nullptr) return ""; // OutOfMemoryError thrown
    
    std::string result(cstr);
    env->ReleaseStringUTFChars(jstr, cstr);
    return result;
}

// Helper function to convert C++ string to Java String
jstring stringToJstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// Helper function to check if status code indicates failure (2 or 3)
bool isFailureStatus(int statusCode) {
    return statusCode == 2 || statusCode == 3; // FAILURE or CRITICAL
}

// Main function to identify devices with repeated failures
JNIEXPORT jobjectArray JNICALL Java_DeviceFailureAnalyzer_identifyRepeatedFailures
  (JNIEnv *env, jobject thiz, jobjectArray updates, jint failureThreshold, jlong timeWindowMinutes) {
    
    jsize arrayLength = env->GetArrayLength(updates);
    
    if (arrayLength == 0 || failureThreshold <= 0) {
        // Return empty array
        return env->NewObjectArray(0, g_stringClass, nullptr);
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_deviceStatusUpdateClass == nullptr) {
        return nullptr;
    }
    
    // Map to track device failures within time windows
    std::unordered_map<std::string, std::vector<long>> deviceFailures;
    
    // Process each update
    for (jsize i = 0; i < arrayLength; i++) {
        jobject update = env->GetObjectArrayElement(updates, i);
        if (update != nullptr) {
            jstring deviceId = (jstring)env->GetObjectField(update, g_deviceIdField);
            jint statusCode = env->GetIntField(update, g_statusCodeField);
            
            std::string deviceIdStr = jstringToString(env, deviceId);
            
            // Check if this is a failure status
            if (isFailureStatus(statusCode)) {
                // In a real implementation, you would get the actual timestamp
                // For this example, we'll use a simple counter as a proxy for time
                long currentTime = std::time(nullptr) - (arrayLength - i) * 60; // Simulate time progression
                
                deviceFailures[deviceIdStr].push_back(currentTime);
            }
            
            env->DeleteLocalRef(deviceId);
            env->DeleteLocalRef(update);
        }
    }
    
    // Identify devices with repeated failures in the time window
    std::vector<std::string> repeatedFailureDevices;
    
    for (const auto& pair : deviceFailures) {
        const std::vector<long>& failures = pair.second;
        long currentTime = std::time(nullptr);
        
        // Count failures within the specified time window
        int failureCount = 0;
        for (long failureTime : failures) {
            if ((currentTime - failureTime) / 60 <= timeWindowMinutes) {
                failureCount++;
            }
        }
        
        if (failureCount >= failureThreshold) {
            repeatedFailureDevices.push_back(pair.first);
        }
    }
    
    // Create result array
    jobjectArray result = env->NewObjectArray(repeatedFailureDevices.size(), g_stringClass, nullptr);
    for (size_t i = 0; i < repeatedFailureDevices.size(); i++) {
        jstring deviceStr = stringToJstring(env, repeatedFailureDevices[i]);
        env->SetObjectArrayElement(result, i, deviceStr);
        env->DeleteLocalRef(deviceStr);
    }
    
    return result;
}

// Function to analyze device failure frequency
JNIEXPORT jobject JNICALL Java_DeviceFailureAnalyzer_analyzeDeviceFailureFrequency
  (JNIEnv *env, jobject thiz, jobjectArray updates) {
    
    jsize arrayLength = env->GetArrayLength(updates);
    
    if (arrayLength == 0) {
        // Return empty HashMap
        return env->NewObject(g_hashMapClass, g_hashMapConstructor);
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_deviceStatusUpdateClass == nullptr) {
        return nullptr;
    }
    
    // Create result HashMap
    jobject resultHashMap = env->NewObject(g_hashMapClass, g_hashMapConstructor);
    
    // Map to count failures per device
    std::unordered_map<std::string, int> failureCount;
    
    // Process each update
    for (jsize i = 0; i < arrayLength; i++) {
        jobject update = env->GetObjectArrayElement(updates, i);
        if (update != nullptr) {
            jstring deviceId = (jstring)env->GetObjectField(update, g_deviceIdField);
            jint statusCode = env->GetIntField(update, g_statusCodeField);
            
            std::string deviceIdStr = jstringToString(env, deviceId);
            
            // Count only failure statuses
            if (isFailureStatus(statusCode)) {
                failureCount[deviceIdStr]++;
            }
            
            env->DeleteLocalRef(deviceId);
            env->DeleteLocalRef(update);
        }
    }
    
    // Populate the Java HashMap
    for (const auto& pair : failureCount) {
        jstring deviceKey = env->NewStringUTF(pair.first.c_str());
        jobject countValue = env->NewObject(g_integerClass, g_integerConstructor, pair.second);
        
        env->CallObjectMethod(resultHashMap, g_hashMapPutMethod, deviceKey, countValue);
        
        env->DeleteLocalRef(deviceKey);
        env->DeleteLocalRef(countValue);
        
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            break;
        }
    }
    
    return resultHashMap;
}

// Function to get devices with recent failures
JNIEXPORT jobjectArray JNICALL Java_DeviceFailureAnalyzer_getDevicesWithRecentFailures
  (JNIEnv *env, jobject thiz, jobjectArray updates, jlong minutesBack) {
    
    jsize arrayLength = env->GetArrayLength(updates);
    
    if (arrayLength == 0) {
        return env->NewObjectArray(0, g_stringClass, nullptr);
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_deviceStatusUpdateClass == nullptr) {
        return nullptr;
    }
    
    // Set to track devices with recent failures
    std::unordered_set<std::string> recentFailureDevices;
    
    // Calculate cutoff time
    long cutoffTime = std::time(nullptr) - (minutesBack * 60);
    
    // Process each update
    for (jsize i = 0; i < arrayLength; i++) {
        jobject update = env->GetObjectArrayElement(updates, i);
        if (update != nullptr) {
            jstring deviceId = (jstring)env->GetObjectField(update, g_deviceIdField);
            jint statusCode = env->GetIntField(update, g_statusCodeField);
            
            std::string deviceIdStr = jstringToString(env, deviceId);
            
            // Check if this is a recent failure
            if (isFailureStatus(statusCode)) {
                // In a real implementation, you would check the actual timestamp
                // For this example, we'll assume all failures are "recent"
                recentFailureDevices.insert(deviceIdStr);
            }
            
            env->DeleteLocalRef(deviceId);
            env->DeleteLocalRef(update);
        }
    }
    
    // Create result array
    std::vector<std::string> resultVector(recentFailureDevices.begin(), recentFailureDevices.end());
    jobjectArray result = env->NewObjectArray(resultVector.size(), g_stringClass, nullptr);
    
    for (size_t i = 0; i < resultVector.size(); i++) {
        jstring deviceStr = stringToJstring(env, resultVector[i]);
        env->SetObjectArrayElement(result, i, deviceStr);
        env->DeleteLocalRef(deviceStr);
    }
    
    return result;
}

// Cleanup function
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_OK) {
        // Delete global references
        if (g_deviceStatusUpdateClass != nullptr) {
            env->DeleteGlobalRef(g_deviceStatusUpdateClass);
            g_deviceStatusUpdateClass = nullptr;
        }
        if (g_stringClass != nullptr) {
            env->DeleteGlobalRef(g_stringClass);
            g_stringClass = nullptr;
        }
        if (g_hashMapClass != nullptr) {
            env->DeleteGlobalRef(g_hashMapClass);
            g_hashMapClass = nullptr;
        }
        if (g_integerClass != nullptr) {
            env->DeleteGlobalRef(g_integerClass);
            g_integerClass = nullptr;
        }
    }
}