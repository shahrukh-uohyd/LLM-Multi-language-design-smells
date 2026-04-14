/**
 * hardware_monitor_native.cpp
 *
 * JNI implementation for:
 *   com.example.hwmonitor.HardwareHandle  (open / close)
 *   com.example.hwmonitor.SystemProbe     (six metric reads)
 *
 * Platform: Linux hwmon sysfs  (/sys/class/hwmon/hwmon0/...)
 * For other platforms replace the sysfs I/O helpers with the
 * appropriate OS API (WMI on Windows, IOKit on macOS).
 *
 * Build (Android / Linux NDK):
 *   add_library(hardware_monitor_native SHARED hardware_monitor_native.cpp)
 *   target_link_libraries(hardware_monitor_native log)
 */

#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstdlib>
#include <cstring>
#include <cerrno>
#include <android/log.h>   // remove if not targeting Android

#include "hardware_monitor_native.h"

#define LOG_TAG  "HwMonitorNative"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── sysfs paths ───────────────────────────────────────────────────────────────
// Adjust these to match your /sys/class/hwmon/hwmon<N>/ layout.
static constexpr const char* HWMON_BASE        = "/sys/class/hwmon/hwmon0";
static constexpr const char* PATH_CPU_TEMP     = "/sys/class/hwmon/hwmon0/temp1_input";
static constexpr const char* PATH_GPU_TEMP     = "/sys/class/hwmon/hwmon1/temp1_input";
static constexpr const char* PATH_MOBO_TEMP    = "/sys/class/hwmon/hwmon0/temp2_input";
static constexpr const char* PATH_FAN1         = "/sys/class/hwmon/hwmon0/fan1_input";
static constexpr const char* PATH_FAN2         = "/sys/class/hwmon/hwmon0/fan2_input";
static constexpr const char* PATH_BAT_VOLTAGE  = "/sys/class/power_supply/BAT0/voltage_now";

// ── sysfs I/O helpers ─────────────────────────────────────────────────────────

/**
 * Reads a single integer from a sysfs file.
 * @return the integer value, or INT32_MIN on failure.
 */
static int32_t readSysfsInt(int fd) {
    if (fd < 0) return INT32_MIN;

    lseek(fd, 0, SEEK_SET);   // sysfs files must be re-read from offset 0

    char buf[32] = {};
    ssize_t n = read(fd, buf, sizeof(buf) - 1);
    if (n <= 0) return INT32_MIN;

    buf[n] = '\0';
    return static_cast<int32_t>(strtol(buf, nullptr, 10));
}

/**
 * Opens a sysfs path and caches the fd.
 * @return fd on success, or -1 on failure.
 */
static int openSysfsPath(const char* path) {
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) {
        LOGE("openSysfsPath: cannot open '%s': %s", path, strerror(errno));
    }
    return fd;
}

// ── HardwareHandle — open / close ────────────────────────────────────────────

