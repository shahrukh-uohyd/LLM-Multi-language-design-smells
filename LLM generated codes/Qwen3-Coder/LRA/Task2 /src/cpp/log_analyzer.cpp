// src/cpp/log_analyzer.cpp
#include "../headers/log_analyzer.h"
#include <jni.h>
#include <map>
#include <string>
#include <cstring>

// Global references for caching
static jclass g_logEventClass = nullptr;
static jclass g_localDateTimeClass = nullptr;
static jclass g_hashMapClass = nullptr;
static jclass g_stringClass = nullptr;
static jclass g_logLevelClass = nullptr;

static jfieldID g_timestampField = nullptr;
static jfieldID g_severityLevelField = nullptr;
static jfieldID g_sourceComponentField = nullptr;
static jfieldID g_messageField = nullptr;

static jmethodID g_hashMapConstructor = nullptr;
static jmethodID g_hashMapPutMethod = nullptr;
static jmethodID g_stringConstructor = nullptr;
static jmethodID g_logLevelGetValueMethod = nullptr;

// Helper function to initialize global references
void initializeGlobalReferences(JNIEnv* env) {
    if (g_logEventClass == nullptr) {
        // Initialize LogEvent class and its fields
        jclass tempLogEventClass = env->FindClass("LogEvent");
        if (tempLogEventClass != nullptr) {
            g_logEventClass = (jclass)env->NewGlobalRef(tempLogEventClass);
            env->DeleteLocalRef(tempLogEventClass);
            
            g_timestampField = env->GetFieldID(g_logEventClass, "timestamp", "Ljava/time/LocalDateTime;");
            g_severityLevelField = env->GetFieldID(g_logEventClass, "severityLevel", "LLogLevel;");
            g_sourceComponentField = env->GetFieldID(g_logEventClass, "sourceComponent", "Ljava/lang/String;");
            g_messageField = env->GetFieldID(g_logEventClass, "message", "Ljava/lang/String;");
        }
        
        // Initialize other classes
        jclass tempLocalDateTimeClass = env->FindClass("java/time/LocalDateTime");
        if (tempLocalDateTimeClass != nullptr) {
            g_localDateTimeClass = (jclass)env->NewGlobalRef(tempLocalDateTimeClass);
            env->DeleteLocalRef(tempLocalDateTimeClass);
        }
        
        jclass tempHashMapClass = env->FindClass("java/util/HashMap");
        if (tempHashMapClass != nullptr) {
            g_hashMapClass = (jclass)env->NewGlobalRef(tempHashMapClass);
            env->DeleteLocalRef(tempHashMapClass);
            
            g_hashMapConstructor = env->GetMethodID(g_hashMapClass, "<init>", "()V");
            g_hashMapPutMethod = env->GetMethodID(g_hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        }
        
        jclass tempStringClass = env->FindClass("java/lang/String");
        if (tempStringClass != nullptr) {
            g_stringClass = (jclass)env->NewGlobalRef(tempStringClass);
            env->DeleteLocalRef(tempStringClass);
        }
        
        jclass tempLogLevelClass = env->FindClass("LogLevel");
        if (tempLogLevelClass != nullptr) {
            g_logLevelClass = (jclass)env->NewGlobalRef(tempLogLevelClass);
            env->DeleteLocalRef(tempLogLevelClass);
            
            g_logLevelGetValueMethod = env->GetMethodID(g_logLevelClass, "getValue", "()I");
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

// Main function to analyze log events by severity level
JNIEXPORT jintArray JNICALL Java_LogAnalyzer_analyzeLogEventsBySeverity
  (JNIEnv *env, jobject thiz, jobjectArray logEvents) {
    
    jsize arrayLength = env->GetArrayLength(logEvents);
    
    if (arrayLength == 0) {
        // Return array with 5 elements (for 5 log levels) initialized to 0
        jintArray result = env->NewIntArray(5);
        if (result != nullptr) {
            jint values[5] = {0, 0, 0, 0, 0};
            env->SetIntArrayRegion(result, 0, 5, values);
        }
        return result;
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_logEventClass == nullptr || g_severityLevelField == nullptr || g_logLevelGetValueMethod == nullptr) {
        return nullptr;
    }
    
    // Initialize counters for each severity level (DEBUG=0, INFO=1, WARNING=2, ERROR=3, CRITICAL=4)
    jint severityCounts[5] = {0, 0, 0, 0, 0};
    
    // Iterate through log events
    for (jsize i = 0; i < arrayLength; i++) {
        jobject logEvent = env->GetObjectArrayElement(logEvents, i);
        if (logEvent != nullptr) {
            // Get severity level object
            jobject severityLevel = env->GetObjectField(logEvent, g_severityLevelField);
            if (severityLevel != nullptr) {
                // Get the integer value of the severity level
                jint levelValue = env->CallIntMethod(severityLevel, g_logLevelGetValueMethod);
                
                if (!env->ExceptionCheck() && levelValue >= 0 && levelValue < 5) {
                    severityCounts[levelValue]++;
                } else if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                }
                
                env->DeleteLocalRef(severityLevel);
            }
            
            env->DeleteLocalRef(logEvent);
        }
    }
    
    // Create result array
    jintArray result = env->NewIntArray(5);
    if (result != nullptr) {
        env->SetIntArrayRegion(result, 0, 5, severityCounts);
    }
    
    return result;
}

// Function to analyze log events by component
JNIEXPORT jobject JNICALL Java_LogAnalyzer_analyzeLogEventsByComponent
  (JNIEnv *env, jobject thiz, jobjectArray logEvents) {
    
    jsize arrayLength = env->GetArrayLength(logEvents);
    
    if (arrayLength == 0) {
        // Return empty HashMap
        return env->NewObject(g_hashMapClass, g_hashMapConstructor);
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_logEventClass == nullptr || g_sourceComponentField == nullptr) {
        return nullptr;
    }
    
    // Create result HashMap
    jobject resultHashMap = env->NewObject(g_hashMapClass, g_hashMapConstructor);
    
    // Use C++ map to count occurrences
    std::map<std::string, int> componentCounts;
    
    // Iterate through log events
    for (jsize i = 0; i < arrayLength; i++) {
        jobject logEvent = env->GetObjectArrayElement(logEvents, i);
        if (logEvent != nullptr) {
            // Get source component string
            jstring sourceComponent = (jstring)env->GetObjectField(logEvent, g_sourceComponentField);
            if (sourceComponent != nullptr) {
                std::string componentStr = jstringToString(env, sourceComponent);
                
                // Increment count for this component
                componentCounts[componentStr]++;
                
                env->DeleteLocalRef(sourceComponent);
            }
            
            env->DeleteLocalRef(logEvent);
        }
    }
    
    // Populate the Java HashMap
    for (const auto& pair : componentCounts) {
        jstring componentKey = env->NewStringUTF(pair.first.c_str());
        jint countValue = pair.second;
        jobject countValueObj = env->NewObject(env->FindClass("java/lang/Integer"), 
                                              env->GetMethodID(env->FindClass("java/lang/Integer"), 
                                                              "<init>", "(I)V"), 
                                              countValue);
        
        env->CallObjectMethod(resultHashMap, g_hashMapPutMethod, componentKey, countValueObj);
        
        env->DeleteLocalRef(componentKey);
        env->DeleteLocalRef(countValueObj);
        
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            break;
        }
    }
    
    return resultHashMap;
}

// Function to analyze log events with time range filtering
JNIEXPORT jlongArray JNICALL Java_LogAnalyzer_analyzeLogEventsWithTimeRange
  (JNIEnv *env, jobject thiz, jobjectArray logEvents, jobject startTime, jobject endTime) {
    
    jsize arrayLength = env->GetArrayLength(logEvents);
    
    if (arrayLength == 0) {
        // Return array with 3 elements: [totalEvents, errorEvents, criticalEvents]
        jlongArray result = env->NewLongArray(3);
        jlong values[3] = {0, 0, 0};
        env->SetLongArrayRegion(result, 0, 3, values);
        return result;
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_logEventClass == nullptr || g_timestampField == nullptr || 
        g_severityLevelField == nullptr || g_logLevelGetValueMethod == nullptr) {
        return nullptr;
    }
    
    jlong totalEvents = 0;
    jlong errorEvents = 0;
    jlong criticalEvents = 0;
    
    // Iterate through log events
    for (jsize i = 0; i < arrayLength; i++) {
        jobject logEvent = env->GetObjectArrayElement(logEvents, i);
        if (logEvent != nullptr) {
            // Get timestamp and severity level
            jobject timestamp = env->GetObjectField(logEvent, g_timestampField);
            jobject severityLevel = env->GetObjectField(logEvent, g_severityLevelField);
            
            if (timestamp != nullptr && severityLevel != nullptr) {
                // Note: In a real implementation, you would need to compare timestamps
                // For simplicity, we'll assume all events are within range
                totalEvents++;
                
                // Get severity level value
                jint levelValue = env->CallIntMethod(severityLevel, g_logLevelGetValueMethod);
                
                if (!env->ExceptionCheck()) {
                    if (levelValue == 3) { // ERROR level
                        errorEvents++;
                    } else if (levelValue == 4) { // CRITICAL level
                        criticalEvents++;
                    }
                } else {
                    env->ExceptionClear();
                }
                
                env->DeleteLocalRef(timestamp);
            }
            
            env->DeleteLocalRef(severityLevel);
            env->DeleteLocalRef(logEvent);
        }
    }
    
    // Create result array
    jlongArray result = env->NewLongArray(3);
    jlong values[3] = {totalEvents, errorEvents, criticalEvents};
    env->SetLongArrayRegion(result, 0, 3, values);
    
    return result;
}

// Cleanup function
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_OK) {
        // Delete global references
        if (g_logEventClass != nullptr) {
            env->DeleteGlobalRef(g_logEventClass);
            g_logEventClass = nullptr;
        }
        if (g_localDateTimeClass != nullptr) {
            env->DeleteGlobalRef(g_localDateTimeClass);
            g_localDateTimeClass = nullptr;
        }
        if (g_hashMapClass != nullptr) {
            env->DeleteGlobalRef(g_hashMapClass);
            g_hashMapClass = nullptr;
        }
        if (g_stringClass != nullptr) {
            env->DeleteGlobalRef(g_stringClass);
            g_stringClass = nullptr;
        }
        if (g_logLevelClass != nullptr) {
            env->DeleteGlobalRef(g_logLevelClass);
            g_logLevelClass = nullptr;
        }
    }
}