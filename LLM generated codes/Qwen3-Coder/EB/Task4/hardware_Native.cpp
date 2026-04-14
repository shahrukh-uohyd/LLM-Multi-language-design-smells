#include <jni.h>
#include <iostream>
#include <chrono>
#include <thread>
#include <ctime>
#include <cstring>

#include "HardwareConnection.h"

// Helper functions to get field IDs
static jfieldID getIsConnectedFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    if (clazz == nullptr) return nullptr;
    
    jfieldID fieldID = env->GetFieldID(clazz, "isConnected", "Z");
    if (fieldID == nullptr) {
        std::cerr << "[NATIVE HARDWARE] Failed to find isConnected field" << std::endl;
    }
    return fieldID;
}

static jfieldID getIsResettingFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    if (clazz == nullptr) return nullptr;
    
    jfieldID fieldID = env->GetFieldID(clazz, "isResetting", "Ljava/util/concurrent/atomic/AtomicBoolean;");
    if (fieldID == nullptr) {
        std::cerr << "[NATIVE HARDWARE] Failed to find isResetting field" << std::endl;
    }
    return fieldID;
}

static jfieldID getConnectionIdFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    if (clazz == nullptr) return nullptr;
    
    jfieldID fieldID = env->GetFieldID(clazz, "connectionId", "Ljava/lang/String;");
    if (fieldID == nullptr) {
        std::cerr << "[NATIVE HARDWARE] Failed to find connectionId field" << std::endl;
    }
    return fieldID;
}

static jfieldID getLastActivityTimeFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    if (clazz == nullptr) return nullptr;
    
    jfieldID fieldID = env->GetFieldID(clazz, "lastActivityTime", "J");
    if (fieldID == nullptr) {
        std::cerr << "[NATIVE HARDWARE] Failed to find lastActivityTime field" << std::endl;
    }
    return fieldID;
}

static jfieldID getTimeoutDurationFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    if (clazz == nullptr) return nullptr;
    
    jfieldID fieldID = env->GetFieldID(clazz, "timeoutDurationMs", "I");
    if (fieldID == nullptr) {
        std::cerr << "[NATIVE HARDWARE] Failed to find timeoutDurationMs field" << std::endl;
    }
    return fieldID;
}

// Helper function to call AtomicBoolean.set() method
static bool setAtomicBoolean(JNIEnv* env, jobject atomicBoolean, jboolean value) {
    if (atomicBoolean == nullptr) return false;
    
    jclass atomicBooleanClass = env->GetObjectClass(atomicBoolean);
    if (atomicBooleanClass == nullptr) return false;
    
    jmethodID setMethod = env->GetMethodID(atomicBooleanClass, "set", "(Z)V");
    if (setMethod == nullptr) return false;
    
    env->CallVoidMethod(atomicBoolean, setMethod, value);
    
    // Check for exceptions
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }
    
    return true;
}

// Helper function to get AtomicBoolean value
static jboolean getAtomicBoolean(JNIEnv* env, jobject atomicBoolean) {
    if (atomicBoolean == nullptr) return JNI_FALSE;
    
    jclass atomicBooleanClass = env->GetObjectClass(atomicBoolean);
    if (atomicBooleanClass == nullptr) return JNI_FALSE;
    
    jmethodID getMethod = env->GetMethodID(atomicBooleanClass, "get", "()Z");
    if (getMethod == nullptr) return JNI_FALSE;
    
    jboolean result = env->CallBooleanMethod(atomicBoolean, getMethod);
    
    // Check for exceptions
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return JNI_FALSE;
    }
    
    return result;
}

