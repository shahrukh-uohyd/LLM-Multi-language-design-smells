public final class HardwareSnapshot {

    private final double cpuTemperature;
    private final double gpuTemperature;
    private final double motherboardTemperature;
    private final int fan1Rpm;
    private final int fan2Rpm;
    private final double batteryVoltage;

    public HardwareSnapshot(
            double cpuTemperature,
            double gpuTemperature,
            double motherboardTemperature,
            int fan1Rpm,
            int fan2Rpm,
            double batteryVoltage) {

        this.cpuTemperature = cpuTemperature;
        this.gpuTemperature = gpuTemperature;
        this.motherboardTemperature = motherboardTemperature;
        this.fan1Rpm = fan1Rpm;
        this.fan2Rpm = fan2Rpm;
        this.batteryVoltage = batteryVoltage;
    }

    public double getCpuTemperature() {
        return cpuTemperature;
    }

    public double getGpuTemperature() {
        return gpuTemperature;
    }

    public double getMotherboardTemperature() {
        return motherboardTemperature;
    }

    public int getFan1Rpm() {
        return fan1Rpm;
    }

    public int getFan2Rpm() {
        return fan2Rpm;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }
}
