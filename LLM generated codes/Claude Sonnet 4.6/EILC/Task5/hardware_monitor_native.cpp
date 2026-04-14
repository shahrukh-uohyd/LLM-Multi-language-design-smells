/**
 * hardware_monitor_native.cpp
 *
 * JNI implementation for:
 *   com.example.hwmonitor.model.HardwareHandle  (nativeOpenHandle / nativeCloseHandle)
 *   com.example.hwmonitor.SystemProbe           (six metric reads)
 *
 * Platform target : Linux hwmon sysfs
 * For Windows replace the sysfs helpers with WMI/OpenHardwareMonitor calls.
 * For macOS  replace with IOKit SMC calls.
 *
 * Build (Android NDK CMakeLists.txt):
 *   add_library(hardware_monitor_native SHARED hardware_monitor_native.cpp)
 *   target_link_libraries(hardware_monitor_native log)
 */

#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstdlib>
#include <cstring>
#include <cerrno>
#include <android/log.h>

#include "hardware_monitor_native.h"

#define LOG_TAG  "HwMonitorNative"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── sysfs path constants ──────────────────────────────────────────────────────
static constexpr const char* HWMON_BASE       = "/sys/class/hwmon/hwmon0";
static constexpr const char* PATH_CPU_TEMP    = "/sys/class/hwmon/hwmon0/temp1_input";
static constexpr const char* PATH_GPU_TEMP    = "/sys/class/hwmon/hwmon1/temp1_input";
static constexpr const char* PATH_MOBO_TEMP   = "/sys/class/hwmon/hwmon0/temp2_input";
static constexpr const char* PATH_FAN1        = "/sys/class/hwmon/hwmon0/fan1_input";
static constexpr const char* PATH_FAN2        = "/sys/class/hwmon/hwmon0/fan2_input";
static constexpr const char* PATH_BAT_VOLTAGE = "/sys/class/power_supply/BAT0/voltage_now";

// ── sysfs I/O helpers ─────────────────────────────────────────────────────────

/**
 * Opens a sysfs path read-only and returns the fd, or -1 on failure.
 * Non-fatal: a missing sensor fd causes the corresponding read to return
 * the unavailable sentinel instead of crashing.
 */
static int openSysfsPath(const char* path) {
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) {
        LOGE("openSysfsPath: cannot open '%s': %s", path, strerror(errno));
    }
    return fd;
}

/**
 * Reads a single ASCII integer from a sysfs virtual file.
 *
 * sysfs files must be re-read from offset 0 each time (the kernel regenerates
 * the content on every read syscall after an lseek to position 0).
 *
 * @return the integer value, or INT32_MIN on any I/O failure.
 */
static int32_t readSysfsInt(int fd) {
    if (fd < 0) return INT32_MIN;

    if (lseek(fd, 0, SEEK_SET) < 0) return INT32_MIN;

    char    buf[32] = {};
    ssize_t n       = read(fd, buf, sizeof(buf) - 1);
    if (n <= 0) return INT32_MIN;

    buf[n] = '\0';
    return static_cast<int32_t>(strtol(buf, nullptr, 10));
}

// ── Context guard helper ──────────────────────────────────────────────────────

/**
 * Validates and casts the opaque jlong handle.
 * Returns nullptr and logs an error if the handle is invalid or closed.
 */
static HwMonitorContext* resolveCtx(jlong jHandle, const char* caller) {
    auto* ctx = reinterpret_cast<HwMonitorContext*>(jHandle);
    if (!ctx || !ctx->valid) {
        LOGE("%s: received invalid or closed HwMonitorContext", caller);
        return nullptr;
    }
    return ctx;
}

// ── JNI implementations ───────────────────────────────────────────────────────

