public class SecureDataProcessor {

    static {
        System.loadLibrary("secure_processor"); // Loads the compiled C code
    }

    // The native method that triggers the C processing logic
    public native void processNativeData();

    // The private method intended only for internal use
    // Note: C/C++ JNI code bypasses the 'private' modifier and can invoke this
    private void logSecureDiagnostic(String message) {
        System.out.println("[SECURE AUDIT LOG] " + message);
        // ... Logic to write to an encrypted diagnostic file ...
    }

    public static void main(String[] args) {
        SecureDataProcessor processor = new SecureDataProcessor();
        
        System.out.println("Java: Starting native secure processing...");
        
        // This will trigger the native C code, which simulates an error 
        // and calls back into logSecureDiagnostic()
        processor.processNativeData();
        
        System.out.println("Java: Processing routine finished.");
    }
}