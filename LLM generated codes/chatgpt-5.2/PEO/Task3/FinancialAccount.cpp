#include <jni.h>
#include "FinancialAccount.h"

/*
 * double calculateRiskMetric()
 */
JNIEXPORT jdouble JNICALL
Java_FinancialAccount_calculateRiskMetric(JNIEnv* env,
                                          jobject obj) {

    // Get the FinancialAccount class
    jclass cls = env->GetObjectClass(obj);
    if (cls == nullptr) {
        return 0.0;
    }

    // Get field IDs for balance and creditLimit
    jfieldID balanceField =
        env->GetFieldID(cls, "balance", "D");
    jfieldID creditLimitField =
        env->GetFieldID(cls, "creditLimit", "D");

    if (balanceField == nullptr || creditLimitField == nullptr) {
        env->ExceptionClear();
        return 0.0;
    }

    // Read values
    jdouble balance =
        env->GetDoubleField(obj, balanceField);
    jdouble creditLimit =
        env->GetDoubleField(obj, creditLimitField);

    // Risk calculation (guard against division by zero)
    if (creditLimit <= 0.0) {
        return 0.0;
    }

    return balance / creditLimit;
}
