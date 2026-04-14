// File: src/main/java/com/example/hardware/HardwareInfoReader.java
package com.example.hardware;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * JNI-based hardware information reader that securely retrieves system hardware details.
 * This class provides a safe interface to native code for accessing hardware information.
 */
public class HardwareInfoReader {
    private static boolean initialized = false;
    
    static {
        try {
            // Load native library securely
            loadNativeLibrary();
            initialized = true;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native library not available: " + e.getMessage());
            System.err.println("Using simulated data for demonstration purposes.");
        } catch (SecurityException e) {
            System.err.println("Security violation when loading native library: " + e.getMessage());
        }
    }

    /**
     * Loads the native library with security validation
     */
    private static void loadNativeLibrary() throws SecurityException {
        // Validate that we're allowed to load native libraries
        if (System.getSecurityManager() != null) {
            System.getSecurityManager().checkPermission(new RuntimePermission("loadLibrary.*"));
        }
        
        // Load the specific native library
        System.loadLibrary("hardwareinfo");
    }

    /**
     * Retrieves CPU information from native code
     * @return Map containing CPU details
     */
    public native Map<String, Object> getCpuInfo();

    /**
     * Retrieves memory information from native code
     * @return Map containing memory details
     */
    public native Map<String, Object> getMemoryInfo();

    /**
     * Retrieves disk information from native code
     * @return List of maps containing disk details
     */
    public native List<Map<String, Object>> getDiskInfo();

    /**
     * Retrieves network interface information from native code
     * @return List of maps containing network interface details
     */
    public native List<Map<String, Object>> getNetworkInfo();

    /**
     * Retrieves GPU information from native code
     * @return List of maps containing GPU details
     */
    public native List<Map<String, Object>> getGpuInfo();

    /**
     * Checks if the native library is loaded and accessible
     * @return true if native library is loaded and ready to use
     */
    public boolean isNativeLibraryAvailable() {
        return initialized;
    }

    /**
     * Retrieves comprehensive system information
     * @return Map containing all system information
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        
        if (!isNativeLibraryAvailable()) {
            // Return simulated data for demonstration
            return getSimulatedSystemInfo();
        }
        
        try {
            systemInfo.put("cpu", getCpuInfo());
            systemInfo.put("memory", getMemoryInfo());
            systemInfo.put("disks", getDiskInfo());
            systemInfo.put("network", getNetworkInfo());
            systemInfo.put("gpu", getGpuInfo());
            systemInfo.put("os", System.getProperty("os.name"));
            systemInfo.put("arch", System.getProperty("os.arch"));
            systemInfo.put("java_version", System.getProperty("java.version"));
        } catch (Exception e) {
            System.err.println("Error retrieving system information: " + e.getMessage());
            // Return what we can gather
            systemInfo.put("error", e.getMessage());
        }
        
        return systemInfo;
    }

    /**
     * Returns simulated system information when native library is unavailable
     * @return Map containing simulated system information
     */
    private Map<String, Object> getSimulatedSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        
        // Simulated CPU info
        Map<String, Object> cpuInfo = new HashMap<>();
        cpuInfo.put("name", "Simulated CPU");
        cpuInfo.put("cores", 4);
        cpuInfo.put("architecture", "x86_64");
        cpuInfo.put("vendor", "Simulated Vendor");
        systemInfo.put("cpu", cpuInfo);
        
        // Simulated memory info
        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("total", 8589934592L); // 8GB in bytes
        memoryInfo.put("available", 4294967296L); // 4GB in bytes
        memoryInfo.put("used", 4294967296L); // 4GB in bytes
        systemInfo.put("memory", memoryInfo);
        
        // Simulated disk info
        List<Map<String, Object>> disks = new ArrayList<>();
        Map<String, Object> disk1 = new HashMap<>();
        disk1.put("device", "/dev/sda1");
        disk1.put("mount_point", "/");
        disk1.put("total_space", 256000000000L); // 256GB
        disk1.put("free_space", 128000000000L); // 128GB
        disk1.put("type", "ext4");
        disks.add(disk1);
        systemInfo.put("disks", disks);
        
        // Simulated network info
        List<Map<String, Object>> networks = new ArrayList<>();
        Map<String, Object> net1 = new HashMap<>();
        net1.put("name", "eth0");
        net1.put("mac_address", "AA:BB:CC:DD:EE:FF");
        net1.put("ip_address", "192.168.1.100");
        net1.put("speed_mbps", 1000);
        networks.add(net1);
        systemInfo.put("network", networks);
        
        // Simulated GPU info
        List<Map<String, Object>> gpus = new ArrayList<>();
        Map<String, Object> gpu1 = new HashMap<>();
        gpu1.put("name", "Simulated GPU");
        gpu1.put("vendor", "Simulated Graphics");
        gpu1.put("vram_mb", 2048);
        gpus.add(gpu1);
        systemInfo.put("gpu", gpus);
        
        systemInfo.put("os", System.getProperty("os.name"));
        systemInfo.put("arch", System.getProperty("os.arch"));
        systemInfo.put("java_version", System.getProperty("java.version"));
        
        return systemInfo;
    }

    /**
     * Safely retrieves hardware information with error handling
     * @return Map containing hardware information or error details
     */
    public Map<String, Object> getSystemInfoSafely() {
        try {
            return getSystemInfo();
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to retrieve system information: " + e.getMessage());
            errorResult.put("stack_trace", e.getClass().getName());
            return errorResult;
        }
    }
}