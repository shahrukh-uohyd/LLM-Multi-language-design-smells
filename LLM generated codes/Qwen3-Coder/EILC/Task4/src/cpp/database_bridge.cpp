// database_bridge.cpp
#include "database_bridge.h"
#include <iostream>
#include <vector>
#include <chrono>
#include <thread>
#include <sqlite3.h>

// Mock database context structure
struct DatabaseContext {
    sqlite3* db;
    bool isConnected;
    
    DatabaseContext() : db(nullptr), isConnected(false) {
        // In real implementation, connect to actual database
        // For demo: simulate database operations
        isConnected = true;
    }
};

// Global database context (in real app, this would be managed properly)
static DatabaseContext g_dbContext;

// Helper function to convert jstring to std::string
std::string jstringToString(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return "";
    
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string str(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return str;
}

// Simulate database commit operation
bool simulateDatabaseCommit(const std::string& transactionId, const std::string& accountId,
                           double amount, const std::string& currency, long timestamp,
                           const std::string& description, const std::string& status) {
    // In real implementation, this would execute SQL INSERT with ACID properties
    // For demo: simulate with occasional failures to test error handling
    
    // Simulate some random failures for testing
    static int callCount = 0;
    callCount++;
    
    // Introduce occasional simulated failures
    if (callCount % 50 == 0) { // Every 50th call fails randomly
        return false;
    }
    
    // Simulate database delay
    std::this_thread::sleep_for(std::chrono::milliseconds(1)); // 1ms delay per commit
    
    // Log the operation
    std::cout << "Committed transaction: " << transactionId << " to account: " << accountId 
              << ", amount: " << amount << std::endl;
    
    return true; // Simulate successful commit
}

JNIEXPORT jboolean JNICALL Java_DatabaseBridge_commitToStorage
  (JNIEnv *env, jobject obj, jstring transactionId, jstring accountId, 
   jdouble amount, jstring currency, jlong timestamp, 
   jstring description, jstring status) {
    
    std::string transId = jstringToString(env, transactionId);
    std::string accId = jstringToString(env, accountId);
    std::string curr = jstringToString(env, currency);
    std::string desc = jstringToString(env, description);
    std::string stat = jstringToString(env, status);
    
    bool success = simulateDatabaseCommit(transId, accId, amount, curr, 
                                         static_cast<long>(timestamp), desc, stat);
    
    return success ? JNI_TRUE : JNI_FALSE;
}

// Helper function to create TransactionResult object
jobject createTransactionResult(JNIEnv* env, const std::string& transactionId, 
                               bool success, const std::string& error, long commitTime) {
    // Find the TransactionResult class
    jclass resultClass = env->FindClass("TransactionResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    // Find the constructor
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", 
        "(Ljava/lang/String;ZLjava/lang/String;J)V");
    if (constructor == nullptr) {
        return nullptr;
    }
    
    // Create string objects
    jstring jTransId = env->NewStringUTF(transactionId.c_str());
    jstring jError = error.empty() ? nullptr : env->NewStringUTF(error.c_str());
    
    // Create the TransactionResult object
    jobject result = env->NewObject(resultClass, constructor, jTransId, success, jError, commitTime);
    
    // Clean up local references
    env->DeleteLocalRef(jTransId);
    if (jError) env->DeleteLocalRef(jError);
    
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_DatabaseBridge_commitRecordsIndividually
  (JNIEnv *env, jobject obj, jobjectArray transactionIds, jobjectArray accountIds,
   jdoubleArray amounts, jobjectArray currencies, jlongArray timestamps,
   jobjectArray descriptions, jobjectArray statuses) {
    
    jsize length = env->GetArrayLength(transactionIds);
    if (length == 0) {
        // Return empty array
        jclass resultClass = env->FindClass("TransactionResult");
        return env->NewObjectArray(0, resultClass, nullptr);
    }
    
    // Create TransactionResult array
    jclass resultClass = env->FindClass("TransactionResult");
    jobjectArray resultArray = env->NewObjectArray(length, resultClass, nullptr);
    
    // Get arrays
    jstring* jTransIds = new jstring[length];
    jstring* jAccIds = new jstring[length];
    jdouble* jAmounts = new jdouble[length];
    jstring* jCurrencies = new jstring[length];
    jlong* jTimestamps = new jlong[length];
    jstring* jDescriptions = new jstring[length];
    jstring* jStatuses = new jstring[length];
    
    // Extract elements from arrays
    for (jsize i = 0; i < length; i++) {
        jTransIds[i] = (jstring)env->GetObjectArrayElement(transactionIds, i);
        jAccIds[i] = (jstring)env->GetObjectArrayElement(accountIds, i);
        jCurrencies[i] = (jstring)env->GetObjectArrayElement(currencies, i);
        jDescriptions[i] = (jstring)env->GetObjectArrayElement(descriptions, i);
        jStatuses[i] = (jstring)env->GetObjectArrayElement(statuses, i);
    }
    
    env->GetDoubleArrayRegion(amounts, 0, length, jAmounts);
    env->GetLongArrayRegion(timestamps, 0, length, jTimestamps);
    
    // Process each record individually (maintaining ACID compliance per record)
    for (jsize i = 0; i < length; i++) {
        auto startTime = std::chrono::high_resolution_clock::now();
        
        std::string transId = jstringToString(env, jTransIds[i]);
        std::string accId = jstringToString(env, jAccIds[i]);
        double amount = jAmounts[i];
        std::string currency = jstringToString(env, jCurrencies[i]);
        long timestamp = jTimestamps[i];
        std::string description = jstringToString(env, jDescriptions[i]);
        std::string status = jstringToString(env, jStatuses[i]);
        
        bool success = simulateDatabaseCommit(transId, accId, amount, currency, 
                                             timestamp, description, status);
        
        auto endTime = std::chrono::high_resolution_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
            endTime - startTime).count();
        
        std::string error = success ? "" : "Failed to commit record to native storage";
        jobject result = createTransactionResult(env, transId, success, error, duration);
        
        env->SetObjectArrayElement(resultArray, i, result);
        
        // Clean up
        env->DeleteLocalRef(result);
        env->DeleteLocalRef(jTransIds[i]);
        env->DeleteLocalRef(jAccIds[i]);
        env->DeleteLocalRef(jCurrencies[i]);
        env->DeleteLocalRef(jDescriptions[i]);
        env->DeleteLocalRef(jStatuses[i]);
    }
    
    // Clean up arrays
    delete[] jTransIds;
    delete[] jAccIds;
    delete[] jAmounts;
    delete[] jCurrencies;
    delete[] jTimestamps;
    delete[] jDescriptions;
    delete[] jStatuses;
    
    return resultArray;
}