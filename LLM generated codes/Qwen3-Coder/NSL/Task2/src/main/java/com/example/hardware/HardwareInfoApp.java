// File: src/main/java/com/example/hardware/HardwareInfoApp.java
package com.example.hardware;

import java.util.Map;
import java.util.List;
import java.util.Properties;

/**
 * Main application demonstrating JNI-based hardware information retrieval
 */
public class HardwareInfoApp {
    private HardwareInfoReader reader;

    public HardwareInfoApp() {
        this.reader = new HardwareInfoReader();
    }

    /**
     * Displays CPU information
     */
    public void displayCpuInfo() {
        System.out.println("\n=== CPU Information ===");
        
        if (!reader.isNativeLibraryAvailable()) {
            System.out.println("Native library unavailable - using simulated data");
        }
        
        Map<String, Object> cpuInfo = reader.getCpuInfo();
        if (cpuInfo != null && !cpuInfo.isEmpty()) {
            System.out.println("CPU Name: " + cpuInfo.get("name"));
            System.out.println("Cores: " + cpuInfo.get("cores"));
            System.out.println("Architecture: " + cpuInfo.get("architecture"));
            System.out.println("Vendor: " + cpuInfo.get("vendor"));
        } else {
            System.out.println("Could not retrieve CPU information");
        }
    }

    /**
     * Displays memory information
     */
    public void displayMemoryInfo() {
        System.out.println("\n=== Memory Information ===");
        
        Map<String, Object> memoryInfo = reader.getMemoryInfo();
        if (memoryInfo != null && !memoryInfo.isEmpty()) {
            Long total = (Long) memoryInfo.get("total");
            Long available = (Long) memoryInfo.get("available");
            Long used = (Long) memoryInfo.get("used");
            
            System.out.println("Total Memory: " + formatBytes(total));
            System.out.println("Available Memory: " + formatBytes(available));
            System.out.println("Used Memory: " + formatBytes(used));
            
            if (total != null && total > 0) {
                double usagePercent = (double) used / total * 100;
                System.out.printf("Memory Usage: %.2f%%\n", usagePercent);
            }
        } else {
            System.out.println("Could not retrieve memory information");
        }
    }

    /**
     * Displays disk information
     */
    public void displayDiskInfo() {
        System.out.println("\n=== Disk Information ===");
        
        List<Map<String, Object>> disks = reader.getDiskInfo();
        if (disks != null && !disks.isEmpty()) {
            for (int i = 0; i < disks.size(); i++) {
                Map<String, Object> disk = disks.get(i);
                System.out.println("Disk " + (i + 1) + ":");
                System.out.println("  Device: " + disk.get("device"));
                System.out.println("  Mount Point: " + disk.get("mount_point"));
                System.out.println("  Total Space: " + formatBytes((Long) disk.get("total_space")));
                System.out.println("  Free Space: " + formatBytes((Long) disk.get("free_space")));
                System.out.println("  Type: " + disk.get("type"));
                
                Long totalSpace = (Long) disk.get("total_space");
                Long freeSpace = (Long) disk.get("free_space");
                if (totalSpace != null && totalSpace > 0) {
                    double usagePercent = (double) (totalSpace - freeSpace) / totalSpace * 100;
                    System.out.printf("  Disk Usage: %.2f%%\n", usagePercent);
                }
            }
        } else {
            System.out.println("Could not retrieve disk information");
        }
    }

    /**
     * Displays network information
     */
    public void displayNetworkInfo() {
        System.out.println("\n=== Network Information ===");
        
        List<Map<String, Object>> networks = reader.getNetworkInfo();
        if (networks != null && !networks.isEmpty()) {
            for (int i = 0; i < networks.size(); i++) {
                Map<String, Object> network = networks.get(i);
                System.out.println("Interface " + (i + 1) + ":");
                System.out.println("  Name: " + network.get("name"));
                System.out.println("  MAC Address: " + network.get("mac_address"));
                System.out.println("  IP Address: " + network.get("ip_address"));
                System.out.println("  Speed: " + network.get("speed_mbps") + " Mbps");
            }
        } else {
            System.out.println("Could not retrieve network information");
        }
    }

