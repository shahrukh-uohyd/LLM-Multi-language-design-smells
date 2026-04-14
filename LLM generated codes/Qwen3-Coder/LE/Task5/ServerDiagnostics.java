/**
 * Main class implementing system health reporting using native hardware access
 */
public class ServerDiagnostics {
    static {
        System.loadLibrary("serverdiagnostics");
    }
    
    // Native method declarations
    private native double readCpuCoreTemperatureNative();
    private native int fetchActiveHardwareInterruptsNative();
    private native double retrievePowerConsumptionNative();
    
    /**
     * Generates a comprehensive system health report
     */
    public SystemHealthReport generateSystemHealthReport() {
        long startTime = System.nanoTime();
        
        try {
            // Step 1: Reading the current CPU core temperature
            double cpuTemp = readCpuCoreTemperatureNative();
            
            // Step 2: Fetching the total number of active hardware interrupts
            int activeInterrupts = fetchActiveHardwareInterruptsNative();
            
            // Step 3: Retrieving the current power consumption in watts
            double powerConsumption = retrievePowerConsumptionNative();
            
            // Create hardware metrics object
            HardwareMetrics metrics = new HardwareMetrics(cpuTemp, activeInterrupts, powerConsumption);
            
            // Determine system load status based on all three metrics
            SystemLoadStatus loadStatus = determineLoadStatus(metrics);
            String statusDescription = generateStatusDescription(loadStatus, metrics);
            
            long endTime = System.nanoTime();
            return new SystemHealthReport(metrics, loadStatus, statusDescription, endTime - startTime);
            
        } catch (Exception e) {
            long endTime = System.nanoTime();
            // Return error report in case of exception
            HardwareMetrics errorMetrics = new HardwareMetrics(-1.0, -1, -1.0);
            return new SystemHealthReport(errorMetrics, SystemLoadStatus.CRITICAL, 
                "Exception during health check: " + e.getMessage(), endTime - startTime);
        }
    }
    
    /**
     * Determines the system load status based on hardware metrics
     */
    private SystemLoadStatus determineLoadStatus(HardwareMetrics metrics) {
        double temp = metrics.getCpuTemperature();
        int interrupts = metrics.getActiveInterrupts();
        double power = metrics.getPowerConsumption();
        
        // Define thresholds for different status levels
        final double TEMP_WARNING_THRESHOLD = 70.0;   // degrees Celsius
        final double TEMP_CRITICAL_THRESHOLD = 85.0;  // degrees Celsius
        final int INTERRUPTS_WARNING_THRESHOLD = 500;  // active interrupts
        final int INTERRUPTS_CRITICAL_THRESHOLD = 1000; // active interrupts
        final double POWER_WARNING_THRESHOLD = 150.0;  // watts
        final double POWER_CRITICAL_THRESHOLD = 200.0; // watts
        
        // Check for critical conditions first
        if (temp >= TEMP_CRITICAL_THRESHOLD || 
            interrupts >= INTERRUPTS_CRITICAL_THRESHOLD || 
            power >= POWER_CRITICAL_THRESHOLD) {
            return SystemLoadStatus.CRITICAL;
        }
        
        // Check for warning conditions
        if (temp >= TEMP_WARNING_THRESHOLD || 
            interrupts >= INTERRUPTS_WARNING_THRESHOLD || 
            power >= POWER_WARNING_THRESHOLD) {
            return SystemLoadStatus.WARNING;
        }
        
        // If no thresholds exceeded, system is normal
        return SystemLoadStatus.NORMAL;
    }
    
    /**
     * Generates a descriptive message for the current status
     */
    private String generateStatusDescription(SystemLoadStatus status, HardwareMetrics metrics) {
        switch (status) {
            case NORMAL:
                return "System operating within normal parameters";
            case WARNING:
                StringBuilder warningDesc = new StringBuilder("Warning: ");
                
                if (metrics.getCpuTemperature() >= 70.0) {
                    warningDesc.append("High CPU temperature (").append(String.format("%.1f°C", metrics.getCpuTemperature())).append("); ");
                }
                if (metrics.getActiveInterrupts() >= 500) {
                    warningDesc.append("High interrupt activity (").append(metrics.getActiveInterrupts()).append("); ");
                }
                if (metrics.getPowerConsumption() >= 150.0) {
                    warningDesc.append("High power consumption (").append(String.format("%.1fW", metrics.getPowerConsumption())).append("); ");
                }
                
                return warningDesc.toString().trim();
            case CRITICAL:
                StringBuilder criticalDesc = new StringBuilder("CRITICAL: ");
                
                if (metrics.getCpuTemperature() >= 85.0) {
                    criticalDesc.append("Critical CPU temperature (").append(String.format("%.1f°C", metrics.getCpuTemperature())).append("); ");
                }
                if (metrics.getActiveInterrupts() >= 1000) {
                    criticalDesc.append("Critical interrupt activity (").append(metrics.getActiveInterrupts()).append("); ");
                }
                if (metrics.getPowerConsumption() >= 200.0) {
                    criticalDesc.append("Critical power consumption (").append(String.format("%.1fW", metrics.getPowerConsumption())).append("); ");
                }
                
                return criticalDesc.toString().trim();
            default:
                return "Unknown status";
        }
    }
}