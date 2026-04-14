#include <jni.h>
#include <string>
#include <sstream>
#include <iostream>
#include <stdexcept>
#include <chrono>

// ════════════════════════════════════════════════════════════════════════════
//  JNI Utility Helpers
// ════════════════════════════════════════════════════════════════════════════

/** Throw a Java RuntimeException from C++ with a descriptive message. */
static void throwJavaException(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls) env->ThrowNew(cls, msg);
}

/** Convert jstring → std::string. Returns "" if jstr is null. */
static std::string jstringToStd(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* raw = env->GetStringUTFChars(jstr, nullptr);
    std::string result(raw);
    env->ReleaseStringUTFChars(jstr, raw);
    return result;
}

/** Convert std::string → jstring. */
static jstring stdToJstring(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

/** Return current time as milliseconds since the Unix epoch. */
static jlong nowMillis() {
    using namespace std::chrono;
    return (jlong)duration_cast<milliseconds>(
        system_clock::now().time_since_epoch()).count();
}

// ════════════════════════════════════════════════════════════════════════════
//  ConnectionStatus Enum Helper
//
//  Reads the enum ordinal name from Java and writes a new enum constant
//  back into the Java object through the setStatus() setter.
// ════════════════════════════════════════════════════════════════════════════

/**
 * Reads the current ConnectionStatus enum name from the Java object.
 * Calls: connection.getStatus().name()  →  e.g. "CONNECTED"
 */
static std::string readStatusName(JNIEnv* env, jobject connObj) {

    // ── Step 1: call getStatus() on HardwareConnection ──────────────────
    jclass connClass = env->GetObjectClass(connObj);
    if (!connClass)
        throw std::runtime_error("Cannot resolve HardwareConnection class");

    jmethodID getStatus =
        env->GetMethodID(connClass, "getStatus", "()LConnectionStatus;");
    if (!getStatus)
        throw std::runtime_error("Cannot find HardwareConnection.getStatus()");

    jobject statusEnum = env->CallObjectMethod(connObj, getStatus);
    if (!statusEnum)
        throw std::runtime_error("getStatus() returned null");

    // ── Step 2: call name() on the enum constant ─────────────────────────
    jclass enumClass = env->GetObjectClass(statusEnum);
    jmethodID nameMethod =
        env->GetMethodID(enumClass, "name", "()Ljava/lang/String;");
    if (!nameMethod)
        throw std::runtime_error("Cannot find Enum.name()");

    auto jName = (jstring)env->CallObjectMethod(statusEnum, nameMethod);
    return jstringToStd(env, jName);
}

/**
 * Sets the ConnectionStatus enum field on the Java object by calling
 * the static ConnectionStatus.valueOf(String) method, then passing
 * the resulting constant to setStatus().
 *
 * @param statusName  e.g. "DISCONNECTED", "RESETTING", "TIMEOUT"
 */
static void writeStatus(JNIEnv*            env,
                        jobject            connObj,
                        const std::string& statusName)
{
    // ── Step 1: resolve ConnectionStatus.valueOf(String) ─────────────────
    jclass statusClass = env->FindClass("ConnectionStatus");
    if (!statusClass)
        throw std::runtime_error("Cannot find ConnectionStatus class");

    jmethodID valueOf = env->GetStaticMethodID(
        statusClass, "valueOf",
        "(Ljava/lang/String;)LConnectionStatus;");
    if (!valueOf)
        throw std::runtime_error("Cannot find ConnectionStatus.valueOf()");

    jobject newStatusEnum = env->CallStaticObjectMethod(
        statusClass, valueOf,
        stdToJstring(env, statusName));
    if (!newStatusEnum)
        throw std::runtime_error(
            "ConnectionStatus.valueOf() returned null for: " + statusName);

    // ── Step 2: call setStatus(ConnectionStatus) on HardwareConnection ───
    jclass connClass = env->GetObjectClass(connObj);
    jmethodID setStatus =
        env->GetMethodID(connClass, "setStatus", "(LConnectionStatus;)V");
    if (!setStatus)
        throw std::runtime_error("Cannot find HardwareConnection.setStatus()");

    env->CallVoidMethod(connObj, setStatus, newStatusEnum);
}

// ════════════════════════════════════════════════════════════════════════════
//  Core Reset Logic  (shared between both JNI entry points)
// ════════════════════════════════════════════════════════════════════════════

/**
 * Performs the full reset sequence on the Java HardwareConnection object:
 *
 *   1. Read current status — skip if already DISCONNECTED.
 *   2. Transition status   → RESETTING  (intermediate guard state).
 *   3. Increment timeoutCount on the Java side.
 *   4. Record the reset timestamp.
 *   5. Transition status   → DISCONNECTED  (final default state).
 */
static void performReset(JNIEnv* env, jobject connObj) {

    jclass connClass = env->GetObjectClass(connObj);
    if (!connClass)
        throw std::runtime_error("Cannot resolve HardwareConnection class");

    // ── Read device name for logging ─────────────────────────────────────
    jmethodID getDeviceName =
        env->GetMethodID(connClass, "getDeviceName", "()Ljava/lang/String;");
    std::string deviceName = getDeviceName
        ? jstringToStd(env, (jstring)env->CallObjectMethod(connObj, getDeviceName))
        : "<unknown>";

    // ── Read current status ────────────────────────���─────────────────────
    std::string currentStatus = readStatusName(env, connObj);
    std::cout << "[NATIVE] Device '" << deviceName
              << "' — current status: " << currentStatus << "\n";

    // ── Guard: nothing to do if already in the default state ─────────────
    if (currentStatus == "DISCONNECTED") {
        std::cout << "[NATIVE] Already DISCONNECTED — reset skipped.\n";
        return;
    }

    // ── Step 1: transition to RESETTING ──────────────────────────────────
    std::cout << "[NATIVE] Transitioning to RESETTING...\n";
    writeStatus(env, connObj, "RESETTING");

    // ── Step 2: increment timeoutCount ───────────────────────────────────
    jmethodID getTimeoutCount =
        env->GetMethodID(connClass, "getTimeoutCount", "()I");
    jmethodID setTimeoutCount =
        env->GetMethodID(connClass, "setTimeoutCount", "(I)V");

    if (getTimeoutCount && setTimeoutCount) {
        jint count = env->CallIntMethod(connObj, getTimeoutCount);
        env->CallVoidMethod(connObj, setTimeoutCount, count + 1);
        std::cout << "[NATIVE] Timeout count incremented to: "
                  << (count + 1) << "\n";
    }

    // ── Step 3: record the reset timestamp ───────────────────────────────
    jmethodID setLastReset =
        env->GetMethodID(connClass, "setLastResetTimestamp", "(J)V");
    if (setLastReset) {
        jlong ts = nowMillis();
        env->CallVoidMethod(connObj, setLastReset, ts);
        std::cout << "[NATIVE] Reset timestamp recorded: " << ts << " ms\n";
    }

    // ── Step 4: transition to final default state: DISCONNECTED ──────────
    std::cout << "[NATIVE] Transitioning to DISCONNECTED (default state).\n";
    writeStatus(env, connObj, "DISCONNECTED");

    std::cout << "[NATIVE] Reset complete for device '" << deviceName << "'.\n";
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: HardwareConnection.resetAfterTimeout()
//
//  Entry point called directly when native code detects a hardware timeout.
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT void JNICALL
Java_HardwareConnection_resetAfterTimeout(JNIEnv* env, jobject connObj)
{
    try {
        std::cout << "[NATIVE] resetAfterTimeout() invoked.\n";
        performReset(env, connObj);
    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: HardwareConnection.simulateTimeoutAndReset()
//
//  Simulates a hardware timeout by forcing the status to TIMEOUT first,
//  then delegates to the same reset logic.
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT void JNICALL
Java_HardwareConnection_simulateTimeoutAndReset(JNIEnv* env, jobject connObj)
{
    try {
        std::cout << "[NATIVE] Simulating hardware timeout...\n";

        // Force the status to TIMEOUT to mimic a real hardware event
        writeStatus(env, connObj, "TIMEOUT");
        std::cout << "[NATIVE] Status set to TIMEOUT.\n";

        // Now perform the standard reset sequence
        performReset(env, connObj);

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: HardwareConnection.getDiagnosticReport()
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jstring JNICALL
Java_HardwareConnection_getDiagnosticReport(JNIEnv* env, jobject connObj)
{
    try {
        jclass connClass = env->GetObjectClass(connObj);
        if (!connClass)
            throw std::runtime_error("Cannot resolve HardwareConnection class");

        // ── Read all fields ───────────────────────────────────────────────

        // deviceName
        jmethodID getDeviceName =
            env->GetMethodID(connClass, "getDeviceName", "()Ljava/lang/String;");
        std::string deviceName = getDeviceName
            ? jstringToStd(env,
                  (jstring)env->CallObjectMethod(connObj, getDeviceName))
            : "<unknown>";

        // status
        std::string statusName = readStatusName(env, connObj);

        // timeoutCount
        jmethodID getTimeoutCount =
            env->GetMethodID(connClass, "getTimeoutCount", "()I");
        jint timeoutCount = getTimeoutCount
            ? env->CallIntMethod(connObj, getTimeoutCount)
            : -1;

        // lastResetTimestamp
        jmethodID getLastReset =
            env->GetMethodID(connClass, "getLastResetTimestamp", "()J");
        jlong lastReset = getLastReset
            ? env->CallLongMethod(connObj, getLastReset)
            : 0L;

        // ── Build report ─────────────────────────────────────────────────
        std::ostringstream oss;
        oss << "[DIAGNOSTIC REPORT]\n"
            << "  Device Name        : " << deviceName               << "\n"
            << "  Current Status     : " << statusName               << "\n"
            << "  Timeout Count      : " << timeoutCount             << "\n"
            << "  Last Reset (epoch) : "
            << (lastReset == 0 ? "Never" : std::to_string(lastReset))
            << "\n";

        return stdToJstring(env, oss.str());

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return nullptr;
    }
}