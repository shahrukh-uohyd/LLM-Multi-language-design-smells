public class JniObjectFactory {
    
    // Load the shared C++ library at runtime
    static {
        System.loadLibrary("objectfactory");
    }

    // Native method declaration: assists with object construction and returns the object
    public native UserProfile createUserProfile(String baseName, int baseId);

    public static void main(String[] args) {
        JniObjectFactory factory = new JniObjectFactory();
        
        System.out.println("Java: Calling native method to prepare data and construct UserProfile...");
        System.out.println("-------------------------------------------------");
        
        // The native method prepares the data, creates the object, and returns it
        UserProfile profile = factory.createUserProfile("Guest", 7);
        
        // The returned value is used by the Java application
        System.out.println("-------------------------------------------------");
        if (profile != null) {
            System.out.println("Java: Successfully received constructed object from JNI!");
            profile.display();
        } else {
            System.out.println("Java: Failed to construct object in JNI.");
        }
    }
}