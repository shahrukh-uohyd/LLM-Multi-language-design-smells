// src/cpp/sensor_inspector.cpp
#include "../headers/sensor_inspector.h"
#include <jni.h>
#include <vector>
#include <string>
#include <algorithm>
#include <sstream>
#include <iomanip>

// Global references for caching
static jclass g_sensorReadingClass = nullptr;
static jclass g_sensorThresholdClass = nullptr;
static jclass g_stringClass = nullptr;

static jfieldID g_sensorIdField = nullptr;
static jfieldID g_measurementValueField = nullptr;
static jfieldID g_unitField = nullptr;
static jfieldID g_collectionTimeField = nullptr;

static jfieldID g_sensorTypeField = nullptr;
static jfieldID g_minThresholdField = nullptr;
static jfieldID g_maxThresholdField = nullptr;

// Helper function to initialize global references
void initializeGlobalReferences(JNIEnv* env) {
    if (g_sensorReadingClass == nullptr) {
        // Initialize SensorReading class and its fields
        jclass tempSensorReadingClass = env->FindClass("SensorReading");
        if (tempSensorReadingClass != nullptr) {
            g_sensorReadingClass = (jclass)env->NewGlobalRef(tempSensorReadingClass);
            env->DeleteLocalRef(tempSensorReadingClass);
            
            g_sensorIdField = env->GetFieldID(g_sensorReadingClass, "sensorId", "Ljava/lang/String;");
            g_measurementValueField = env->GetFieldID(g_sensorReadingClass, "measurementValue", "D");
            g_unitField = env->GetFieldID(g_sensorReadingClass, "unit", "Ljava/lang/String;");
            g_collectionTimeField = env->GetFieldID(g_sensorReadingClass, "collectionTime", "Ljava/time/LocalDateTime;");
        }
        
        // Initialize SensorThreshold class and its fields
        jclass tempSensorThresholdClass = env->FindClass("SensorThreshold");
        if (tempSensorThresholdClass != nullptr) {
            g_sensorThresholdClass = (jclass)env->NewGlobalRef(tempSensorThresholdClass);
            env->DeleteLocalRef(tempSensorThresholdClass);
            
            g_sensorTypeField = env->GetFieldID(g_sensorThresholdClass, "sensorType", "Ljava/lang/String;");
            g_minThresholdField = env->GetFieldID(g_sensorThresholdClass, "minThreshold", "D");
            g_maxThresholdField = env->GetFieldID(g_sensorThresholdClass, "maxThreshold", "D");
        }
        
        // Initialize String class
        jclass tempStringClass = env->FindClass("java/lang/String");
        if (tempStringClass != nullptr) {
            g_stringClass = (jclass)env->NewGlobalRef(tempStringClass);
            env->DeleteLocalRef(tempStringClass);
        }
    }
}

// Helper function to convert Java String to C++ string
std::string jstringToString(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return "";
    
    const char* cstr = env->GetStringUTFChars(jstr, nullptr);
    std::string result(cstr);
    env->ReleaseStringUTFChars(jstr, cstr);
    return result;
}

// Helper function to convert C++ string to Java String
jstring stringToJstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// Main function to find sensor readings that exceed thresholds
JNIEXPORT jobjectArray JNICALL Java_SensorInspector_findExceedingReadings
  (JNIEnv *env, jobject thiz, jobjectArray sensorReadings, jobjectArray sensorThresholds) {
    
    jsize readingCount = env->GetArrayLength(sensorReadings);
    jsize thresholdCount = env->GetArrayLength(sensorThresholds);
    
    if (readingCount == 0 || thresholdCount == 0) {
        // Return empty array
        jobjectArray result = env->NewObjectArray(0, g_stringClass, nullptr);
        return result;
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_sensorReadingClass == nullptr || g_sensorThresholdClass == nullptr) {
        return nullptr;
    }
    
    // Vector to store exceeding readings
    std::vector<std::string> exceedingReadings;
    
    // Process each reading against all thresholds
    for (jsize i = 0; i < readingCount; i++) {
        jobject reading = env->GetObjectArrayElement(sensorReadings, i);
        if (reading != nullptr) {
            // Extract reading properties
            jstring sensorId = (jstring)env->GetObjectField(reading, g_sensorIdField);
            jdouble measurementValue = env->GetDoubleField(reading, g_measurementValueField);
            jstring unit = (jstring)env->GetObjectField(reading, g_unitField);
            
            std::string sensorIdStr = jstringToString(env, sensorId);
            std::string unitStr = jstringToString(env, unit);
            
            bool exceedsThreshold = false;
            
            // Check against all thresholds
            for (jsize j = 0; j < thresholdCount; j++) {
                jobject threshold = env->GetObjectArrayElement(sensorThresholds, j);
                if (threshold != nullptr) {
                    jstring thresholdType = (jstring)env->GetObjectField(threshold, g_sensorTypeField);
                    jdouble minThreshold = env->GetDoubleField(threshold, g_minThresholdField);
                    jdouble maxThreshold = env->GetDoubleField(threshold, g_maxThresholdField);
                    
                    std::string thresholdTypeStr = jstringToString(env, thresholdType);
                    
                    // Check if sensor type matches and value is out of bounds
                    if (sensorIdStr.find(thresholdTypeStr) != std::string::npos) {
                        if (measurementValue < minThreshold || measurementValue > maxThreshold) {
                            exceedsThreshold = true;
                            break;
                        }
                    }
                    
                    env->DeleteLocalRef(thresholdType);
                    env->DeleteLocalRef(threshold);
                }
            }
            
            if (exceedsThreshold) {
                std::stringstream ss;
                ss << std::fixed << std::setprecision(2);
                ss << "EXCEEDING:" << sensorIdStr << "=" << measurementValue << " " << unitStr;
                exceedingReadings.push_back(ss.str());
            }
            
            env->DeleteLocalRef(sensorId);
            env->DeleteLocalRef(unit);
            env->DeleteLocalRef(reading);
        }
    }
    
    // Create result array
    jobjectArray result = env->NewObjectArray(exceedingReadings.size(), g_stringClass, nullptr);
    for (size_t i = 0; i < exceedingReadings.size(); i++) {
        jstring readingStr = stringToJstring(env, exceedingReadings[i]);
        env->SetObjectArrayElement(result, i, readingStr);
        env->DeleteLocalRef(readingStr);
    }
    
    return result;
}

