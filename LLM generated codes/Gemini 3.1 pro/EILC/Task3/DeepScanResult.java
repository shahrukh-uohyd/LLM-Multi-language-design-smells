public class DeepScanResult {
    public final float cpuTemperature;
    public final float gpuTemperature;
    public final float motherboardTemperature;
    public final int fan1Speed;
    public final int fan2Speed;
    public final float batteryVoltage;

    public DeepScanResult(float cpuTemperature, float gpuTemperature, float motherboardTemperature, 
                          int fan1Speed, int fan2Speed, float batteryVoltage) {
        this.cpuTemperature = cpuTemperature;
        this.gpuTemperature = gpuTemperature;
        this.motherboardTemperature = motherboardTemperature;
        this.fan1Speed = fan1Speed;
        this.fan2Speed = fan2Speed;
        this.batteryVoltage = batteryVoltage;
    }

    @Override
    public String toString() {
        return String.format("ScanResult [CPU: %.1f°C, GPU: %.1f°C, Mobo: %.1f°C, Fan1: %d RPM, Fan2: %d RPM, Batt: %.2fV]",
                cpuTemperature, gpuTemperature, motherboardTemperature, fan1Speed, fan2Speed, batteryVoltage);
    }
}