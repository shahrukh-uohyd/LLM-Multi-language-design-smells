#include <jni.h>
#include <iostream>
#include <string>
#include <thread>
#include <chrono>
#include "Device.h" // Generated header file

// Helper function to get field IDs (cached for performance)
static jfieldID getDeviceIdFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "deviceId", "Ljava/lang/String;");
}

static jfieldID getTemperatureFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "temperature", "I");
}

static jfieldID getIsRunningFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "isRunning", "Z");
}

static jfieldID getVoltageFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "voltage", "D");
}

static jfieldID getStateFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "state", "LDeviceState;");
}

static jfieldID getDeviceStateValueFieldID(JNIEnv* env, jobject stateObj) {
    jclass stateClazz = env->GetObjectClass(stateObj);
    return env->GetFieldID(stateClazz, "value", "I");
}

// Helper function to get DeviceState enum by value
static jobject getDeviceStateByValue(JNIEnv* env, int value) {
    jclass stateClazz = env->FindClass("DeviceState");
    if (stateClazz == nullptr) return nullptr;

    jmethodID valueOfMethod = env->GetStaticMethodID(stateClazz, "values", "()[LDeviceState;");
    if (valueOfMethod == nullptr) return nullptr;

    jobjectArray states = (jobjectArray)env->CallStaticObjectMethod(stateClazz, valueOfMethod);
    if (states == nullptr) return nullptr;

    jsize arrayLength = env->GetArrayLength(states);
    for (jsize i = 0; i < arrayLength; i++) {
        jobject state = env->GetObjectArrayElement(states, i);
        jfieldID valueField = getDeviceStateValueFieldID(env, state);
        jint stateValue = env->GetIntField(state, valueField);
        
        if (stateValue == value) {
            return state;
        }
        env->DeleteLocalRef(state);
    }
    
    return nullptr;
}