    /**
     * Displays GPU information
     */
    public void displayGpuInfo() {
        System.out.println("\n=== GPU Information ===");
        
        List<Map<String, Object>> gpus = reader.getGpuInfo();
        if (gpus != null && !gpus.isEmpty()) {
            for (int i = 0; i < gpus.size(); i++) {
                Map<String, Object> gpu = gpus.get(i);
                System.out.println("GPU " + (i + 1) + ":");
                System.out.println("  Name: " + gpu.get("name"));
                System.out.println("  Vendor: " + gpu.get("vendor"));
                System.out.println("  VRAM: " + gpu.get("vram_mb") + " MB");
            }
        } else {
            System.out.println("Could not retrieve GPU information");
        }
    }

    /**
     * Displays overall system information
     */
    public void displaySystemInfo() {
        System.out.println("\n=== System Information ===");
        
        Map<String, Object> systemInfo = reader.getSystemInfoSafely();
        
        if (systemInfo.containsKey("error")) {
            System.out.println("Error: " + systemInfo.get("error"));
            return;
        }
        
        System.out.println("Operating System: " + systemInfo.get("os"));
        System.out.println("Architecture: " + systemInfo.get("arch"));
        System.out.println("Java Version: " + systemInfo.get("java_version"));
        
        displayCpuInfo();
        displayMemoryInfo();
        displayDiskInfo();
        displayNetworkInfo();
        displayGpuInfo();
    }

    /**
     * Formats byte values into human-readable form
     * @param bytes number of bytes to format
     * @return formatted string with appropriate unit
     */
    private String formatBytes(Long bytes) {
        if (bytes == null) return "Unknown";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double size = bytes.doubleValue();
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * Performs a hardware compatibility check
     */
    public void performCompatibilityCheck() {
        System.out.println("\n=== Hardware Compatibility Check ===");
        
        Map<String, Object> systemInfo = reader.getSystemInfoSafely();
        
        if (systemInfo.containsKey("error")) {
            System.out.println("Cannot perform compatibility check: " + systemInfo.get("error"));
            return;
        }
        
        // Example compatibility checks
        Map<String, Object> memoryInfo = (Map<String, Object>) systemInfo.get("memory");
        Long totalMemory = memoryInfo != null ? (Long) memoryInfo.get("total") : 0L;
        
        if (totalMemory != null && totalMemory < 2L * 1024 * 1024 * 1024) { // Less than 2GB
            System.out.println("⚠️  Warning: System has less than 2GB RAM - may impact performance");
        } else {
            System.out.println("✓ Sufficient RAM detected");
        }
        
        // Check for multiple CPU cores
        Map<String, Object> cpuInfo = (Map<String, Object>) systemInfo.get("cpu");
        Integer cores = cpuInfo != null ? (Integer) cpuInfo.get("cores") : 1;
        
        if (cores != null && cores < 2) {
            System.out.println("⚠️  Warning: Single-core CPU detected - may impact performance");
        } else {
            System.out.println("✓ Multi-core CPU detected");
        }
        
        System.out.println("Compatibility check completed.");
    }

    public static void main(String[] args) {
        System.out.println("JNI-Based Hardware Information Reader");
        System.out.println("=====================================");
        
        HardwareInfoApp app = new HardwareInfoApp();
        
        // Check if native library is available
        if (!app.reader.isNativeLibraryAvailable()) {
            System.out.println("Note: Native library not available - displaying simulated data");
        }
        
        // Display comprehensive system information
        app.displaySystemInfo();
        
        // Perform compatibility check
        app.performCompatibilityCheck();
        
        System.out.println("\nApplication completed successfully.");
    }
}