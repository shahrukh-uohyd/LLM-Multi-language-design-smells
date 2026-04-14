// DataSerializer.java
import java.util.HashMap;
import java.util.Map;

/**
 * A Java application that serializes and deserializes data using native code.
 * The native library containing the serialization logic is loaded from a project subdirectory.
 */
public class DataSerializer {

    // Load the native serialization library when the class is initialized.
    static {
        loadNativeLibrary();
    }

    /**
     * Loads the native library from the "libs" subfolder within the project.
     * This ensures the application is self-contained with its required native components.
     */
    private static void loadNativeLibrary() {
        try {
            String libPath = System.getProperty("user.dir") + 
                           System.getProperty("file.separator") + 
                           "libs" +
                           System.getProperty("file.separator") + 
                           System.mapLibraryName("native_serializer");
            System.load(libPath);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                "Failed to load native library 'native_serializer' from the 'libs' subfolder. " +
                "Ensure the library file exists there. Error: " + e.getMessage(), e);
        }
    }

    /**
     * Serializes a map of key-value pairs into a byte array using native code.
     *
     * @param dataMap A Map containing String keys and String values to be serialized.
     * @return A byte array representing the serialized data.
     */
    public static native byte[] serializeData(Map<String, String> dataMap);

    /**
     * Deserializes a byte array back into a map of key-value pairs using native code.
     *
     * @param serializedBytes A byte array previously created by the serializeData method.
     * @return A Map containing the deserialized key-value pairs.
     */
    public static native Map<String, String> deserializeData(byte[] serializedBytes);

    /**
     * The main method demonstrates the usage of native serialization and deserialization.
     */
    public static void main(String[] args) {
        // Create sample data to serialize
        Map<String, String> originalData = new HashMap<>();
        originalData.put("name", "Alice");
        originalData.put("city", "Wonderland");
        originalData.put("mood", "curious");
        originalData.put("answer", "42");

        System.out.println("Original Data:");
        originalData.forEach((key, value) -> System.out.println(key + " -> " + value));

        // Serialize the data using the native method
        System.out.println("\nSerializing data using native code...");
        byte[] serializedBytes = serializeData(originalData);

        // Print the length of the serialized data as a basic check
        System.out.println("Serialized data length: " + serializedBytes.length + " bytes");

        // Deserialize the data back using the native method
        System.out.println("\nDeserializing data using native code...");
        Map<String, String> deserializedData = deserializeData(serializedBytes);

        // Verify the deserialized data matches the original
        System.out.println("\nDeserialized Data:");
        deserializedData.forEach((key, value) -> System.out.println(key + " -> " + value));

        // Simple validation check
        if (originalData.equals(deserializedData)) {
            System.out.println("\nSuccess! Original and deserialized data are identical.");
        } else {
            System.out.println("\nError! Original and deserialized data do not match.");
        }
    }
}