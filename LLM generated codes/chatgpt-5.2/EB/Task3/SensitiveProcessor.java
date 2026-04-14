public class SensitiveProcessor {

    static {
        System.loadLibrary("nativeprocessor");
    }

    // Native entry point
    public native void processSensitiveData();

    // Private internal logger (NOT part of public API)
    private void logInternal(String message) {
        // In real systems, this would write to a secure log
        System.out.println("[SECURE LOG] " + message);
    }

    public static void main(String[] args) {
        SensitiveProcessor processor = new SensitiveProcessor();
        processor.processSensitiveData();
    }
}
