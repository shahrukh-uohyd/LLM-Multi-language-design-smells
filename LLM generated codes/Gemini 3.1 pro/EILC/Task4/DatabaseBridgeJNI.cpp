#include <jni.h>
#include <string>

// Hypothetical native database storage class
class NativeStorage {
public:
    static bool commitRow(const std::string& id, double amount, long long timestamp) {
        // Implementation for your specific C++ DB
        // BEGIN TRANSACTION;
        // INSERT INTO transactions ...
        // COMMIT;
        return true; 
    }
};

extern "C"
JNIEXPORT jboolean JNICALL
Java_DatabaseBridge_commitToStorage(JNIEnv *env, jobject thiz, jobject recordObj) {
    if (recordObj == nullptr) {
        return JNI_FALSE;
    }

    // 1. Get the class of the object
    jclass recordClass = env->GetObjectClass(recordObj);

    // 2. Get the Field IDs
    jfieldID fidId        = env->GetFieldID(recordClass, "transactionId", "Ljava/lang/String;");
    jfieldID fidAmount    = env->GetFieldID(recordClass, "amount", "D");
    jfieldID fidTimestamp = env->GetFieldID(recordClass, "timestamp", "J");

    // 3. Extract the data
    jstring jId = (jstring) env->GetObjectField(recordObj, fidId);
    jdouble amount = env->GetDoubleField(recordObj, fidAmount);
    jlong timestamp = env->GetLongField(recordObj, fidTimestamp);

    // Convert Java String to C++ std::string
    const char* idChars = env->GetStringUTFChars(jId, nullptr);
    std::string transactionId(idChars);
    env->ReleaseStringUTFChars(jId, idChars);

    // 4. Clean up the local references
    env->DeleteLocalRef(jId);
    env->DeleteLocalRef(recordClass);

    // 5. Commit individually to the native database to guarantee row-level ACID
    bool isCommitted = NativeStorage::commitRow(transactionId, amount, timestamp);

    return isCommitted ? JNI_TRUE : JNI_FALSE;
}