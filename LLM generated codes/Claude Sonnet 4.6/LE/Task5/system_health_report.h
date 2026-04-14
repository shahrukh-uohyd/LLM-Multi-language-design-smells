/**
 * system_health_report.h
 *
 * Internal C++ types, hardware-access abstractions, threshold constants,
 * and shared helper declarations for the SystemHealthReport JNI layer.
 *
 * ── Platform read interfaces ─────────────��───────────────────────────
 *
 *  CPU temperature  : /sys/class/thermal/thermal_zone*/temp  (Linux)
 *                     ACPI _TMP / IPMI SDR                   (generic)
 *
 *  HW interrupts    : /proc/interrupts column sums           (Linux)
 *                     APIC IRQ counter registers             (generic)
 *
 *  Power consumption: Intel RAPL MSR 0x611 (PKG_ENERGY_STATUS)(x86)
 *                     IPMI dcmi power reading                (generic)
 */

#ifndef SYSTEM_HEALTH_REPORT_H
#define SYSTEM_HEALTH_REPORT_H

#include <jni.h>
#include <cstdint>
#include <cstddef>
#include <string>

// ── Physical sensor range guards ─────────────────────────────────── //
inline constexpr double  CPU_TEMP_MIN_C    = -40.0;
inline constexpr double  CPU_TEMP_MAX_C    = 150.0;
inline constexpr double  POWER_MIN_W       =   0.0;
inline constexpr double  POWER_MAX_W       = 5000.0;  // generous server ceiling

// ── Linux sysfs / procfs paths (used by stub implementations) ──── //
inline constexpr const char* SYSFS_THERMAL_PATH =
        "/sys/class/thermal/thermal_zone0/temp";
inline constexpr const char* PROC_INTERRUPTS_PATH =
        "/proc/interrupts";

// ── Platform hardware-read function declarations ─────────────────── //

/**
 * Reads the peak CPU core temperature in degrees Celsius.
 *
 * @param out_temp  receives the temperature value on success
 * @param err_msg   receives a diagnostic message on failure
 * @return true on success, false on failure
 */
bool hw_read_cpu_temperature(double& out_temp, std::string& err_msg);

/**
 * Returns the total active hardware interrupt count across all CPUs.
 *
 * @param out_count  receives the interrupt count on success
 * @param err_msg    receives a diagnostic message on failure
 * @return true on success, false on failure
 */
bool hw_read_hardware_interrupts(long long& out_count, std::string& err_msg);

/**
 * Reads the current system power consumption in watts.
 *
 * @param out_watts  receives the wattage on success
 * @param err_msg    receives a diagnostic message on failure
 * @return true on success, false on failure
 */
bool hw_read_power_consumption(double& out_watts, std::string& err_msg);

// ── JNI helper declarations ──────────────────────────────────────── //

/**
 * Throws a Java HardwareMetricException identifying the given metric.
 *
 * @param env          JNI environment
 * @param metric_field Java enum field name, e.g. "CPU_TEMPERATURE"
 * @param message      non-sensitive diagnostic description
 * @return always 0 / nullptr — assign directly:
 *         return static_cast<jdouble>(throwMetricEx(env, "CPU_TEMPERATURE", "msg"));
 */
void throwMetricEx(JNIEnv* env,
                   const char* metric_field,
                   const char* message);

#endif // SYSTEM_HEALTH_REPORT_H