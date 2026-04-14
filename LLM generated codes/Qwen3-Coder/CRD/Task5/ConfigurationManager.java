// ConfigurationManager.java
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;

public class ConfigurationManager {
    private SystemMonitor systemMonitor;
    private String activeConfigPath;
    private Properties currentConfig;
    private boolean isInitialized;

    public ConfigurationManager() {
        this.systemMonitor = new SystemMonitor();
        this.currentConfig = new Properties();
        this.isInitialized = false;
    }

    /**
     * Initializes the configuration manager with the specified config file
     */
    public boolean initialize(String configPath) {
        try {
            this.activeConfigPath = configPath;
            
            // Load configuration from file
            String configData = systemMonitor.loadConfiguration(configPath);
            if (configData == null) {
                System.out.println("Configuration file does not exist, creating default...");
                createDefaultConfiguration(configPath);
            } else {
                // Parse loaded configuration
                loadPropertiesFromString(configData);
            }
            
            // Validate configuration
            boolean isValid = systemMonitor.validateConfiguration(configPath);
            if (!isValid) {
                System.err.println("Warning: Configuration validation failed!");
            }
            
            isInitialized = true;
            System.out.println("Configuration manager initialized successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to initialize configuration manager: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets a configuration value by key
     */
    public String getConfigValue(String key) {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        // Try to get from current properties first
        String value = currentConfig.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // Fallback to native method
        return systemMonitor.getConfigurationValue(activeConfigPath, key);
    }

    /**
     * Sets a configuration value
     */
    public boolean setConfigValue(String key, String value) {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        currentConfig.setProperty(key, value);
        return systemMonitor.updateConfigurationValue(activeConfigPath, key, value);
    }

    /**
     * Gets all configuration keys
     */
    public String[] getAllConfigKeys() {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        return systemMonitor.listConfigurationKeys(activeConfigPath);
    }

    /**
     * Saves current configuration to file
     */
    public boolean saveConfiguration() {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        // Convert properties back to string format
        StringBuilder configBuilder = new StringBuilder();
        for (String key : currentConfig.stringPropertyNames()) {
            configBuilder.append(key).append("=").append(currentConfig.getProperty(key)).append("\n");
        }
        
        return systemMonitor.saveConfiguration(activeConfigPath, configBuilder.toString());
    }

    /**
     * Reloads configuration from file
     */
    public boolean reloadConfiguration() {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        String configData = systemMonitor.loadConfiguration(activeConfigPath);
        if (configData != null) {
            loadPropertiesFromString(configData);
            return true;
        }
        return false;
    }

    /**
     * Backups the current configuration
     */
    public boolean backupConfiguration(String backupPath) {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        return systemMonitor.backupConfiguration(activeConfigPath, backupPath);
    }

    /**
     * Restores configuration from backup
     */
    public boolean restoreConfiguration(String backupPath) {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        boolean restored = systemMonitor.restoreConfiguration(backupPath, activeConfigPath);
        if (restored) {
            reloadConfiguration(); // Reload into current properties
        }
        return restored;
    }

    /**
     * Encrypts the configuration file
     */
    public boolean encryptConfiguration(String password) {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        return systemMonitor.encryptConfiguration(activeConfigPath, password);
    }

    /**
     * Decrypts the configuration file
     */
    public boolean decryptConfiguration(String password) {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        return systemMonitor.decryptConfiguration(activeConfigPath, password);
    }

    /**
     * Gets configuration as a Map
     */
    public Map<String, String> getConfigurationAsMap() {
        if (!isInitialized) {
            throw new IllegalStateException("Configuration manager not initialized");
        }
        
        Map<String, String> configMap = new HashMap<>();
        for (String key : currentConfig.stringPropertyNames()) {
            configMap.put(key, currentConfig.getProperty(key));
        }
        return configMap;
    }

    /**
     * Sets multiple configuration values at once
     */
    public boolean setMultipleValues(Map<String, String> configMap) {
        boolean success = true;
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            if (!setConfigValue(entry.getKey(), entry.getValue())) {
                success = false;
            }
        }
        return success;
    }

    /**
     * Checks if a configuration key exists
     */
    public boolean hasConfigKey(String key) {
        return currentConfig.containsKey(key) || 
               systemMonitor.getConfigurationValue(activeConfigPath, key) != null;
    }

    /**
     * Gets the active configuration path
     */
    public String getActiveConfigPath() {
        return activeConfigPath;
    }

    /**
     * Checks if the manager is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Creates a default configuration file
     */
    private void createDefaultConfiguration(String configPath) {
        Properties defaultProps = new Properties();
        defaultProps.setProperty("application.name", "MyApplication");
        defaultProps.setProperty("application.version", "1.0.0");
        defaultProps.setProperty("logging.level", "INFO");
        defaultProps.setProperty("database.url", "jdbc:mysql://localhost:3306/mydb");
        defaultProps.setProperty("database.username", "admin");
        defaultProps.setProperty("database.password", "password");
        defaultProps.setProperty("server.port", "8080");
        defaultProps.setProperty("server.host", "localhost");
        defaultProps.setProperty("cache.enabled", "true");
        defaultProps.setProperty("cache.size", "1000");
        defaultProps.setProperty("security.encryption", "AES256");
        
        // Write default configuration to file
        StringBuilder configBuilder = new StringBuilder();
        for (String key : defaultProps.stringPropertyNames()) {
            configBuilder.append(key).append("=").append(defaultProps.getProperty(key)).append("\n");
        }
        
        systemMonitor.saveConfiguration(configPath, configBuilder.toString());
    }

    /**
     * Loads properties from string data
     */
    private void loadPropertiesFromString(String configData) {
        currentConfig.clear();
        String[] lines = configData.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue; // Skip comments and empty lines
            }
            
            int separatorIndex = line