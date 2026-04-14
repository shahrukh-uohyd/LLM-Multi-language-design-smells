// HardwareMetrics.java
public class HardwareMetrics {
    private double cpuTemperature;
    private double gpuTemperature;
    private double motherboardTemperature;
    private int fan1Speed;  // RPM
    private int fan2Speed;  // RPM
    private double batteryVoltage;
    
    public HardwareMetrics(double cpuTemp, double gpuTemp, double mbTemp, 
                          int fan1Speed, int fan2Speed, double batteryVoltage) {
        this.cpuTemperature = cpuTemp;
        this.gpuTemperature = gpuTemp;
        this.motherboardTemperature = mbTemp;
        this.fan1Speed = fan1Speed;
        this.fan2Speed = fan2Speed;
        this.batteryVoltage = batteryVoltage;
    }
    
    // Getters
    public double getCpuTemperature() { return cpuTemperature; }
    public double getGpuTemperature() { return gpuTemperature; }
    public double getMotherboardTemperature() { return motherboardTemperature; }
    public int getFan1Speed() { return fan1Speed; }
    public int getFan2Speed() { return fan2Speed; }
    public double getBatteryVoltage() { return batteryVoltage; }
    
    @Override
    public String toString() {
        return String.format(
            "HardwareMetrics{\n" +
            "  CPU Temperature: %.2f°C\n" +
            "  GPU Temperature: %.2f°C\n" +
            "  Motherboard Temperature: %.2f°C\n" +
            "  Fan 1 Speed: %d RPM\n" +
            "  Fan 2 Speed: %d RPM\n" +
            "  Battery Voltage: %.2fV\n" +
            "}",
            cpuTemperature, gpuTemperature, motherboardTemperature,
            fan1Speed, fan2Speed, batteryVoltage
        );
    }
}