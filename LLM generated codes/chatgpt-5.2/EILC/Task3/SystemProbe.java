public final class SystemProbe {

    static {
        // libhardware_monitor.so / hardware_monitor.dll
        System.loadLibrary("hardware_monitor");
    }

    private final HardwareHandle hardwareHandle;

    public SystemProbe(HardwareHandle hardwareHandle) {
        if (hardwareHandle == null) {
            throw new IllegalArgumentException("HardwareHandle must not be null");
        }
        this.hardwareHandle = hardwareHandle;
    }

    /* -------- Native metric queries -------- */

    private static native double readCpuTemperature(long handle);
    private static native double readGpuTemperature(long handle);
    private static native double readMotherboardTemperature(long handle);

    private static native int readFan1Speed(long handle);   // RPM
    private static native int readFan2Speed(long handle);   // RPM

    private static native double readBatteryVoltage(long handle); // Volts

    /**
     * Performs a deep hardware scan and returns all six metrics.
     */
    public HardwareMetrics performDeepScan() {
        long handle = hardwareHandle.getNativeHandle();

        HardwareMetrics metrics = new HardwareMetrics();

        metrics.cpuTemperature          = readCpuTemperature(handle);
        metrics.gpuTemperature          = readGpuTemperature(handle);
        metrics.motherboardTemperature  = readMotherboardTemperature(handle);
        metrics.fan1Speed               = readFan1Speed(handle);
        metrics.fan2Speed               = readFan2Speed(handle);
        metrics.batteryVoltage          = readBatteryVoltage(handle);

        return metrics;
    }
}
