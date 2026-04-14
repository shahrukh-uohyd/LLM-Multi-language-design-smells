public class SystemHealthReport {

    public final double cpuTemperature;
    public final long interruptCount;
    public final double powerWatts;
    public final LoadStatus loadStatus;

    public SystemHealthReport(double cpuTemperature,
                              long interruptCount,
                              double powerWatts,
                              LoadStatus loadStatus) {
        this.cpuTemperature = cpuTemperature;
        this.interruptCount = interruptCount;
        this.powerWatts = powerWatts;
        this.loadStatus = loadStatus;
    }

    @Override
    public String toString() {
        return "SystemHealthReport{" +
               "cpuTemperature=" + cpuTemperature +
               ", interruptCount=" + interruptCount +
               ", powerWatts=" + powerWatts +
               ", loadStatus=" + loadStatus +
               '}';
    }
}
