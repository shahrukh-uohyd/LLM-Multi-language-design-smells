// src/java/SensorInspector.java
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SensorInspector {
    static {
        try {
            System.loadLibrary("sensorinspector"); // Load native library
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native sensor inspector library failed to load: " + e.getMessage());
        }
    }
    
    // Native method declarations
    public native String[] findExceedingReadings(SensorReading[] readings, 
                                                SensorThreshold[] thresholds);
    
    public native double[] analyzeMinMaxValues(SensorReading[] readings);
    
    public native String[] identifyCriticalReadings(SensorReading[] readings, 
                                                   double criticalThreshold, 
                                                   String unit);
    
    // Wrapper methods for easier usage
    public List<SensorReading> getExceedingReadings(SensorReading[] readings, 
                                                   SensorThreshold[] thresholds) {
        String[] exceedingJson = findExceedingReadings(readings, thresholds);
        List<SensorReading> result = new ArrayList<>();
        
        // In a real implementation, you would parse the JSON strings back to objects
        // For demonstration, we'll just return the raw string representations
        for (String json : exceedingJson) {
            if (json != null) {
                result.add(new SensorReading(json, 0.0, "", LocalDateTime.now()));
            }
        }
        
        return result;
    }
    
    public double[] getOverallMinMax(SensorReading[] readings) {
        return analyzeMinMaxValues(readings);
    }
    
    public List<SensorReading> getCriticalReadings(SensorReading[] readings, 
                                                  double criticalThreshold, 
                                                  String unit) {
        String[] criticalJson = identifyCriticalReadings(readings, criticalThreshold, unit);
        List<SensorReading> result = new ArrayList<>();
        
        for (String json : criticalJson) {
            if (json != null) {
                result.add(new SensorReading(json, 0.0, "", LocalDateTime.now()));
            }
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        SensorInspector inspector = new SensorInspector();
        
        // Create sample sensor readings (simulating batch of sensor data)
        SensorReading[] readings = new SensorReading[100];
        for (int i = 0; i < 100; i++) {
            String sensorId = "TEMP_" + (i % 10);
            double value = Math.random() * 100; // Random temperature readings
            String unit = "°C";
            LocalDateTime time = LocalDateTime.now().minusMinutes(i);
            
            readings[i] = new SensorReading(sensorId, value, unit, time);
        }
        
        // Create sample thresholds
        SensorThreshold[] thresholds = {
            new SensorThreshold("TEMP_", 0.0, 80.0, "°C"),
            new SensorThreshold("HUM_", 0.0, 90.0, "%")
        };
        
        // Find exceeding readings
        String[] exceedingReadings = inspector.findExceedingReadings(readings, thresholds);
        System.out.println("Number of exceeding readings: " + exceedingReadings.length);
        
        // Analyze min/max values
        double[] minMax = inspector.analyzeMinMaxValues(readings);
        System.out.println("Min value: " + minMax[0]);
        System.out.println("Max value: " + minMax[1]);
        
        // Identify critical readings
        String[] criticalReadings = inspector.identifyCriticalReadings(readings, 90.0, "°C");
        System.out.println("Number of critical readings: " + criticalReadings.length);
    }
}