/*
 * Class:     HardwareConnection
 * Method:    resetConnectionStatus
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_HardwareConnection_resetConnectionStatus
  (JNIEnv *env, jobject obj) {
    
    std::cout << "[NATIVE HARDWARE] Starting connection reset operation" << std::endl;

    // Get field IDs
    jfieldID isConnectedField = getIsConnectedFieldID(env, obj);
    jfieldID isResettingField = getIsResettingFieldID(env, obj);
    jfieldID connectionIdField = getConnectionIdFieldID(env, obj);

    if (isConnectedField == nullptr || isResettingField == nullptr) {
        std::cerr << "[NATIVE HARDWARE] Failed to get required field IDs" << std::endl;
        return;
    }

    // Get current connection ID for logging
    jstring connectionId = (jstring)env->GetObjectField(obj, connectionIdField);
    if (connectionId != nullptr) {
        const char* connIdStr = env->GetStringUTFChars(connectionId, nullptr);
        if (connIdStr != nullptr) {
            std::cout << "[NATIVE HARDWARE] Resetting connection: " << connIdStr << std::endl;
            env->ReleaseStringUTFChars(connectionId, connIdStr);
        }
        env->DeleteLocalRef(connectionId);
    }

    // Get the AtomicBoolean object for isResetting
    jobject isResettingObj = env->GetObjectField(obj, isResettingField);
    if (isResettingObj != nullptr) {
        // Set resetting flag to true
        if (setAtomicBoolean(env, isResettingObj, JNI_TRUE)) {
            std::cout << "[NATIVE HARDWARE] Set resetting flag to true" << std::endl;
        }
    }

    // Get current connection status
    jboolean currentStatus = env->GetBooleanField(obj, isConnectedField);
    std::cout << "[NATIVE HARDWARE] Current connection status: " << (currentStatus ? "CONNECTED" : "DISCONNECTED") << std::endl;

    // Perform any native-level reset operations here
    // For example, resetting hardware registers, clearing buffers, etc.
    std::this_thread::sleep_for(std::chrono::milliseconds(10)); // Simulate native reset time

    // Reset the connection status to default (false/disconnected)
    env->SetBooleanField(obj, isConnectedField, JNI_FALSE);
    std::cout << "[NATIVE HARDWARE] Connection status reset to DISCONNECTED" << std::endl;

    // Update last activity time to current time (as part of reset)
    jfieldID lastActivityTimeField = getLastActivityTimeFieldID(env, obj);
    if (lastActivityTimeField != nullptr) {
        jlong currentTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();
        env->SetLongField(obj, lastActivityTimeField, currentTime);
        std::cout << "[NATIVE HARDWARE] Last activity time updated to current time" << std::endl;
    }

    // Reset the isResetting flag
    if (isResettingObj != nullptr) {
        if (setAtomicBoolean(env, isResettingObj, JNI_FALSE)) {
            std::cout << "[NATIVE HARDWARE] Set resetting flag to false" << std::endl;
        }
        env->DeleteLocalRef(isResettingObj);
    }

    std::cout << "[NATIVE HARDWARE] Connection reset operation completed" << std::endl;
}

/*
 * Class:     HardwareConnection
 * Method:    checkAndHandleTimeout
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_HardwareConnection_checkAndHandleTimeout
  (JNIEnv *env, jobject obj) {
    
    std::cout << "[NATIVE HARDWARE] Checking for timeout condition" << std::endl;

    // Get required field IDs
    jfieldID lastActivityTimeField = getLastActivityTimeFieldID(env, obj);
    jfieldID timeoutDurationField = getTimeoutDurationFieldID(env, obj);
    jfieldID isResettingField = getIsResettingFieldID(env, obj);

    if (lastActivityTimeField == nullptr || timeoutDurationField == nullptr) {
        std::cerr << "[NATIVE HARDWARE] Failed to get time field IDs" << std::endl;
        return JNI_FALSE;
    }

    // Get current values
    jlong lastActivityTime = env->GetLongField(obj, lastActivityTimeField);
    jint timeoutDuration = env->GetIntField(obj, timeoutDurationField);

    // Get current time
    jlong currentTime = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();

    jlong timeSinceLastActivity = currentTime - lastActivityTime;
    bool isTimedOut = (timeSinceLastActivity > timeoutDuration);

    std::cout << "[NATIVE HARDWARE] Time since last activity: " << timeSinceLastActivity 
              << "ms, Timeout threshold: " << timeoutDuration << "ms" << std::endl;

    if (isTimedOut) {
        std::cout << "[NATIVE HARDWARE] TIMEOUT DETECTED! Time since activity: " 
                  << timeSinceLastActivity << "ms > " << timeoutDuration << "ms" << std::endl;

        // Check if already resetting to avoid race conditions
        jobject isResettingObj = env->GetObjectField(obj, isResettingField);
        bool isCurrentlyResetting = false;
        
        if (isResettingObj != nullptr) {
            isCurrentlyResetting = getAtomicBoolean(env, isResettingObj);
            env->DeleteLocalRef(isResettingObj);
        }

        if (!isCurrentlyResetting) {
            // Call the reset method to handle the timeout
            jclass clazz = env->GetObjectClass(obj);
            jmethodID resetMethod = env->GetMethodID(clazz, "resetConnectionStatus", "()V");
            
            if (resetMethod != nullptr) {
                env->CallVoidMethod(obj, resetMethod);
                
                // Check for exceptions
                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                    std::cerr << "[NATIVE HARDWARE] Exception occurred during timeout reset" << std::endl;
                    return JNI_FALSE;
                }
                
                std::cout << "[NATIVE HARDWARE] Timeout reset completed successfully" << std::endl;
                return JNI_TRUE;
            } else {
                std::cerr << "[NATIVE HARDWARE] Failed to find resetConnectionStatus method" << std::endl;
                return JNI_FALSE;
            }
        } else {
            std::cout << "[NATIVE HARDWARE] Connection is already being reset, skipping" << std::endl;
            return JNI_FALSE;
        }
    } else {
        std::cout << "[NATIVE HARDWARE] No timeout detected" << std::endl;
        return JNI_FALSE;
    }
}