extern "C" {

// ─────────────────────────────────────────────────────────────────────────────
// HardwareHandle.nativeOpenHandle()
// Allocates HwMonitorContext and opens all six sensor fds.
// Returns the context pointer as jlong, or 0L on failure.
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jlong JNICALL
Java_com_example_hwmonitor_model_HardwareHandle_nativeOpenHandle(
        JNIEnv* /*env*/, jclass /*clazz*/) {

    auto* ctx = new (std::nothrow) HwMonitorContext{};
    if (!ctx) {
        LOGE("nativeOpenHandle: out of memory");
        return 0L;
    }

    // Validate that the hwmon subsystem is accessible
    ctx->hwmonDirFd = open(HWMON_BASE, O_RDONLY | O_DIRECTORY | O_CLOEXEC);
    if (ctx->hwmonDirFd < 0) {
        LOGE("nativeOpenHandle: hwmon base '%s' not accessible: %s",
             HWMON_BASE, strerror(errno));
        delete ctx;
        return 0L;
    }

    // Open individual sensor fds (failures are non-fatal — metric returns -1)
    ctx->fdCpuTemp       = openSysfsPath(PATH_CPU_TEMP);
    ctx->fdGpuTemp       = openSysfsPath(PATH_GPU_TEMP);
    ctx->fdMoboTemp      = openSysfsPath(PATH_MOBO_TEMP);
    ctx->fdFan1          = openSysfsPath(PATH_FAN1);
    ctx->fdFan2          = openSysfsPath(PATH_FAN2);
    ctx->fdBatteryVoltage = openSysfsPath(PATH_BAT_VOLTAGE);

    ctx->valid = true;
    LOGI("nativeOpenHandle: HwMonitorContext opened (ptr=%p)", ctx);
    return reinterpret_cast<jlong>(ctx);
}

// ─────────────────────────────────────────────────────────────────────────────
// HardwareHandle.nativeCloseHandle()
// Closes all sensor fds and frees the context.
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_example_hwmonitor_model_HardwareHandle_nativeCloseHandle(
        JNIEnv* /*env*/, jclass /*clazz*/, jlong jHandle) {

    auto* ctx = reinterpret_cast<HwMonitorContext*>(jHandle);
    if (!ctx) return;

    ctx->valid = false;

    auto closeFd = [](int fd) { if (fd >= 0) ::close(fd); };
    closeFd(ctx->fdCpuTemp);
    closeFd(ctx->fdGpuTemp);
    closeFd(ctx->fdMoboTemp);
    closeFd(ctx->fdFan1);
    closeFd(ctx->fdFan2);
    closeFd(ctx->fdBatteryVoltage);
    closeFd(ctx->hwmonDirFd);

    delete ctx;
    LOGI("nativeCloseHandle: HwMonitorContext freed");
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — SystemProbe.nativeReadCpuTemperature()
//
// sysfs reports millidegrees Celsius; divide by 1000 → °C.
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jfloat JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadCpuTemperature(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong jHandle) {

    HwMonitorContext* ctx = resolveCtx(jHandle, "nativeReadCpuTemperature");
    if (!ctx) return HW_METRIC_UNAVAILABLE_F;

    int32_t raw = readSysfsInt(ctx->fdCpuTemp);
    if (raw == INT32_MIN) {
        LOGE("nativeReadCpuTemperature: sensor read failed");
        return HW_METRIC_UNAVAILABLE_F;
    }

    float result = static_cast<float>(raw) / 1000.0f;
    LOGI("nativeReadCpuTemperature: %.1f °C", result);
    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — SystemProbe.nativeReadGpuTemperature()
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jfloat JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadGpuTemperature(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong jHandle) {

    HwMonitorContext* ctx = resolveCtx(jHandle, "nativeReadGpuTemperature");
    if (!ctx) return HW_METRIC_UNAVAILABLE_F;

    int32_t raw = readSysfsInt(ctx->fdGpuTemp);
    if (raw == INT32_MIN) {
        LOGE("nativeReadGpuTemperature: sensor read failed");
        return HW_METRIC_UNAVAILABLE_F;
    }

    float result = static_cast<float>(raw) / 1000.0f;
    LOGI("nativeReadGpuTemperature: %.1f °C", result);
    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — SystemProbe.nativeReadMoboTemperature()
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jfloat JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadMoboTemperature(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong jHandle) {

    HwMonitorContext* ctx = resolveCtx(jHandle, "nativeReadMoboTemperature");
    if (!ctx) return HW_METRIC_UNAVAILABLE_F;

    int32_t raw = readSysfsInt(ctx->fdMoboTemp);
    if (raw == INT32_MIN) {
        LOGE("nativeReadMoboTemperature: sensor read failed");
        return HW_METRIC_UNAVAILABLE_F;
    }

    float result = static_cast<float>(raw) / 1000.0f;
    LOGI("nativeReadMoboTemperature: %.1f °C", result);
    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4 — SystemProbe.nativeReadFan1Speed()
//
// sysfs reports RPM directly as an ASCII integer.
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jint JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadFan1Speed(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong jHandle) {

    HwMonitorContext* ctx = resolveCtx(jHandle, "nativeReadFan1Speed");
    if (!ctx) return HW_METRIC_UNAVAILABLE_I;

    int32_t rpm = readSysfsInt(ctx->fdFan1);
    if (rpm == INT32_MIN) {
        LOGE("nativeReadFan1Speed: sensor read failed");
        return HW_METRIC_UNAVAILABLE_I;
    }

    LOGI("nativeReadFan1Speed: %d RPM", rpm);
    return static_cast<jint>(rpm);
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 5 — SystemProbe.nativeReadFan2Speed()
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jint JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadFan2Speed(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong jHandle) {

    HwMonitorContext* ctx = resolveCtx(jHandle, "nativeReadFan2Speed");
    if (!ctx) return HW_METRIC_UNAVAILABLE_I;

    int32_t rpm = readSysfsInt(ctx->fdFan2);
    if (rpm == INT32_MIN) {
        LOGE("nativeReadFan2Speed: sensor read failed");
        return HW_METRIC_UNAVAILABLE_I;
    }

    LOGI("nativeReadFan2Speed: %d RPM", rpm);
    return static_cast<jint>(rpm);
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 6 — SystemProbe.nativeReadBatteryVoltage()
//
// sysfs reports microvolts (µV); divide by 1,000,000 → Volts.
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jfloat JNICALL
Java_com_example_hwmonitor_SystemProbe_nativeReadBatteryVoltage(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong jHandle) {

    HwMonitorContext* ctx = resolveCtx(jHandle, "nativeReadBatteryVoltage");
    if (!ctx) return HW_METRIC_UNAVAILABLE_F;

    int32_t raw = readSysfsInt(ctx->fdBatteryVoltage);
    if (raw == INT32_MIN) {
        LOGE("nativeReadBatteryVoltage: sensor read failed");
        return HW_METRIC_UNAVAILABLE_F;
    }

    float result = static_cast<float>(raw) / 1'000'000.0f;
    LOGI("nativeReadBatteryVoltage: %.3f V", result);
    return result;
}

} // extern "C"