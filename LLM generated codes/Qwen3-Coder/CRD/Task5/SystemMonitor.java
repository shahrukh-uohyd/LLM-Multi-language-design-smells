// SystemMonitor.java
import java.util.Map;
import java.util.HashMap;

public class SystemMonitor {
    static {
        System.loadLibrary("monitor_config");
    }

    // Existing native system monitoring methods
    public native long getFreeMemory();
    public native long getTotalMemory();
    public native double getCpuUsage();
    public native String getSystemLoadAverage();
    public native long getDiskUsage();
    public native String getNetworkStatus();
    public native int getRunningProcesses();
    public native String getSystemUptime();

    // New configuration management native methods
    public native boolean saveConfiguration(String configPath, String configData);
    public native String loadConfiguration(String configPath);
    public native boolean updateConfigurationValue(String configPath, String key, String value);
    public native String getConfigurationValue(String configPath, String key);
    public native String[] listConfigurationKeys(String configPath);
    public native boolean validateConfiguration(String configPath);
    public native boolean backupConfiguration(String configPath, String backupPath);
    public native boolean restoreConfiguration(String backupPath, String configPath);
    public native boolean encryptConfiguration(String configPath, String password);
    public native boolean decryptConfiguration(String configPath, String password);

    // Convenience methods for configuration management
    public Map<String, String> loadConfigurationAsMap(String configPath) {
        String configData = loadConfiguration(configPath);
        if (configData == null) {
            return new HashMap<>();
        }
        
        Map<String, String> configMap = new HashMap<>();
        String[] lines = configData.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue; // Skip comments and empty lines
            }
            
            int separatorIndex = line.indexOf('=');
            if (separatorIndex > 0) {
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();
                configMap.put(key, value);
            }
        }
        
        return configMap;
    }

    public boolean saveConfigurationFromMap(String configPath, Map<String, String> configMap) {
        StringBuilder configData = new StringBuilder();
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            configData.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        return saveConfiguration(configPath, configData.toString());
    }

    public boolean setConfigurationValue(String configPath, String key, String value) {
        return updateConfigurationValue(configPath, key, value);
    }

    public String getFormattedSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("System Monitor Information:\n");
        info.append("=============================\n");
        info.append("Total Memory: ").append(getTotalMemory()).append(" bytes\n");
        info.append("Free Memory: ").append(getFreeMemory()).append(" bytes\n");
        info.append("CPU Usage: ").append(String.format("%.2f", getCpuUsage())).append("%\n");
        info.append("Running Processes: ").append(getRunningProcesses()).append("\n");
        info.append("System Uptime: ").append(getSystemUptime()).append("\n");
        info.append("System Load Average: ").append(getSystemLoadAverage()).append("\n");
        info.append("Disk Usage: ").append(getDiskUsage()).append(" bytes\n");
        info.append("Network Status: ").append(getNetworkStatus()).append("\n");
        
        return info.toString();
    }

    public double getMemoryUtilizationPercentage() {
        long total = getTotalMemory();
        long free = getFreeMemory();
        if (total == 0) {
            return 0.0;
        }
        return ((double)(total - free) / total) * 100.0;
    }
}