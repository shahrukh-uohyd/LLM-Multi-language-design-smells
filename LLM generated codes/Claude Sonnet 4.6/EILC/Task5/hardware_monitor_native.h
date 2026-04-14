#pragma once
#include <cstdint>

// ── Sentinel values — must mirror ScanReport Java constants ──────────────────
#define HW_METRIC_UNAVAILABLE_F  (-1.0f)
#define HW_METRIC_UNAVAILABLE_I  (-1)

/**
 * Kernel hardware-monitor context.
 *
 * On Linux  : wraps open hwmon sysfs file-descriptors.
 * On Windows: wraps a WMI / OpenHardwareMonitor session handle.
 * On macOS  : wraps an IOKit service connection.
 */
struct HwMonitorContext {
    int  hwmonDirFd;        // hwmon sysfs directory fd (validity sentinel)
    bool valid;             // set to false on close

    // Sensor file-descriptors opened once; re-read via lseek(fd, 0, SEEK_SET)
    int  fdCpuTemp;         // /sys/class/hwmon/hwmon0/temp1_input  (millidegrees C)
    int  fdGpuTemp;         // /sys/class/hwmon/hwmon1/temp1_input
    int  fdMoboTemp;        // /sys/class/hwmon/hwmon0/temp2_input
    int  fdFan1;            // /sys/class/hwmon/hwmon0/fan1_input   (RPM)
    int  fdFan2;            // /sys/class/hwmon/hwmon0/fan2_input
    int  fdBatteryVoltage;  // /sys/class/power_supply/BAT0/voltage_now (microvolts)
};