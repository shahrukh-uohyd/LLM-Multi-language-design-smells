/**
 * Container for all hardware metrics collected from native layer
 */
public class HardwareMetrics {
    private double cpuTemperature; // in Celsius
    private int activeInterrupts;
    private double powerConsumption; // in watts
    private long timestamp;
    
    public HardwareMetrics(double cpuTemperature, int activeInterrupts, double powerConsumption) {
        this.cpuTemperature = cpuTemperature;
        this.activeInterrupts = activeInterrupts;
        this.powerConsumption = powerConsumption;
        this.timestamp = System.currentTimeMillis();
    }
    
    public double getCpuTemperature() {
        return cpuTemperature;
    }
    
    public int getActiveInterrupts() {
        return activeInterrupts;
    }
    
    public double getPowerConsumption() {
        return powerConsumption;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}