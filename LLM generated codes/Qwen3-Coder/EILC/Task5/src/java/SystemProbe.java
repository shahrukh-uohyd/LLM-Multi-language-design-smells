// SystemProbe.java
public class SystemProbe {
    static {
        System.loadLibrary("hardware_monitor"); // Load native library
    }
    
    private HardwareHandle hardwareHandle;
    
    public SystemProbe(HardwareHandle hardwareHandle) {
        this.hardwareHandle = hardwareHandle;
        if (!hardwareHandle.isValid()) {
            throw new IllegalArgumentException("Invalid hardware handle provided");
        }
    }
    
    // Native method declarations
    public native double readCPUTemperature(long handle);
    public native double readGPUTemperature(long handle);
    public native double readMotherboardTemperature(long handle);
    public native int readFan1Speed(long handle);
    public native int readFan2Speed(long handle);
    public native double readBatteryVoltage(long handle);
    
    /**
     * Performs a deep scan of all hardware metrics using the HardwareHandle
     * @return HardwareMetrics object containing all sensor readings
     */
    public HardwareMetrics performDeepScan() {
        if (!hardwareHandle.isValid()) {
            throw new IllegalStateException("Hardware handle is invalid");
        }
        
        // Read all metrics using the hardware handle
        double cpuTemp = readCPUTemperature(hardwareHandle.getHandlePtr());
        double gpuTemp = readGPUTemperature(hardwareHandle.getHandlePtr());
        double mbTemp = readMotherboardTemperature(hardwareHandle.getHandlePtr());
        int fan1Speed = readFan1Speed(hardwareHandle.getHandlePtr());
        int fan2Speed = readFan2Speed(hardwareHandle.getHandlePtr());
        double batteryVoltage = readBatteryVoltage(hardwareHandle.getHandlePtr());
        
        return new HardwareMetrics(cpuTemp, gpuTemp, mbTemp, fan1Speed, fan2Speed, batteryVoltage);
    }
    
    /**
     * Alternative implementation: Single native call for all metrics (more efficient)
     */
    public native HardwareMetrics performDeepScanAllAtOnce(long handle);
    
    /**
     * Convenience method that uses the optimized single-call approach
     */
    public HardwareMetrics performDeepScanOptimized() {
        if (!hardwareHandle.isValid()) {
            throw new IllegalStateException("Hardware handle is invalid");
        }
        
        return performDeepScanAllAtOnce(hardwareHandle.getHandlePtr());
    }
    
    public HardwareHandle getHardwareHandle() {
        return hardwareHandle;
    }
    
    /**
     * Checks if all temperatures are within safe limits
     */
    public boolean areTemperaturesSafe() {
        HardwareMetrics metrics = performDeepScan();
        
        // Define safe temperature thresholds
        final double MAX_CPU_TEMP = 80.0;
        final double MAX_GPU_TEMP = 85.0;
        final double MAX_MB_TEMP = 50.0;
        
        return metrics.getCpuTemperature() <= MAX_CPU_TEMP &&
               metrics.getGpuTemperature() <= MAX_GPU_TEMP &&
               metrics.getMotherboardTemperature() <= MAX_MB_TEMP;
    }
    
    /**
     * Checks if all fans are operating within normal range
     */
    public boolean areFansOperating() {
        HardwareMetrics metrics = performDeepScan();
        
        // Define minimum fan speeds (RPM) - adjust based on your hardware
        final int MIN_FAN_SPEED = 500;
        final int MAX_FAN_SPEED = 5000;
        
        return metrics.getFan1Speed() >= MIN_FAN_SPEED && metrics.getFan1Speed() <= MAX_FAN_SPEED &&
               metrics.getFan2Speed() >= MIN_FAN_SPEED && metrics.getFan2Speed() <= MAX_FAN_SPEED;
    }
}