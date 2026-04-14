import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataHelperLoader {

    private static boolean isLoaded = false;

    /**
     * Locates and loads the 'data_helper' native library from the sibling 'bin' folder.
     */
    public static synchronized void loadLibrary() {
        if (isLoaded) {
            return; // Prevent multiple initializations
        }

        try {
            // 1. Get the base name of the library
            String baseName = "data_helper";

            // 2. Map to OS-specific filename (e.g., data_helper.dll on Windows, libdata_helper.so on Linux)
            String osSpecificName = System.mapLibraryName(baseName);

            // 3. Get the directory where the Java application was executed
            String executionDir = System.getProperty("user.dir");

            // 4. Construct the path to the sibling 'bin' folder
            // Path structure: <execution_dir>/../bin/<os_specific_name>
            Path libPath = Paths.get(executionDir, "..", "bin", osSpecificName)
                                .normalize()       // Resolves the ".." into a clean path
                                .toAbsolutePath(); // Ensures it is an absolute path for System.load()

            File libFile = libPath.toFile();

            // 5. Ensure the file actually exists before attempting to load
            if (!libFile.exists()) {
                throw new RuntimeException("Native library not found at expected location: " + 
                                           libFile.getAbsolutePath() + 
                                           "\nEnsure the 'bin' folder is a sibling to the execution folder.");
            }

            // 6. Load the native library using the absolute file path
            System.load(libFile.getAbsolutePath());
            isLoaded = true;
            
            System.out.println("Successfully loaded native data library from: " + libFile.getAbsolutePath());

        } catch (UnsatisfiedLinkError e) {
            System.err.println("Link Error: The native library was found, but failed to load. " +
                               "Check if you have missing OS dependencies (like C++ redistributables) " +
                               "or if the Java architecture (32/64-bit) matches the library architecture.");
            throw e;
        } catch (Exception e) {
            System.err.println("An unexpected error occurred while resolving the native library path.");
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    // Native Method Declarations go here
    // =========================================================
    // public static native void processData(byte[] input);
}