/**
 * system_health_report.cpp
 *
 * JNI C++ implementation of the three hardware metric reads declared
 * in SystemHealthReport.java.
 *
 * ── Platform abstractions ─────────────────────────────────────────────
 *
 *  hw_read_cpu_temperature()
 *      Linux production:  reads /sys/class/thermal/thermal_zone*/temp
 *                         (value is in milli-Celsius; divide by 1000)
 *      Stub (this file):  returns a deterministic simulated value so
 *                         the smoke-test is self-contained without root.
 *
 *  hw_read_hardware_interrupts()
 *      Linux production:  parses /proc/interrupts, sums the per-CPU
 *                         columns for all hardware interrupt lines.
 *      Stub (this file):  returns a simulated counter.
 *
 *  hw_read_power_consumption()
 *      Linux/x86 production: reads Intel RAPL via MSR 0x611, converts
 *                            energy units using MSR 0x606 time window.
 *      Stub (this file):  returns a simulated wattage.
 *
 * Replace the three hw_read_* stub bodies with real platform calls
 * (open(), read(), ioctl(), pread() on /dev/cpu/*/msr, etc.) to
 * connect to actual hardware.
 */

#include "system_health_report.h"

#include <cerrno>
#include <cstdio>
#include <cstring>
#include <sstream>
#include <string>

// ═══════════════════════════════════════════════════════════════════════
//  JNI helper implementation
// ═══════════════════════════════════════════════════════════════════════

void throwMetricEx(JNIEnv*     env,
                   const char* metric_field,
                   const char* message)
{
    // Locate HardwareMetricException class
    jclass ex_cls = env->FindClass("HardwareMetricException");
    if (!ex_cls) return; // FindClass already threw NoClassDefFoundError

    // Locate the nested Metric enum class
    jclass metric_cls = env->FindClass("HardwareMetricException$Metric");
    if (!metric_cls) return;

    // Retrieve the matching static enum constant
    jfieldID fid = env->GetStaticFieldID(
            metric_cls, metric_field, "LHardwareMetricException$Metric;");
    if (!fid) return;

    jobject metric_obj = env->GetStaticObjectField(metric_cls, fid);
    if (!metric_obj) return;

    // Locate the (Metric, String) constructor
    jmethodID ctor = env->GetMethodID(
            ex_cls, "<init>",
            "(LHardwareMetricException$Metric;Ljava/lang/String;)V");
    if (!ctor) return;

    jstring jmsg = env->NewStringUTF(message);
    jobject ex   = env->NewObject(ex_cls, ctor, metric_obj, jmsg);
    if (ex) env->Throw(static_cast<jthrowable>(ex));
}


// ═══════════════════════════════════════════════════════════════════════
//  Platform hardware-read implementations
//
//  ── PRODUCTION REPLACEMENT GUIDE ────────────────────────────────────
//
//  hw_read_cpu_temperature():
//    FILE* f = fopen("/sys/class/thermal/thermal_zone0/temp", "r");
//    int milli_c; fscanf(f, "%d", &milli_c); fclose(f);
//    out_temp = milli_c / 1000.0;
//
//  hw_read_hardware_interrupts():
//    Parse /proc/interrupts: skip the first header line, then for
//    each subsequent line sum columns 1..N (N = number of CPUs).
//    Use sscanf or std::istringstream per line.
//
//  hw_read_power_consumption() via Intel RAPL:
//    int fd = open("/dev/cpu/0/msr", O_RDONLY);
//    uint64_t unit_raw; pread(fd, &unit_raw, 8, 0x606);
//    double energy_unit = pow(0.5, (unit_raw >> 8) & 0x1F);
//    uint64_t energy_raw; pread(fd, &energy_raw, 8, 0x611);
//    // Differentiate two readings ~100 ms apart to get watts.
//    close(fd);
// ════════════════════════════════════════════════════════════════════════

bool hw_read_cpu_temperature(double& out_temp, std::string& err_msg)
{
    /*
     * ── Stub implementation ──────────────────────────────────────────
     * Simulates a realistic idle server temperature with a small
     * pseudo-random variation so consecutive calls return different values.
     * Replace with the sysfs / IPMI / BMC read described above.
     */
    static long long call_count = 0;
    ++call_count;

    // Cycle through a realistic range: 42–88 °C
    out_temp = 42.0 + static_cast<double>((call_count * 7) % 47);
    return true;
}

bool hw_read_hardware_interrupts(long long& out_count, std::string& err_msg)
{
    /*
     * ── Stub implementation ──────────────────────────────────────────
     * Simulates a monotonically increasing interrupt counter.
     * Replace with the /proc/interrupts parse described above.
     */
    static long long base_irq = 450000LL;
    static long long call_count = 0;
    ++call_count;

    // Simulate ~50 000 new interrupts per call
    base_irq += 50000LL;
    out_count = base_irq + (call_count % 13) * 1000LL;
    return true;
}

bool hw_read_power_consumption(double& out_watts, std::string& err_msg)
{
    /*
     * ── Stub implementation ──────────────────────────────────────────
     * Simulates server power draw oscillating between 120–260 W.
     * Replace with the RAPL MSR / IPMI read described above.
     */
    static long long call_count = 0;
    ++call_count;

    out_watts = 120.0 + static_cast<double>((call_count * 11) % 140);
    return true;
}


// ═══════════════════════════════════════════════════════════════════════
//  Stage 1 — nativeReadCpuTemperature
// ═══════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jdouble JNICALL
Java_SystemHealthReport_nativeReadCpuTemperature(JNIEnv* env, jobject /*self*/)
{
    double      temp_c  = 0.0;
    std::string err_msg;

    if (!hw_read_cpu_temperature(temp_c, err_msg)) {
        throwMetricEx(env, "CPU_TEMPERATURE",
                      ("Failed to read CPU temperature sensor: " + err_msg).c_str());
        return static_cast<jdouble>(0.0);
    }

    // Range guard: sensor may return garbage on hardware fault
    if (temp_c < CPU_TEMP_MIN_C || temp_c > CPU_TEMP_MAX_C) {
        char buf[128];
        std::snprintf(buf, sizeof(buf),
            "CPU temperature %.1f°C is outside the valid physical range [%.0f, %.0f]",
            temp_c, CPU_TEMP_MIN_C, CPU_TEMP_MAX_C);
        throwMetricEx(env, "CPU_TEMPERATURE", buf);
        return static_cast<jdouble>(0.0);
    }

    return static_cast<jdouble>(temp_c);
}


// ═══════════════════════════════════════════════════════════════════════
//  Stage 2 — nativeReadHardwareInterrupts
// ═══════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jlong JNICALL
Java_SystemHealthReport_nativeReadHardwareInterrupts(JNIEnv* env, jobject /*self*/)
{
    long long   irq_count = 0;
    std::string err_msg;

    if (!hw_read_hardware_interrupts(irq_count, err_msg)) {
        throwMetricEx(env, "HARDWARE_INTERRUPTS",
                      ("Failed to read hardware interrupt counter: " + err_msg).c_str());
        return static_cast<jlong>(0);
    }

    if (irq_count < 0) {
        throwMetricEx(env, "HARDWARE_INTERRUPTS",
                      "Hardware interrupt counter returned a negative value");
        return static_cast<jlong>(0);
    }

    return static_cast<jlong>(irq_count);
}


// ═══════════════════════════════════════════════════════════════════════
//  Stage 3 — nativeReadPowerConsumption
// ═══════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jdouble JNICALL
Java_SystemHealthReport_nativeReadPowerConsumption(JNIEnv* env, jobject /*self*/)
{
    double      watts   = 0.0;
    std::string err_msg;

    if (!hw_read_power_consumption(watts, err_msg)) {
        throwMetricEx(env, "POWER_CONSUMPTION",
                      ("Failed to read power consumption sensor: " + err_msg).c_str());
        return static_cast<jdouble>(0.0);
    }

    if (watts < POWER_MIN_W || watts > POWER_MAX_W) {
        char buf[128];
        std::snprintf(buf, sizeof(buf),
            "Power reading %.1fW is outside the valid range [%.0f, %.0f]",
            watts, POWER_MIN_W, POWER_MAX_W);
        throwMetricEx(env, "POWER_CONSUMPTION", buf);
        return static_cast<jdouble>(0.0);
    }

    return static_cast<jdouble>(watts);
}