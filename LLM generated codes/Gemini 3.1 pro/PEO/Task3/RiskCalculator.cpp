#include <jni.h>

extern "C"
JNIEXPORT jdouble JNICALL
Java_FinancialAccount_calculateRiskMetric(JNIEnv *env, jobject thisObj) {
    
    // 1. Get the class of the object (thisObj is the FinancialAccount instance)
    jclass accountClass = env->GetObjectClass(thisObj);
    if (accountClass == nullptr) {
        return -1.0; // Error: Class not found
    }

    // 2. Get the Field IDs for balance and creditLimit.
    // "D" is the JNI signature for the primitive 'double' type.
    jfieldID balanceField = env->GetFieldID(accountClass, "balance", "D");
    jfieldID creditLimitField = env->GetFieldID(accountClass, "creditLimit", "D");

    // Check if fields were successfully found
    if (balanceField == nullptr || creditLimitField == nullptr) {
        return -1.0; 
    }

    // 3. Extract the double values from the object
    jdouble balance = env->GetDoubleField(thisObj, balanceField);
    jdouble creditLimit = env->GetDoubleField(thisObj, creditLimitField);

    // 4. Calculate the risk metric (e.g., credit utilization ratio)
    if (creditLimit <= 0.0) {
        // Handle division by zero or invalid credit limit
        return (balance > 0.0) ? 100.0 : 0.0; 
    }

    // Example risk metric: ratio of balance to credit limit as a percentage
    jdouble riskMetric = (balance / creditLimit) * 100.0;

    return riskMetric;
}