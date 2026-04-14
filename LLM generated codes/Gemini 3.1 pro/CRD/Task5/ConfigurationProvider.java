// New utility class specifically for configuration JNI calls
public class ConfigurationProvider {
    static {
        // Loads the new native library for configuration (e.g., libappconfig.so or appconfig.dll)
        System.loadLibrary("appconfig");
    }

    // Native method to fetch a configuration string by key
    public native String fetchConfigValue(String key);

    // Native method to save or update a configuration string
    public native boolean updateConfigValue(String key, String value);
}