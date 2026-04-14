#pragma once
#include <cstdint>

// Sentinel value returned when a sensor read fails
#define HW_METRIC_UNAVAILABLE  (-1.0f)
#define HW_FAN_UNAVAILABLE     (-1)

/**
 * Kernel hardware-monitor context.
 * On Linux this wraps an open hwmon sysfs directory fd.
 * On Windows this wraps a WMI/OpenHardwareMonitor session handle.
 */
struct HwMonitorContext {
    int  hwmonFd;          // hwmon sysfs directory file descriptor (Linux)
    bool valid;            // set to false on close

    // Cached sensor file-descriptors (opened once, reused per read)
    int  fdCpuTemp;
    int  fdGpuTemp;
    int  fdMoboTemp;
    int  fdFan1;
    int  fdFan2;
    int  fdBatteryVoltage;
};