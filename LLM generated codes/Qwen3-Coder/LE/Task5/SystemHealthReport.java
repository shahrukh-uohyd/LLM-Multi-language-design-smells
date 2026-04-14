/**
 * Represents a comprehensive health report of the system
 */
public class SystemHealthReport {
    private HardwareMetrics metrics;
    private SystemLoadStatus loadStatus;
    private String statusDescription;
    private long reportGenerationTime;
    
    public SystemHealthReport(HardwareMetrics metrics, SystemLoadStatus loadStatus, 
                             String statusDescription, long reportGenerationTime) {
        this.metrics = metrics;
        this.loadStatus = loadStatus;
        this.statusDescription = statusDescription;
        this.reportGenerationTime = reportGenerationTime;
    }
    
    public HardwareMetrics getMetrics() {
        return metrics;
    }
    
    public SystemLoadStatus getLoadStatus() {
        return loadStatus;
    }
    
    public String getStatusDescription() {
        return statusDescription;
    }
    
    public long getReportGenerationTime() {
        return reportGenerationTime;
    }
    
    @Override
    public String toString() {
        return String.format(
            "System Health Report:\n" +
            "  CPU Temperature: %.2f°C\n" +
            "  Active Interrupts: %d\n" +
            "  Power Consumption: %.2fW\n" +
            "  Load Status: %s\n" +
            "  Status Description: %s\n" +
            "  Report Time: %d",
            metrics.getCpuTemperature(),
            metrics.getActiveInterrupts(),
            metrics.getPowerConsumption(),
            loadStatus.getDisplayName(),
            statusDescription,
            reportGenerationTime
        );
    }
}