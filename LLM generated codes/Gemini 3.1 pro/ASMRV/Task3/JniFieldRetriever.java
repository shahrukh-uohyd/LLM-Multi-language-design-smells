public class JniFieldRetriever {
    
    // Load the shared C++ library at runtime
    static {
        System.loadLibrary("fieldretriever");
    }

    // Native method declaration: takes a DataContainer object as a parameter
    // and returns the integer value retrieved from its field.
    public native int retrieveIntegerValue(DataContainer container);

    public static void main(String[] args) {
        JniFieldRetriever app = new JniFieldRetriever();
        
        // Instantiate the Java object with a specific integer value
        DataContainer container = new DataContainer(404);
        
        System.out.println("Java: Created DataContainer with targetValue = " + container.targetValue);
        System.out.println("Java: Passing object to C++ native method...");
        System.out.println("-------------------------------------------------");
        
        // Call the native method, passing the Java object as a parameter
        int retrievedValue = app.retrieveIntegerValue(container);
        
        // The returned value is used by the Java application
        System.out.println("-------------------------------------------------");
        System.out.println("Java: Value successfully retrieved from C++ JNI: " + retrievedValue);
    }
}