public class SensorApplication {
    public static void main(String[] args) {
        SensorDeviceManager sensorManager = new SensorDeviceManager();
        
        try {
            // Initialize all sensors
            if (sensorManager.initializeAllSensors()) {
                System.out.println("All sensors initialized successfully");
                
                // Read sensor values
                double temp = sensorManager.getTemperature();
                double pressure = sensorManager.getPressure();
                double humidity = sensorManager.getHumidity();
                
                System.out.printf("Temperature: %.2f°C%n", temp);
                System.out.printf("Pressure: %.2f hPa%n", pressure);
                System.out.printf("Humidity: %.2f%%%n", humidity);
            } else {
                System.err.println("Failed to initialize sensors");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            // Always clean up resources
            sensorManager.cleanup();
        }
    }
}