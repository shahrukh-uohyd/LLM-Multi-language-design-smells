// src/java/DeviceFailureAnalyzer.java
import java.time.LocalDateTime;
import java.util.*;

public class DeviceFailureAnalyzer {
    static {
        try {
            System.loadLibrary("devicemonitor"); // Load native library
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native device monitor library failed to load: " + e.getMessage());
        }
    }
    
    // Native method declarations
    public native String[] identifyRepeatedFailures(DeviceStatusUpdate[] updates, 
                                                  int failureThreshold, 
                                                  long timeWindowMinutes);
    
    public native Map<String, Integer> analyzeDeviceFailureFrequency(DeviceStatusUpdate[] updates);
    
    public native String[] getDevicesWithRecentFailures(DeviceStatusUpdate[] updates, 
                                                       long minutesBack);
    
    // Wrapper methods for easier usage
    public List<String> findConsistentlyFailingDevices(DeviceStatusUpdate[] updates, 
                                                      int failureThreshold, 
                                                      long timeWindowMinutes) {
        String[] failingDevices = identifyRepeatedFailures(updates, failureThreshold, timeWindowMinutes);
        return Arrays.asList(failingDevices);
    }
    
    public Map<String, Integer> getFailureFrequencyPerDevice(DeviceStatusUpdate[] updates) {
        return analyzeDeviceFailureFrequency(updates);
    }
    
    public List<String> getRecentlyFailingDevices(DeviceStatusUpdate[] updates, 
                                                 long minutesBack) {
        String[] recentlyFailing = getDevicesWithRecentFailures(updates, minutesBack);
        return Arrays.asList(recentlyFailing);
    }
    
    public static void main(String[] args) {
        DeviceFailureAnalyzer analyzer = new DeviceFailureAnalyzer();
        
        // Create sample device status updates (simulating batch of updates)
        DeviceStatusUpdate[] updates = new DeviceStatusUpdate[1000];
        for (int i = 0; i < 1000; i++) {
            String deviceId = "DEVICE_" + (i % 50); // 50 different devices
            int statusCode = i % 4; // Cycle through 0,1,2,3 (HEALTHY, WARNING, FAILURE, CRITICAL)
            String description = "Status update " + i;
            LocalDateTime time = LocalDateTime.now().minusMinutes(i);
            
            updates[i] = new DeviceStatusUpdate(deviceId, statusCode, description, time);
        }
        
        // Identify devices with repeated failures
        List<String> consistentlyFailing = analyzer.findConsistentlyFailingDevices(updates, 3, 60);
        System.out.println("Consistently failing devices (3+ failures in 60 min):");
        for (String device : consistentlyFailing) {
            System.out.println("  " + device);
        }
        
        // Analyze failure frequency
        Map<String, Integer> failureFreq = analyzer.getFailureFrequencyPerDevice(updates);
        System.out.println("\nTop 5 devices by failure count:");
        failureFreq.entrySet().stream()
                  .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                  .limit(5)
                  .forEach(entry -> 
                      System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " failures"));
        
        // Get recent failures
        List<String> recentFailures = analyzer.getRecentlyFailingDevices(updates, 30);
        System.out.println("\nDevices with failures in last 30 minutes: " + recentFailures.size());
    }
}