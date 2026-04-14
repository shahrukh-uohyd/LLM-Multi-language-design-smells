import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageFilter {

    private static boolean isLoaded = false;

    /**
     * Locates and loads the 'image_filter' native library from the 'native_libs' subdirectory.
     */
    public static synchronized void loadLibrary() {
        if (isLoaded) return;

        try {
            String libName = "image_filter";
            String osSpecificName = System.mapLibraryName(libName);
            String projectRoot = System.getProperty("user.dir");

            // Construct absolute path: <project_root>/native_libs/<os_specific_name>
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
            System.out.println("Loaded native library from: " + libFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Failed to load the native image filter library.");
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    // Native Method Declarations
    // =========================================================
    
    /**
     * Applies a grayscale filter to an array of ARGB pixels.
     * The array is modified in-place by the native code for optimal performance.
     * 
     * @param pixels 1D array of ARGB pixel data
     * @param width  Image width
     * @param height Image height
     */
    public native void applyGrayscale(int[] pixels, int width, int height);
}