// Function to analyze min/max values across all readings
JNIEXPORT jdoubleArray JNICALL Java_SensorInspector_analyzeMinMaxValues
  (JNIEnv *env, jobject thiz, jobjectArray sensorReadings) {
    
    jsize readingCount = env->GetArrayLength(sensorReadings);
    
    if (readingCount == 0) {
        // Return array with NaN values
        jdoubleArray result = env->NewDoubleArray(2);
        jdouble values[2] = {NAN, NAN};
        env->SetDoubleArrayRegion(result, 0, 2, values);
        return result;
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_sensorReadingClass == nullptr) {
        return nullptr;
    }
    
    double minValue = INFINITY;
    double maxValue = -INFINITY;
    
    // Iterate through readings to find min/max
    for (jsize i = 0; i < readingCount; i++) {
        jobject reading = env->GetObjectArrayElement(sensorReadings, i);
        if (reading != nullptr) {
            jdouble measurementValue = env->GetDoubleField(reading, g_measurementValueField);
            
            if (measurementValue < minValue) {
                minValue = measurementValue;
            }
            if (measurementValue > maxValue) {
                maxValue = measurementValue;
            }
            
            env->DeleteLocalRef(reading);
        }
    }
    
    // Create result array
    jdoubleArray result = env->NewDoubleArray(2);
    jdouble values[2] = {minValue, maxValue};
    env->SetDoubleArrayRegion(result, 0, 2, values);
    
    return result;
}

// Function to identify critical readings above a threshold
JNIEXPORT jobjectArray JNICALL Java_SensorInspector_identifyCriticalReadings
  (JNIEnv *env, jobject thiz, jobjectArray sensorReadings, jdouble criticalThreshold, jstring unitFilter) {
    
    jsize readingCount = env->GetArrayLength(sensorReadings);
    
    if (readingCount == 0) {
        // Return empty array
        jobjectArray result = env->NewObjectArray(0, g_stringClass, nullptr);
        return result;
    }
    
    std::string filterUnit = jstringToString(env, unitFilter);
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_sensorReadingClass == nullptr) {
        return nullptr;
    }
    
    // Vector to store critical readings
    std::vector<std::string> criticalReadings;
    
    // Process each reading
    for (jsize i = 0; i < readingCount; i++) {
        jobject reading = env->GetObjectArrayElement(sensorReadings, i);
        if (reading != nullptr) {
            jstring sensorId = (jstring)env->GetObjectField(reading, g_sensorIdField);
            jdouble measurementValue = env->GetDoubleField(reading, g_measurementValueField);
            jstring unit = (jstring)env->GetObjectField(reading, g_unitField);
            
            std::string unitStr = jstringToString(env, unit);
            
            // Check if value exceeds critical threshold and unit matches filter
            if (measurementValue > criticalThreshold && 
                (filterUnit.empty() || filterUnit == unitStr)) {
                
                std::stringstream ss;
                ss << std::fixed << std::setprecision(2);
                ss << "CRITICAL:" << jstringToString(env, sensorId) 
                   << "=" << measurementValue << " " << unitStr;
                criticalReadings.push_back(ss.str());
            }
            
            env->DeleteLocalRef(sensorId);
            env->DeleteLocalRef(unit);
            env->DeleteLocalRef(reading);
        }
    }
    
    // Create result array
    jobjectArray result = env->NewObjectArray(criticalReadings.size(), g_stringClass, nullptr);
    for (size_t i = 0; i < criticalReadings.size(); i++) {
        jstring readingStr = stringToJstring(env, criticalReadings[i]);
        env->SetObjectArrayElement(result, i, readingStr);
        env->DeleteLocalRef(readingStr);
    }
    
    return result;
}

// Cleanup function
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_OK) {
        // Delete global references
        if (g_sensorReadingClass != nullptr) {
            env->DeleteGlobalRef(g_sensorReadingClass);
            g_sensorReadingClass = nullptr;
        }
        if (g_sensorThresholdClass != nullptr) {
            env->DeleteGlobalRef(g_sensorThresholdClass);
            g_sensorThresholdClass = nullptr;
        }
        if (g_stringClass != nullptr) {
            env->DeleteGlobalRef(g_stringClass);
            g_stringClass = nullptr;
        }
    }
}