// Existing class for system monitoring and performance diagnostics
public class SystemMonitor {
    static {
        // Loads the existing native library (e.g., libsysmonitor.so or sysmonitor.dll)
        System.loadLibrary("sysmonitor");
    }

    // Existing native methods for OS interaction
    public native double getCpuLoad();
    public native long getFreeMemory();
}