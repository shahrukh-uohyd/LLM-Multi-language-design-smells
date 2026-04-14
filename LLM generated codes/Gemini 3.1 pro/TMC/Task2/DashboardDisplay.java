public class DashboardDisplay {

    static {
        System.loadLibrary("infotainment_native");
    }

    // Native declarations for vehicle telemetry
    private native float readFuelLevel();
    private native float readEngineTemperature();

    /**
     * Called periodically (e.g., by a UI rendering loop) to fetch 
     * real-time vehicle data and update the dashboard UI.
     */
    public void refreshDisplay() {
        System.out.println("Refreshing dashboard telemetry...");
        
        // Fetch values via JNI
        float fuelPercentage = readFuelLevel();
        float engineTempCelsius = readEngineTemperature();
        
        // Update UI components (simulated here with console output)
        if (fuelPercentage < 10.0f) {
            System.err.println("WARNING: Low Fuel! Current level: " + fuelPercentage + "%");
        } else {
            System.out.println("Fuel Level: " + fuelPercentage + "%");
        }
        
        if (engineTempCelsius > 110.0f) {
            System.err.println("CRITICAL: Engine Overheating! Temp: " + engineTempCelsius + "°C");
        } else {
            System.out.println("Engine Temp: " + engineTempCelsius + "°C");
        }
    }
}