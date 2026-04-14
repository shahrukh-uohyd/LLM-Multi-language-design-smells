// sensor_logger.cpp
#include "sensor_logger.h"
#include <sqlite3.h>
#include <iostream>
#include <vector>
#include <chrono>
#include <thread>
#include <atomic>
#include <sstream>

struct SensorData {
    jlong timestamp;
    std::string sensorId;
    jdouble value;
    std::string dataType;
};

class NativeDatabase {
private:
    sqlite3* db;
    std::mutex db_mutex;
    std::atomic<int> pending_sync_count{0};
    bool initialized;

public:
    NativeDatabase() : db(nullptr), initialized(false) {}
    
    ~NativeDatabase() {
        if (db) {
            sqlite3_close(db);
        }
    }
    
    bool initialize(const std::string& dbPath) {
        std::lock_guard<std::mutex> lock(db_mutex);
        
        int rc = sqlite3_open(dbPath.c_str(), &db);
        if (rc != SQLITE_OK) {
            std::cerr << "Cannot open database: " << sqlite3_errmsg(db) << std::endl;
            return false;
        }
        
        // Create table if it doesn't exist
        const char* create_table_sql = R"(
            CREATE TABLE IF NOT EXISTS sensor_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                sensor_id TEXT NOT NULL,
                value REAL NOT NULL,
                data_type TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        )";
        
        rc = sqlite3_exec(db, create_table_sql, nullptr, nullptr, nullptr);
        if (rc != SQLITE_OK) {
            std::cerr << "SQL error: " << sqlite3_errmsg(db) << std::endl;
            return false;
        }
        
        initialized = true;
        return true;
    }
    
    bool batchInsert(const std::vector<SensorData>& logs) {
        if (!initialized || logs.empty()) {
            return false;
        }
        
        std::lock_guard<std::mutex> lock(db_mutex);
        
        // Begin transaction for better performance
        const char* begin_transaction = "BEGIN TRANSACTION;";
        sqlite3_exec(db, begin_transaction, nullptr, nullptr, nullptr);
        
        const char* insert_sql = R"(
            INSERT INTO sensor_logs (timestamp, sensor_id, value, data_type)
            VALUES (?, ?, ?, ?);
        )";
        
        bool success = true;
        for (const auto& log : logs) {
            sqlite3_stmt* stmt;
            int rc = sqlite3_prepare_v2(db, insert_sql, -1, &stmt, nullptr);
            
            if (rc == SQLITE_OK) {
                sqlite3_bind_int64(stmt, 1, log.timestamp);
                sqlite3_bind_text(stmt, 2, log.sensorId.c_str(), -1, SQLITE_STATIC);
                sqlite3_bind_double(stmt, 3, log.value);
                sqlite3_bind_text(stmt, 4, log.dataType.c_str(), -1, SQLITE_STATIC);
                
                rc = sqlite3_step(stmt);
                if (rc != SQLITE_DONE) {
                    std::cerr << "Insert failed: " << sqlite3_errmsg(db) << std::endl;
                    success = false;
                }
            } else {
                std::cerr << "Prepare statement failed: " << sqlite3_errmsg(db) << std::endl;
                success = false;
            }
            
            sqlite3_finalize(stmt);
            
            if (!success) break;
        }
        
        // Commit transaction
        const char* commit_transaction = "COMMIT;";
        if (success) {
            sqlite3_exec(db, commit_transaction, nullptr, nullptr, nullptr);
        } else {
            const char* rollback_transaction = "ROLLBACK;";
            sqlite3_exec(db, rollback_transaction, nullptr, nullptr, nullptr);
        }
        
        pending_sync_count -= logs.size();
        if (pending_sync_count < 0) pending_sync_count = 0;
        
        return success;
    }
    
    void incrementPendingCount(int count) {
        pending_sync_count += count;
    }
    
    int getPendingCount() const {
        return pending_sync_count.load();
    }
    
    bool close() {
        std::lock_guard<std::mutex> lock(db_mutex);
        if (db) {
            sqlite3_close(db);
            db = nullptr;
            initialized = false;
        }
        return true;
    }
};

static NativeDatabase g_database;

JNIEXPORT jboolean JNICALL Java_NativeDatabaseSync_initializeDatabase
  (JNIEnv *env, jobject obj, jstring dbPath) {
    
    const char* path = env->GetStringUTFChars(dbPath, 0);
    bool result = g_database.initialize(std::string(path));
    env->ReleaseStringUTFChars(dbPath, path);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_NativeDatabaseSync_syncLogs
  (JNIEnv *env, jobject obj, jobjectArray logs) {
    
    jsize length = env->GetArrayLength(logs);
    if (length == 0) {
        return JNI_TRUE;
    }
    
    std::vector<SensorData> sensorLogs;
    sensorLogs.reserve(length);
    
    // Extract data from Java objects
    for (jsize i = 0; i < length; i++) {
        jobject logObj = env->GetObjectArrayElement(logs, i);
        if (logObj == nullptr) continue;
        
        SensorData logData;
        
        // Get timestamp
        jclass logClass = env->GetObjectClass(logObj);
        jmethodID getTimestamp = env->GetMethodID(logClass, "getTimestamp", "()J");
        logData.timestamp = env->CallLongMethod(logObj, getTimestamp);
        
        // Get sensorId
        jmethodID getSensorId = env->GetMethodID(logClass, "getSensorId", "()Ljava/lang/String;");
        jstring sensorId = (jstring)env->CallObjectMethod(logObj, getSensorId);
        const char* sensorIdStr = env->GetStringUTFChars(sensorId, 0);
        logData.sensorId = std::string(sensorIdStr);
        env->ReleaseStringUTFChars(sensorId, sensorIdStr);
        
        // Get value
        jmethodID getValue = env->GetMethodID(logClass, "getValue", "()D");
        logData.value = env->CallDoubleMethod(logObj, getValue);
        
        // Get dataType
        jmethodID getDataType = env->GetMethodID(logClass, "getDataType", "()Ljava/lang/String;");
        jstring dataType = (jstring)env->CallObjectMethod(logObj, getDataType);
        const char* dataTypeStr = env->GetStringUTFChars(dataType, 0);
        logData.dataType = std::string(dataTypeStr);
        env->ReleaseStringUTFChars(dataType, dataTypeStr);
        
        sensorLogs.push_back(logData);
        env->DeleteLocalRef(logObj);
    }
    
    // Increment pending count before sync
    g_database.incrementPendingCount(static_cast<int>(sensorLogs.size()));
    
    // Perform batch insert
    bool result = g_database.batchInsert(sensorLogs);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_NativeDatabaseSync_closeDatabase
  (JNIEnv *env, jobject obj) {
    
    return g_database.close() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_NativeDatabaseSync_getPendingSyncCount
  (JNIEnv *env, jobject obj) {
    
    return static_cast<jint>(g_database.getPendingCount());
}