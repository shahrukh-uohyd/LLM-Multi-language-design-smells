#include <jni.h>
#include <vector>
#include <iostream>

// Represents the native C++ equivalent of the Java SensorLog
struct NativeSensorLog {
    long long timestamp;
    int sensorId;
    float value;
};

// Hypothetical native database class
class NativeDatabase {
public:
    static bool bulkInsert(const std::vector<NativeSensorLog>& logs) {
        // Implementation for your specific C++ DB (e.g., SQLite batch insert)
        // BEGIN TRANSACTION;
        // INSERT INTO sensor_logs ...
        // COMMIT;
        std::cout << "Successfully saved " << logs.size() << " logs to native DB." << std::endl;
        return true; 
    }
};

extern "C"
JNIEXPORT jboolean JNICALL
Java_DataLogger_syncLogsToDatabase(JNIEnv *env, jobject thiz, jobjectArray logsArray) {
    if (logsArray == nullptr) {
        return JNI_FALSE;
    }

    jsize logCount = env->GetArrayLength(logsArray);
    if (logCount == 0) {
        return JNI_TRUE; // Nothing to sync
    }

    // 1. Find the Java class and cache Field IDs 
    // Note: Use your actual package path (e.g., "com/yourdomain/SensorLog")
    jclass sensorLogClass = env->FindClass("SensorLog"); 
    if (sensorLogClass == nullptr) {
        return JNI_FALSE; 
    }

    jfieldID fidTimestamp = env->GetFieldID(sensorLogClass, "timestamp", "J");
    jfieldID fidSensorId  = env->GetFieldID(sensorLogClass, "sensorId", "I");
    jfieldID fidValue     = env->GetFieldID(sensorLogClass, "value", "F");

    if (fidTimestamp == nullptr || fidSensorId == nullptr || fidValue == nullptr) {
        env->DeleteLocalRef(sensorLogClass);
        return JNI_FALSE;
    }

    // 2. Pre-allocate vector to prevent re-allocations
    std::vector<NativeSensorLog> nativeLogs;
    nativeLogs.reserve(logCount);

    // 3. Iterate over the 30 objects and extract data
    for (jsize i = 0; i < logCount; ++i) {
        jobject logObj = env->GetObjectArrayElement(logsArray, i);
        if (logObj != nullptr) {
            NativeSensorLog nLog;
            nLog.timestamp = env->GetLongField(logObj, fidTimestamp);
            nLog.sensorId  = env->GetIntField(logObj, fidSensorId);
            nLog.value     = env->GetFloatField(logObj, fidValue);

            nativeLogs.push_back(nLog);

            // CRITICAL: Delete local reference to prevent JNI local reference table overflow.
            // Even though the default limit is 512 and we only have 30 objects, 
            // cleaning up in a loop is a strict JNI best practice.
            env->DeleteLocalRef(logObj); 
        }
    }

    env->DeleteLocalRef(sensorLogClass);

    // 4. Send the extracted C++ structs to the native database for persistent storage
    bool isSaved = NativeDatabase::bulkInsert(nativeLogs);

    return isSaved ? JNI_TRUE : JNI_FALSE;
}