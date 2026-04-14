public final class SystemProbe {

    static {
        System.loadLibrary("hardware_monitor"); // libhardware_monitor.so / .dll
    }

    private final HardwareHandle hardwareHandle;

    public SystemProbe(HardwareHandle hardwareHandle) {
        if (hardwareHandle == null) {
            throw new IllegalArgumentException("HardwareHandle must not be null");
        }
        this.hardwareHandle = hardwareHandle;
    }

    /**
     * Performs a deep hardware scan using kernel-backed native calls.
     */
    public HardwareSnapshot performDeepScan() {
        long handle = hardwareHandle.getNativeHandle();

        double cpuTemp = getCpuTemperature(handle);
        double gpuTemp = getGpuTemperature(handle);
        double motherboardTemp = getMotherboardTemperature(handle);
        int fan1Rpm = getFanSpeed(handle, 1);
        int fan2Rpm = getFanSpeed(handle, 2);
        double batteryVoltage = getBatteryVoltage(handle);

        return new HardwareSnapshot(
            cpuTemp,
            gpuTemp,
            motherboardTemp,
            fan1Rpm,
            fan2Rpm,
            batteryVoltage
        );
    }

    /* =======================
       Native method bindings
       ======================= */

    private static native double getCpuTemperature(long nativeHandle);

    private static native double getGpuTemperature(long nativeHandle);

    private static native double getMotherboardTemperature(long nativeHandle);

    private static native int getFanSpeed(long nativeHandle, int fanIndex);

    private static native double getBatteryVoltage(long nativeHandle);
}
