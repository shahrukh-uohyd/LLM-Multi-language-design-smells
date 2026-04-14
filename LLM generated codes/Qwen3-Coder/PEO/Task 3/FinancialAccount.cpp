#include <jni.h>
#include "FinancialAccount.h"
#include <cmath>
#include <algorithm>

// Risk calculation strategy enum
enum RiskStrategy {
    UTILIZATION_BASED = 0,
    COMPLEX_MODEL = 1,
    SIMPLE_RATIO = 2
};

JNIEXPORT jdouble JNICALL Java_FinancialAccount_calculateRiskMetric
  (JNIEnv *env, jobject thisObj) {
    
    jclass accountClass = nullptr;
    jfieldID balanceFieldID = nullptr;
    jfieldID creditLimitFieldID = nullptr;
    jdouble balance = 0.0;
    jdouble creditLimit = 0.0;
    jdouble riskMetric = 0.0;
    
    // Validate input
    if (env == nullptr || thisObj == nullptr) {
        return 0.0;
    }
    
    try {
        // Get the FinancialAccount class
        accountClass = env->GetObjectClass(thisObj);
        if (accountClass == nullptr) {
            env->ExceptionDescribe();
            return 0.0;
        }
        
        // Find the field IDs for balance and creditLimit
        balanceFieldID = env->GetFieldID(accountClass, "balance", "D");
        creditLimitFieldID = env->GetFieldID(accountClass, "creditLimit", "D");
        
        if (balanceFieldID == nullptr || creditLimitFieldID == nullptr) {
            env->ExceptionDescribe();
            return 0.0;
        }
        
        // Get the values of balance and creditLimit fields
        balance = env->GetDoubleField(thisObj, balanceFieldID);
        creditLimit = env->GetDoubleField(thisObj, creditLimitFieldID);
        
        // Check for potential exceptions during GetDoubleField
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            return 0.0;
        }
        
        // Validate inputs are not NaN
        if (std::isnan(balance) || std::isnan(creditLimit)) {
            return 0.0;
        }
        
        // Strategy 0: Utilization-based risk (standard approach)
        if (creditLimit > 0.0) {
            riskMetric = balance / creditLimit;
            
            // Apply non-linear transformation for more sensitivity near limits
            if (riskMetric >= 1.0) {
                // Exponential increase when over limit
                riskMetric = 1.0 + log(riskMetric + 1.0);
            } else if (riskMetric > 0.8) {
                // Steeper increase when approaching limit
                riskMetric = 0.8 + (riskMetric - 0.8) * 2.0;
            }
        } else if (creditLimit == 0.0 && balance > 0.0) {
            // No credit limit but has positive balance
            riskMetric = 5.0;  // High risk
        } else if (balance < 0.0) {
            // Negative balance (credit to customer) - low risk
            riskMetric = 0.0;
        }
        
        // Clamp result to [0, 10] range
        riskMetric = std::max(0.0, std::min(10.0, riskMetric));
        
    } catch (...) {
        return 0.0;
    }
    
    return riskMetric;
}

JNIEXPORT jdouble JNICALL Java_FinancialAccount_calculateRiskMetricWithWeight
  (JNIEnv *env, jobject thisObj, jdouble balanceWeight, jdouble creditLimitWeight) {
    
    jclass accountClass = env->GetObjectClass(thisObj);
    jfieldID balanceFieldID = env->GetFieldID(accountClass, "balance", "D");
    jfieldID creditLimitFieldID = env->GetFieldID(accountClass, "creditLimit", "D");
    
    if (balanceFieldID == nullptr || creditLimitFieldID == nullptr) {
        env->ExceptionDescribe();
        return 0.0;
    }
    
    jdouble balance = env->GetDoubleField(thisObj, balanceFieldID);
    jdouble creditLimit = env->GetDoubleField(thisObj, creditLimitFieldID);
    
    // Validate inputs
    if (std::isnan(balance) || std::isnan(creditLimit) || 
        std::isnan(balanceWeight) || std::isnan(creditLimitWeight)) {
        return 0.0;
    }
    
    // Validate weights are non-negative
    if (balanceWeight < 0.0 || creditLimitWeight < 0.0) {
        return 0.0;
    }
    
    jdouble riskMetric = 0.0;
    
    if (creditLimit > 0.0) {
        // Calculate utilization ratio
        jdouble utilization = balance / creditLimit;
        
        // Weighted risk calculation
        riskMetric = (utilization * balanceWeight) + 
                    ((creditLimit > 0) ? (creditLimitWeight / creditLimit) : 0.0);
    } else if (creditLimit == 0.0 && balance > 0.0) {
        // No credit limit but has balance - use only balance weight
        riskMetric = balance * balanceWeight;
    }
    
    // Apply exponential scaling for very high risk scenarios
    if (riskMetric > 5.0) {
        riskMetric = 5.0 + log(riskMetric - 4.0) * 2.0;
    }
    
    // Clamp to reasonable range
    riskMetric = std::max(0.0, std::min(10.0, riskMetric));
    
    return riskMetric;
}