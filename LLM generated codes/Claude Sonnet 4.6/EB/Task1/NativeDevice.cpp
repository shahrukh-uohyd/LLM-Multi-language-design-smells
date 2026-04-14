#include <jni.h>
#include <string>
#include <iostream>
#include <stdexcept>

// ─── Helper: throw a Java RuntimeException from C++ ────────────────────────
static void throwJavaException(JNIEnv* env, const char* message) {
    jclass exClass = env->FindClass("java/lang/RuntimeException");
    if (exClass) env->ThrowNew(exClass, message);
}

// ─── Helper: read all device info from the Java Device object ──────────────
struct DeviceInfo {
    std::string deviceId;
    int         batteryLevel;
    bool        isOnline;
    std::string firmwareVersion;
};

static DeviceInfo readDeviceInfo(JNIEnv* env, jobject deviceObj) {
    DeviceInfo info{};

    // ── Device class ───────────────────────────────────────────────────────
    jclass deviceClass = env->GetObjectClass(deviceObj);
    if (!deviceClass) throw std::runtime_error("Cannot find Device class");

    // device.getDeviceId()
    jmethodID getDeviceId = env->GetMethodID(deviceClass, "getDeviceId",
                                             "()Ljava/lang/String;");
    if (!getDeviceId) throw std::runtime_error("Cannot find getDeviceId()");

    auto jDeviceId = (jstring) env->CallObjectMethod(deviceObj, getDeviceId);
    if (jDeviceId) {
        const char* cStr = env->GetStringUTFChars(jDeviceId, nullptr);
        info.deviceId = cStr;
        env->ReleaseStringUTFChars(jDeviceId, cStr);
    }

    // device.getState()  →  DeviceState object
    jmethodID getState = env->GetMethodID(deviceClass, "getState",
                                          "()LDeviceState;");
    if (!getState) throw std::runtime_error("Cannot find getState()");

    jobject stateObj = env->CallObjectMethod(deviceObj, getState);
    if (!stateObj) throw std::runtime_error("getState() returned null");

    // ── DeviceState class ──────────────────────────────────────────────────
    jclass stateClass = env->GetObjectClass(stateObj);
    if (!stateClass) throw std::runtime_error("Cannot find DeviceState class");

    // state.getBatteryLevel()
    jmethodID getBattery = env->GetMethodID(stateClass, "getBatteryLevel", "()I");
    if (!getBattery) throw std::runtime_error("Cannot find getBatteryLevel()");
    info.batteryLevel = env->CallIntMethod(stateObj, getBattery);

    // state.isOnline()
    jmethodID isOnline = env->GetMethodID(stateClass, "isOnline", "()Z");
    if (!isOnline) throw std::runtime_error("Cannot find isOnline()");
    info.isOnline = (env->CallBooleanMethod(stateObj, isOnline) == JNI_TRUE);

    // state.getFirmwareVersion()
    jmethodID getFirmware = env->GetMethodID(stateClass, "getFirmwareVersion",
                                             "()Ljava/lang/String;");
    if (!getFirmware) throw std::runtime_error("Cannot find getFirmwareVersion()");

    auto jFirmware = (jstring) env->CallObjectMethod(stateObj, getFirmware);
    if (jFirmware) {
        const char* cStr = env->GetStringUTFChars(jFirmware, nullptr);
        info.firmwareVersion = cStr;
        env->ReleaseStringUTFChars(jFirmware, cStr);
    }

    return info;
}

// ─── JNI: Device.performNativeDiagnostic() ─────────────────────────────────
extern "C"
JNIEXPORT jstring JNICALL
Java_Device_performNativeDiagnostic(JNIEnv* env, jobject thisObj) {
    try {
        DeviceInfo info = readDeviceInfo(env, thisObj);

        // Simulate native diagnostic logic
        std::string status = info.isOnline ? "ONLINE" : "OFFLINE";
        std::string report =
            "[NATIVE DIAGNOSTIC]\n"
            "  Device ID       : " + info.deviceId        + "\n"
            "  Firmware        : " + info.firmwareVersion  + "\n"
            "  Battery Level   : " + std::to_string(info.batteryLevel) + "%\n"
            "  Network Status  : " + status               + "\n"
            "  Diagnosis       : " + (info.batteryLevel < 20
                                        ? "WARNING: Low battery"
                                        : "All systems nominal");

        return env->NewStringUTF(report.c_str());

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return nullptr;
    }
}

// ─── JNI: Device.sendNativeCommand(String command) ─────────────────────────
extern "C"
JNIEXPORT jboolean JNICALL
Java_Device_sendNativeCommand(JNIEnv* env, jobject thisObj, jstring jCommand) {
    try {
        DeviceInfo info = readDeviceInfo(env, thisObj);

        if (!jCommand) {
            throwJavaException(env, "Command string is null");
            return JNI_FALSE;
        }

        const char* cmdCStr = env->GetStringUTFChars(jCommand, nullptr);
        std::string command(cmdCStr);
        env->ReleaseStringUTFChars(jCommand, cmdCStr);

        std::cout << "[NATIVE] Received command '" << command
                  << "' for device '" << info.deviceId << "'\n";

        // Simulate: only accept commands when the device is online
        if (!info.isOnline) {
            std::cout << "[NATIVE] Device is OFFLINE – command rejected.\n";
            return JNI_FALSE;
        }

        if (command == "PING" || command == "RESET" || command == "STATUS") {
            std::cout << "[NATIVE] Command '" << command << "' accepted.\n";
            return JNI_TRUE;
        }

        std::cout << "[NATIVE] Unknown command '" << command << "' – rejected.\n";
        return JNI_FALSE;

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return JNI_FALSE;
    }
}