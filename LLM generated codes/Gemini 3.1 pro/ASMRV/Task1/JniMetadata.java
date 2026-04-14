public class JniMetadata {
    
    // Load the shared C library at runtime
    static {
        System.loadLibrary("metadata");
    }

    // Native method declaration: locates TargetClass and returns metadata
    public native String getTargetClassMetadata();

    public static void main(String[] args) {
        JniMetadata app = new JniMetadata();
        
        System.out.println("Calling native method to retrieve class metadata...");
        
        // The native method is invoked, and it returns a String
        String metadata = app.getTargetClassMetadata();
        
        // The returned value is used by the Java application
        System.out.println("-------------------------------------------------");
        System.out.println("Result retrieved from JNI Native Code:");
        System.out.println(metadata);
        System.out.println("-------------------------------------------------");
    }
}