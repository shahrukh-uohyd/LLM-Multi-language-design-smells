public final class HardwareMetrics {

    public double cpuTemperature;         // °C
    public double gpuTemperature;         // °C
    public double motherboardTemperature;// °C

    public int fan1Speed;                 // RPM
    public int fan2Speed;                 // RPM

    public double batteryVoltage;         // Volts

    @Override
    public String toString() {
        return "HardwareMetrics{" +
                "cpuTemperature=" + cpuTemperature +
                ", gpuTemperature=" + gpuTemperature +
                ", motherboardTemperature=" + motherboardTemperature +
                ", fan1Speed=" + fan1Speed +
                ", fan2Speed=" + fan2Speed +
                ", batteryVoltage=" + batteryVoltage +
                '}';
    }
}