/*
 * Class:     Device
 * Method:    performNativeOperation
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_Device_performNativeOperation
  (JNIEnv *env, jobject obj) {
    
    // Get device ID from Java object
    jfieldID deviceIdField = getDeviceIdFieldID(env, obj);
    jstring deviceId = (jstring)env->GetObjectField(obj, deviceIdField);
    
    const char* deviceIdStr = env->GetStringUTFChars(deviceId, nullptr);
    std::cout << "[NATIVE] Performing operation for device: " << deviceIdStr << std::endl;
    
    // Simulate some native processing
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    
    // Get current temperature
    jfieldID tempField = getTemperatureFieldID(env, obj);
    jint currentTemp = env->GetIntField(obj, tempField);
    
    // Update temperature based on some calculation
    jint newTemp = currentTemp + 5;
    env->SetIntField(obj, tempField, newTemp);
    
    std::cout << "[NATIVE] Updated temperature to: " << newTemp << "°C" << std::endl;
    
    env->ReleaseStringUTFChars(deviceId, deviceIdStr);
    env->DeleteLocalRef(deviceId);
}

/*
 * Class:     Device
 * Method:    getCalculatedValue
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_Device_getCalculatedValue
  (JNIEnv *env, jobject obj) {
    
    // Get device fields
    jfieldID tempField = getTemperatureFieldID(env, obj);
    jfieldID voltageField = getVoltageFieldID(env, obj);
    jfieldID runningField = getIsRunningFieldID(env, obj);
    
    jint temp = env->GetIntField(obj, tempField);
    jdouble voltage = env->GetDoubleField(obj, voltageField);
    jboolean isRunning = env->GetBooleanField(obj, runningField);
    
    // Perform calculation using Java object data
    jint calculated = (jint)(temp * voltage * (isRunning ? 1 : 0.5));
    
    std::cout << "[NATIVE] Calculation: " << temp << " * " << voltage 
              << " * " << (isRunning ? 1 : 0.5) << " = " << calculated << std::endl;
    
    return calculated;
}

/*
 * Class:     Device
 * Method:    validateDevice
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_Device_validateDevice
  (JNIEnv *env, jobject obj) {
    
    jfieldID tempField = getTemperatureFieldID(env, obj);
    jfieldID voltageField = getVoltageFieldID(env, obj);
    jfieldID idField = getDeviceIdFieldID(env, obj);
    
    jint temp = env->GetIntField(obj, tempField);
    jdouble voltage = env->GetDoubleField(obj, voltageField);
    jstring id = (jstring)env->GetObjectField(obj, idField);
    
    const char* idStr = env->GetStringUTFChars(id, nullptr);
    
    bool isValid = true;
    if (temp < -40 || temp > 85) {
        std::cout << "[NATIVE] Validation failed: Temperature out of range: " << temp << std::endl;
        isValid = false;
    }
    
    if (voltage < 9.0 || voltage > 15.0) {
        std::cout << "[NATIVE] Validation failed: Voltage out of range: " << voltage << std::endl;
        isValid = false;
    }
    
    if (idStr == nullptr || strlen(idStr) == 0) {
        std::cout << "[NATIVE] Validation failed: Invalid device ID" << std::endl;
        isValid = false;
    }
    
    env->ReleaseStringUTFChars(id, idStr);
    env->DeleteLocalRef(id);
    
    std::cout << "[NATIVE] Device validation result: " << (isValid ? "PASS" : "FAIL") << std::endl;
    
    return isValid ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     Device
 * Method:    getNativeDeviceInfo
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_Device_getNativeDeviceInfo
  (JNIEnv *env, jobject obj) {
    
    jfieldID idField = getDeviceIdFieldID(env, obj);
    jfieldID tempField = getTemperatureFieldID(env, obj);
    jfieldID voltageField = getVoltageFieldID(env, obj);
    jfieldID runningField = getIsRunningFieldID(env, obj);
    jfieldID stateField = getStateFieldID(env, obj);
    
    jstring id = (jstring)env->GetObjectField(obj, idField);
    jint temp = env->GetIntField(obj, tempField);
    jdouble voltage = env->GetDoubleField(obj, voltageField);
    jboolean isRunning = env->GetBooleanField(obj, runningField);
    
    // Get state name
    jobject stateObj = env->GetObjectField(obj, stateField);
    jclass stateClazz = env->GetObjectClass(stateObj);
    jmethodID nameMethod = env->GetMethodID(stateClazz, "name", "()Ljava/lang/String;");
    jstring stateName = (jstring)env->CallObjectMethod(stateObj, nameMethod);
    
    const char* idStr = env->GetStringUTFChars(id, nullptr);
    const char* stateStr = env->GetStringUTFChars(stateName, nullptr);
    
    std::string info = std::string("[NATIVE INFO] Device: ") + idStr + 
                      ", Temp: " + std::to_string(temp) + "°C" +
                      ", Voltage: " + std::to_string(voltage) + "V" +
                      ", Running: " + (isRunning ? "YES" : "NO") +
                      ", State: " + stateStr;
    
    env->ReleaseStringUTFChars(id, idStr);
    env->ReleaseStringUTFChars(stateName, stateStr);
    env->DeleteLocalRef(id);
    env->DeleteLocalRef(stateName);
    env->DeleteLocalRef(stateObj);
    
    return env->NewStringUTF(info.c_str());
}

/*
 * Class:     Device
 * Method:    updateFromNative
 * Signature: (ID)V
 */
JNIEXPORT void JNICALL Java_Device_updateFromNative
  (JNIEnv *env, jobject obj, jint newTemp, jdouble newVoltage) {
    
    jfieldID tempField = getTemperatureFieldID(env, obj);
    jfieldID voltageField = getVoltageFieldID(env, obj);
    jfieldID stateField = getStateFieldID(env, obj);
    
    // Update temperature and voltage
    env->SetIntField(obj, tempField, newTemp);
    env->SetDoubleField(obj, voltageField, newVoltage);
    
    // Update state to RUNNING if conditions are met
    if (newTemp > 30 && newVoltage > 10.0) {
        jobject runningState = getDeviceStateByValue(env, 1); // RUNNING = 1
        if (runningState != nullptr) {
            env->SetObjectField(obj, stateField, runningState);
            env->DeleteLocalRef(runningState);
        }
    }
    
    std::cout << "[NATIVE] Updated device: temp=" << newTemp << "°C, voltage=" << newVoltage << "V" << std::endl;
}