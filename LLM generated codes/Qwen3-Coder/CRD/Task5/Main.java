// Main.java - Example usage
public class Main {
    public static void main(String[] args) {
        AppInitializer initializer = new AppInitializer();
        
        System.out.println("=== Application Initialization Demo ===\n");
        
        // Initialize the application
        String configPath = "app.conf";
        boolean initSuccess = initializer.initializeApplication(configPath);
        
        if (initSuccess) {
            System.out.println("\n=== Configuration Management Demo ===");
            
            ConfigurationManager configMgr = initializer.getConfigManager();
            
            // Display current configuration
            System.out.println("Current configuration keys:");
            String[] keys = configMgr.getAllConfigKeys();
            for (String key : keys) {
                String value = configMgr.getConfigValue(key);
                System.out.println("  " + key + " = " + value);
            }
            
            // Update a configuration value
            System.out.println("\nUpdating server port...");
            configMgr.setConfigValue("server.port", "9090");
            System.out.println("New server port: " + configMgr.getConfigValue("server.port"));
            
            // Show system info for configuration
            ConfigurationManager.SystemInfoForConfig sysInfo = configMgr.getSystemInfoForConfig();
            System.out.println("\nSystem information for configuration:");
            System.out.println("  Total Memory: " + sysInfo.totalMemory + " bytes");
            System.out.println("  Free Memory: " + sysInfo.freeMemory + " bytes");
            System.out.println("  CPU Usage: " + String.format("%.2f%%", sysInfo.cpuUsage));
            System.out.println("  Running Processes: " + sysInfo.runningProcesses);
            
            // Create a backup
            System.out.println("\nCreating configuration backup...");
            boolean backupSuccess = configMgr.backupConfiguration("app.conf.backup");
            System.out.println("Backup created: " + backupSuccess);
            
            // Test encryption
            System.out.println("\nTesting configuration encryption...");
            boolean encryptSuccess = configMgr.encryptConfiguration("mySecretPassword123");
            System.out.println("Encryption success: " + encryptSuccess);
            
            // Test decryption
            System.out.println("Testing configuration decryption...");
            boolean decryptSuccess = configMgr.decryptConfiguration("mySecretPassword123");
            System.out.println("Decryption success: " + decryptSuccess);
            
            // Start services
            System.out.println("\nStarting application services...");
            boolean servicesStarted = initializer.startServices();
            System.out.println("Services started: " + servicesStarted);
            
            // Save configuration changes
            System.out.println("\nSaving configuration changes...");
            boolean saveSuccess = configMgr.saveConfiguration();
            System.out.println("Configuration saved: " + saveSuccess);
        } else {
            System.err.println("Application initialization failed!");
        }
        
        // Shutdown
        System.out.println("\nShutting down application...");
        initializer.shutdownApplication();
        
        System.out.println("\nDemo completed successfully!");
    }
}