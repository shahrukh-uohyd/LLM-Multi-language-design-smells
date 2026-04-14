package com.example.diagnostics;

public class SystemDiagnostics {

    // Load the native library containing the JNI C++ implementations
    static {
        System.loadLibrary("sys_diagnostics_lib");
    }

    /**
     * Data class representing the generated health report.
     */
    public static class SystemHealthReport {
        public final double cpuTemperatureCelsius;
        public final long activeInterrupts;
        public final double powerConsumptionWatts;
        public final String loadStatus;

        public SystemHealthReport(double temp, long interrupts, double power, String status) {
            this.cpuTemperatureCelsius = temp;
            this.activeInterrupts = interrupts;
            this.powerConsumptionWatts = power;
            this.loadStatus = status;
        }

        @Override
        public String toString() {
            return String.format(
                "SystemHealthReport [Status: %s | Temp: %.1f°C | Power: %.1fW | Interrupts: %d]",
                loadStatus, cpuTemperatureCelsius, powerConsumptionWatts, activeInterrupts
            );
        }
    }

    /**
     * Gathers hardware metrics and determines the current load status.
     * 
     * @return A populated SystemHealthReport.
     */
    public SystemHealthReport generateHealthReport() {
        // 1. Read the current CPU core temperature
        double temperature = readCpuTemperature();

        // 2. Fetch the total number of active hardware interrupts
        long interrupts = fetchActiveInterrupts();

        // 3. Retrieve the current power consumption in watts
        double power = retrievePowerConsumption();

        // 4. Determine load status based on the gathered metrics
        String status = determineLoadStatus(temperature, interrupts, power);

        return new SystemHealthReport(temperature, interrupts, power, status);
    }

    /**
     * Evaluates the metrics to determine if the system is NORMAL, HIGH_LOAD, or CRITICAL.
     */
    private String determineLoadStatus(double temp, long interrupts, double power) {
        if (temp >= 85.0 || power >= 500.0 || interrupts >= 100000) {
            return "CRITICAL";
        } else if (temp >= 70.0 || power >= 350.0 || interrupts >= 50000) {
            return "HIGH_LOAD";
        }
        return "NORMAL";
    }

    // --- Native Method Declarations ---
    
    // Reads current CPU core temperature in Celsius
    private native double readCpuTemperature();
    
    // Fetches the current active hardware interrupt count
    private native long fetchActiveInterrupts();
    
    // Retrieves the current power draw of the system in Watts
    private native double retrievePowerConsumption();
}