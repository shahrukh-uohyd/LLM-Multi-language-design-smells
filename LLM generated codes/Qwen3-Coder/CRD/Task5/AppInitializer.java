// AppInitializer.java
import java.util.logging.Logger;
import java.util.logging.Level;

public class AppInitializer {
    private static final Logger logger = Logger.getLogger(AppInitializer.class.getName());
    private ConfigurationManager configManager;
    private SystemMonitor systemMonitor;

    public AppInitializer() {
        this.configManager = new ConfigurationManager();
        this.systemMonitor = new SystemMonitor();
    }

    /**
     * Initializes the application with configuration
     */
    public boolean initializeApplication(String configPath) {
        try {
            System.out.println("Starting application initialization...");
            
            // Initialize system monitor
            System.out.println(systemMonitor.getFormattedSystemInfo());
            
            // Initialize configuration manager
            boolean configInit = configManager.initialize(configPath);
            if (!configInit) {
                System.err.println("Failed to initialize configuration manager");
                return false;
            }
            
            // Configure application based on system resources
            configureBasedOnSystemResources();
            
            // Set up logging based on configuration
            setupLogging();
            
            // Validate critical configurations
            if (!validateCriticalConfigs()) {
                System.err.println("Critical configuration validation failed");
                return false;
            }
            
            System.out.println("Application initialized successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Application initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Configures application settings based on system resources
     */
    private void configureBasedOnSystemResources() {
        ConfigurationManager.SystemInfoForConfig sysInfo = configManager.getSystemInfoForConfig();
        
        // Adjust cache size based on available memory
        long availableMemory = sysInfo.freeMemory;
        if (availableMemory < 512 * 1024 * 1024) { // Less than 512MB
            configManager.setConfigValue("cache.size", "500");
            configManager.setConfigValue("cache.enabled", "false");
        } else if (availableMemory < 2 * 1024 * 1024 * 1024) { // Less than 2GB
            configManager.setConfigValue("cache.size", "1000");
        } else { // More than 2GB
            configManager.setConfigValue("cache.size", "5000");
        }
        
        // Adjust thread pool based on CPU cores
        if (sysInfo.cpuUsage > 80.0) {
            configManager.setConfigValue("thread.pool.size", "4");
        } else {
            configManager.setConfigValue("thread.pool.size", "8");
        }
        
        System.out.println("Configuration adjusted based on system resources");
    }

    /**
     * Sets up logging based on configuration
     */
    private void setupLogging() {
        String logLevel = configManager.getConfigValue("logging.level");
        if (logLevel != null) {
            Level level = Level.INFO;
            switch (logLevel.toUpperCase()) {
                case "DEBUG":
                    level = Level.FINEST;
                    break;
                case "INFO":
                    level = Level.INFO;
                    break;
                case "WARN":
                    level = Level.WARNING;
                    break;
                case "ERROR":
                    level = Level.SEVERE;
                    break;
            }
            logger.setLevel(level);
            System.out.println("Logging configured with level: " + logLevel);
        }
    }

    /**
     * Validates critical configuration values
     */
    private boolean validateCriticalConfigs() {
        boolean valid = true;
        
        // Check database URL
        String dbUrl = configManager.getConfigValue("database.url");
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            System.err.println("Critical error: Database URL not configured");
            valid = false;
        }
        
        // Check server port
        String portStr = configManager.getConfigValue("server.port");
        if (portStr != null) {
            try {
                int port = Integer.parseInt(portStr);
                if (port < 1 || port > 65535) {
                    System.err.println("Critical error: Invalid server port: " + port);
                    valid = false;
                }
            } catch (NumberFormatException e) {
                System.err.println("Critical error: Invalid server port format: " + portStr);
                valid = false;
            }
        }
        
        return valid;
    }

    /**
     * Starts the application services
     */
    public boolean startServices() {
        if (!configManager.isInitialized()) {
            System.err.println("Cannot start services: Configuration manager not initialized");
            return false;
        }
        
        try {
            String serverPort = configManager.getConfigValue("server.port");
            String serverHost = configManager.getConfigValue("server.host");
            
            System.out.println("Starting services on " + serverHost + ":" + serverPort);
            
            // Simulate starting services
            // In real application, this would start actual server components
            
            System.out.println("Services started successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to start services: " + e.getMessage());
            return false;
        }
    }

    /**
     * Shuts down the application gracefully
     */
    public boolean shutdownApplication() {
        try {
            System.out.println("Shutting down application...");
            
            // Save current configuration state
            configManager.saveConfiguration();
            
            System.out.println("Application shut down successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            return false;
        }
    }

    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    public SystemMonitor getSystemMonitor() {
        return systemMonitor;
    }
}