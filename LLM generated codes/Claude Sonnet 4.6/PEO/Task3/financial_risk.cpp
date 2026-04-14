#include <jni.h>
#include <cmath>       // std::abs, std::isfinite
#include <stdexcept>   // for domain error constants
#include "FinancialAccount.h"   // generated via: javac -h . FinancialAccount.java

// ---------------------------------------------------------------------------
// Risk Metric Formula (pure C++ — zero JNI dependency):
//
//   risk = balance / creditLimit
//
// Interpretation:
//   0.0        → no balance drawn, zero risk
//   0.0–0.30   → low utilisation, low risk
//   0.30–0.70  → moderate utilisation
//   0.70–1.0   → high utilisation, elevated risk
//   > 1.0      → balance exceeds credit limit, critical risk
//
// This is the standard Credit Utilisation Ratio used in credit scoring models
// (e.g. FICO). It is intentionally isolated from JNI so it can be unit-tested
// in pure C++ without a JVM.
// ---------------------------------------------------------------------------
static double computeRiskMetric(double balance, double creditLimit) {

    // --- Guard 1: Non-finite inputs (NaN, Inf) are rejected ---
    if (!std::isfinite(balance) || !std::isfinite(creditLimit)) {
        return -1.0;    // Sentinel: caller should treat as invalid input
    }

    // --- Guard 2: Division-by-zero — creditLimit must be positive ---
    if (creditLimit <= 0.0) {
        return -1.0;    // Sentinel: undefined ratio, signal to caller
    }

    // --- Guard 3: Negative balance (credit/overpayment) — clamp to 0 ---
    // A negative balance means the account holder is in credit.
    // Treat as zero utilisation for risk purposes.
    double effectiveBalance = (balance < 0.0) ? 0.0 : balance;

    // --- Core calculation ---
    return effectiveBalance / creditLimit;
}


// ---------------------------------------------------------------------------
// APPROACH A (Less Preferred):
// Receives the full FinancialAccount jobject.
// Reads ONLY balance and creditLimit — every other field is deliberately
// ignored, but the compiler cannot enforce that constraint.
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jdouble JNICALL
Java_FinancialAccount_calculateRiskMetric(JNIEnv *env, jobject thisObj) {

    // --- Step 1: Resolve the class ---
    jclass accountClass = env->GetObjectClass(thisObj);
    if (accountClass == nullptr) {
        return -1.0;    // Fail safely — do not throw or crash
    }

    // --- Step 2: Resolve field IDs for balance and creditLimit ONLY ---
    // JNI type descriptor for 'double' is "D"
    jfieldID fid_balance     = env->GetFieldID(accountClass, "balance",     "D");
    jfieldID fid_creditLimit = env->GetFieldID(accountClass, "creditLimit", "D");

    if (fid_balance == nullptr || fid_creditLimit == nullptr) {
        return -1.0;    // Field resolution failed — fail safely
    }

    // --- Step 3: Read ONLY the two required fields ---
    // accountNumber, customerName, interestRate, accountStatus: never touched.
    jdouble balance     = env->GetDoubleField(thisObj, fid_balance);
    jdouble creditLimit = env->GetDoubleField(thisObj, fid_creditLimit);

    // --- Step 4: Delegate to the pure-C++ computation ---
    return static_cast<jdouble>(computeRiskMetric(balance, creditLimit));
}


// ---------------------------------------------------------------------------
// APPROACH B (RECOMMENDED — Principle of Least Privilege):
// Java's calculateRiskMetricSecure() extracted balance and creditLimit
// before crossing the JNI boundary. 'thisObj' is the implicit Java 'this'
// reference — it is deliberately unused and commented as such.
//
// accountNumber, customerName, interestRate, accountStatus, and the rest
// of the FinancialAccount object are structurally unreachable here.
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jdouble JNICALL
Java_FinancialAccount_nativeCalculateRiskMetric(JNIEnv *env, jobject /*thisObj*/,
                                                jdouble balance, jdouble creditLimit) {

    // No object introspection. No field lookups. No sensitive data in scope.
    // The computation receives exactly the two numbers it needs — nothing more.
    return static_cast<jdouble>(computeRiskMetric(balance, creditLimit));
}