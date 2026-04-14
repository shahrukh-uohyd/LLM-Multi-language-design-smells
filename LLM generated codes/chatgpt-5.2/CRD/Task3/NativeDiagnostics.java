public class NativeDiagnostics {

    static {
        System.loadLibrary("nativediag");
    }

    // Existing native methods (OS / diagnostics)
    public native long getProcessId();
    public native void logToSystem(String message);

    // New native-backed encryption feature
    public native byte[] encrypt(byte[] data, byte key);
    public native byte[] decrypt(byte[] data, byte key);

    // Existing internal diagnostics logic
    public void logStartup() {
        logToSystem("Application started, pid=" + getProcessId());
    }
}