extern "C" {

/**
 * com.example.hwmonitor.model.HardwareHandle.nativeOpenHandle
 *
 * Allocates and initialises an HwMonitorContext; opens every sensor fd.
 * Returns the context pointer cast to jlong, or 0 on failure.
 */
JNIEXPORT jlong JNICALL
Java_com_example_hwmonitor_model_HardwareHandle_nativeOpenHandle(
        JNIEnv* /* env */, jclass /* clazz */) {

    auto* ctx = new (std::nothrow) HwMonitorContext{};
    if (!ctx) {
        LOGE("nativeOpenHandle: out of memory");
        return 0L;
    }

    // Open base hwmon directory fd (used for validity check)
    ctx->hwmonFd = open(HWMON_BASE, O_RDONLY | O_DIRECTORY | O_CLOEXEC);
    if (ctx->hwmonFd < 0) {
        LOGE("nativeOpenHandle: cannot open hwmon base '%s': %s",
             HWMON_BASE, strerror(errno));
        delete ctx;
        return 0L;
    }

    // Open individual sensor fds (failures are non-fatal; reads return unavailable)
    ctx->fdCpuTemp      = openSysfsPath(PATH_CPU_TEMP);
    ctx->fdGpuTemp      = openSysfsPath(PATH_GPU_TEMP);
    ctx->fdMoboTemp     = openSysfsPath(PATH_MOBO_TEMP);
    ctx->fdFan1         = openSysfsPath(PATH_FAN1);
    ctx->fdFan2         = openSysfsPath(PATH_FAN2);
    ctx->fdBatteryVoltage = openSysfsPath(PATH_BAT_VOLTAGE);

    ctx->valid = true;
    LOGI("nativeOpenHandle: hardware context opened (ptr=%p)", ctx);
    return reinterpret_cast<jlong>(ctx);
}

/**
 * com.example.hwmonitor.model.HardwareHandle.nativeCloseHandle
 *
 * Closes all sensor fds and frees the context.
 */
JNIEXPORT void JNICALL
Java_com_example_hwmonitor_model_HardwareHandle_nativeCloseHandle(
        JNIEnv* /* env */, jclass /* clazz */, jlong jHandle) {

    auto* ctx = reinterpret_cast<HwMonitorContext*>(jHandle);
    if (!ctx) return;

    ctx->valid = false;

    auto closeFd = [](int fd) { if (fd >= 0) close(fd); };
    closeFd(ctx->fdCpuTemp);
    closeFd(ctx->fdGpuTemp);
    closeFd(ctx->fdMoboTemp);
    closeFd(ctx->fdFan1);
    closeFd(ctx->fdFan2);
    closeFd(ctx->fdBatteryVoltage);
    closeFd(ctx->hwmonFd);

    delete ctx;
    LOGI("nativeCloseHandle: hardware context released");
}

// ── SystemProbe — six metric reads ───────────────────────────────────────────

/**
 * Helper: validate the handle pointer and cast it.
 * Returns nullptr (and logs) if the handle is invalid.
 */
static HwMonitorContext* resolveHandle(jlong jHandle, const char* caller) {
    auto* ctx = reinterpret_cast<HwMonitorContext*>(jHandle);
    if (!ctx || !ctx->valid) {
        LOGE("%s: received invalid or closed HardwareHandle", caller);
        return nullptr;
    }
    return ctx;
}

// ── Step 1 : CPU Temperature ──────────────────────────────────────────────────
/**
 * com.example.hwmonitor.SystemProbe.nativeReadCpuTemperature
 *
 * sysfs reports millidegrees Celsius; divide by 1000 to get °C.
 */
JNIEXPORT jfloat JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadCpuTemperature(
        JNIEnv* /* env */, jobject /* thiz */, jlong jHandle) {

    HwMonitorContext* ctx = resolveHandle(jHandle, "nativeReadCpuTemperature");
    if (!ctx) return HW_METRIC_UNAVAILABLE;

    int32_t raw = readSysfsInt(ctx->fdCpuTemp);
    if (raw == INT32_MIN) {
        LOGE("nativeReadCpuTemperature: sensor read failed");
        return HW_METRIC_UNAVAILABLE;
    }

    float tempCelsius = static_cast<float>(raw) / 1000.0f;
    LOGI("nativeReadCpuTemperature: %.1f °C", tempCelsius);
    return tempCelsius;
}

// ── Step 2 : GPU Temperature ──────────────────────────────────────────────────
/**
 * com.example.hwmonitor.SystemProbe.nativeReadGpuTemperature
 */
JNIEXPORT jfloat JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadGpuTemperature(
        JNIEnv* /* env */, jobject /* thiz */, jlong jHandle) {

    HwMonitorContext* ctx = resolveHandle(jHandle, "nativeReadGpuTemperature");
    if (!ctx) return HW_METRIC_UNAVAILABLE;

    int32_t raw = readSysfsInt(ctx->fdGpuTemp);
    if (raw == INT32_MIN) {
        LOGE("nativeReadGpuTemperature: sensor read failed");
        return HW_METRIC_UNAVAILABLE;
    }

    float tempCelsius = static_cast<float>(raw) / 1000.0f;
    LOGI("nativeReadGpuTemperature: %.1f °C", tempCelsius);
    return tempCelsius;
}

// ── Step 3 : Motherboard Temperature ─────────────────────────────────────────
/**
 * com.example.hwmonitor.SystemProbe.nativeReadMoboTemperature
 */
JNIEXPORT jfloat JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadMoboTemperature(
        JNIEnv* /* env */, jobject /* thiz */, jlong jHandle) {

    HwMonitorContext* ctx = resolveHandle(jHandle, "nativeReadMoboTemperature");
    if (!ctx) return HW_METRIC_UNAVAILABLE;

    int32_t raw = readSysfsInt(ctx->fdMoboTemp);
    if (raw == INT32_MIN) {
        LOGE("nativeReadMoboTemperature: sensor read failed");
        return HW_METRIC_UNAVAILABLE;
    }

    float tempCelsius = static_cast<float>(raw) / 1000.0f;
    LOGI("nativeReadMoboTemperature: %.1f °C", tempCelsius);
    return tempCelsius;
}

// ── Step 4 : Fan 1 Speed ──────────────────────────────────────────────────────
/**
 * com.example.hwmonitor.SystemProbe.nativeReadFan1Speed
 *
 * sysfs reports RPM directly as an integer.
 */
JNIEXPORT jint JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadFan1Speed(
        JNIEnv* /* env */, jobject /* thiz */, jlong jHandle) {

    HwMonitorContext* ctx = resolveHandle(jHandle, "nativeReadFan1Speed");
    if (!ctx) return HW_FAN_UNAVAILABLE;

    int32_t rpm = readSysfsInt(ctx->fdFan1);
    if (rpm == INT32_MIN) {
        LOGE("nativeReadFan1Speed: sensor read failed");
        return HW_FAN_UNAVAILABLE;
    }

    LOGI("nativeReadFan1Speed: %d RPM", rpm);
    return static_cast<jint>(rpm);
}

// ── Step 5 : Fan 2 Speed ──────────────────────────────────────────────────────
/**
 * com.example.hwmonitor.SystemProbe.nativeReadFan2Speed
 */
JNIEXPORT jint JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadFan2Speed(
        JNIEnv* /* env */, jobject /* thiz */, jlong jHandle) {

    HwMonitorContext* ctx = resolveHandle(jHandle, "nativeReadFan2Speed");
    if (!ctx) return HW_FAN_UNAVAILABLE;

    int32_t rpm = readSysfsInt(ctx->fdFan2);
    if (rpm == INT32_MIN) {
        LOGE("nativeReadFan2Speed: sensor read failed");
        return HW_FAN_UNAVAILABLE;
    }

    LOGI("nativeReadFan2Speed: %d RPM", rpm);
    return static_cast<jint>(rpm);
}

// ── Step 6 : Battery Voltage ──────────────────────────────────────────────────
/**
 * com.example.hwmonitor.SystemProbe.nativeReadBatteryVoltage
 *
 * sysfs reports microvolts (µV); divide by 1,000,000 to get Volts.
 */
JNIEXPORT jfloat JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadBatteryVoltage(
        JNIEnv* /* env */, jobject /* thiz */, jlong jHandle) {

    HwMonitorContext* ctx = resolveHandle(jHandle, "nativeReadBatteryVoltage");
    if (!ctx) return HW_METRIC_UNAVAILABLE;

    int32_t raw = readSysfsInt(ctx->fdBatteryVoltage);
    if (raw == INT32_MIN) {
        LOGE("nativeReadBatteryVoltage: sensor read failed");
        return HW_METRIC_UNAVAILABLE;
    }

    float volts = static_cast<float>(raw) / 1_000_000.0f;
    LOGI("nativeReadBatteryVoltage: %.3f V", volts);
    return volts;
}

} // extern "C"