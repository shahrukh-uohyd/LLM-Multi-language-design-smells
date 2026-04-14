import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdvancedMath {

    private static boolean isLoaded = false;

    /**
     * Locates and loads the 'advmath' native library from the 'native_libs' subdirectory.
     */
    public static synchronized void loadLibrary() {
        if (isLoaded) return;

        try {
            String libName = "advmath";
            String osSpecificName = System.mapLibraryName(libName);
            String projectRoot = System.getProperty("user.dir");

            // Construct absolute path: <project_root>/native_libs/<os_specific_name>
            Path libPath = Paths.get(projectRoot, "native_libs", osSpecificName)
                                .normalize()
                                .toAbsolutePath();

            File libFile = libPath.toFile();

            if (!libFile.exists()) {
                throw new RuntimeException("Native library not found at: " + libFile.getAbsolutePath() +
                        "\nPlease compile the C code and place the native library in the 'native_libs' directory.");
            }

            System.load(libFile.getAbsolutePath());
            isLoaded = true;
            System.out.println("Loaded native library from: " + libFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Failed to load the native math library.");
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    // Native Method Declarations
    // =========================================================
    
    // Calculates base raised to the power of exponent
    public native double power(double base, double exponent);

    // Calculates the factorial of n (n!)
    public native long factorial(int n);

    // Calculates the n-th Fibonacci number
    public native long fibonacci(int n);
}