public class SystemMonitor {

    static {
        System.loadLibrary("systemmonitor");
    }

    // Existing native methods (system / performance)
    public native long getUptimeMillis();
    public native int getAvailableProcessors();

    // New native-backed configuration feature
    public native void loadConfiguration(String configPath);

    // Internal monitoring logic
    public void printSystemStatus() {
        System.out.println("Uptime (ms): " + getUptimeMillis());
        System.out.println("CPU cores: " + getAvailableProcessors());
    }
}
