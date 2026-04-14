import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NativeSerializer {

    private static boolean isLoaded = false;

    /**
     * Locates and loads the 'native_serializer' library from the 'native_libs' subdirectory.
     */
    public static synchronized void loadLibrary() {
        if (isLoaded) return;

        try {
            String libName = "native_serializer";
            String osSpecificName = System.mapLibraryName(libName);
            String projectRoot = System.getProperty("user.dir");

            Path libPath = Paths.get(projectRoot, "native_libs", osSpecificName)
                                .normalize()
                                .toAbsolutePath();

            File libFile = libPath.toFile();

            if (!libFile.exists()) {
                throw new RuntimeException("Native library not found at: " + libFile.getAbsolutePath() +
                        "\nPlease compile the C code and place it in the 'native_libs' directory.");
            }

            System.load(libFile.getAbsolutePath());
            isLoaded = true;
            System.out.println("Loaded native serializer from: " + libFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Failed to load the native serializer library.");
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    // Native Method Declarations
    // =========================================================
    
    // Serializes a UserData object into a packed byte array
    public native byte[] serialize(UserData user);

    // Deserializes a packed byte array back into a UserData object
    public native UserData deserialize(byte[] data);
}