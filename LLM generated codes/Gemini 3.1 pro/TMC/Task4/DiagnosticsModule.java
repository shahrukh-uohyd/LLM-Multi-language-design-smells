public class DiagnosticsModule {

    static {
        System.loadLibrary("warehouse_robot_native");
    }

    // Native declarations for hardware telemetry
    private native float checkBatteryVoltage();
    private native float getInternalTemperature();

    /**
     * Runs a health check to determine if the robot needs to return to the charging dock
     * or shut down to prevent hardware damage.
     */
    public boolean runHealthCheck() {
        System.out.println("Running system diagnostics...");
        
        float currentVoltage = checkBatteryVoltage();
        float coreTemp = getInternalTemperature();
        
        System.out.println("Battery: " + currentVoltage + "V | Core Temp: " + coreTemp + "°C");
        
        if (coreTemp > 85.0f) {
            System.err.println("WARNING: Core temperature critical. Initiating thermal throttling.");
            return false;
        }
        
        if (currentVoltage < 22.5f) { // Assuming a 24V system
            System.err.println("WARNING: Battery voltage low. Routing to charging station.");
            return false;
        }
        
        return true; // Robot is healthy and ready for tasks
    }
}