import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PhysicsLibraryLoader {

    private static boolean isLoaded = false;

    /**
     * Loads the native physics library from the 'native_libs' directory.
     * This should be called once during the engine's initialization phase.
     */
    public static synchronized void load() {
        if (isLoaded) {
            return; // Prevent multiple load attempts
        }

        try {
            // The base name of your library
            String libName = "physics_core";

            // Converts "physics_core" to OS-specific name (e.g., "physics_core.dll", "libphysics_core.so")
            String mappedLibName = System.mapLibraryName(libName);

            // Get the current working directory (which is typically the project root)
            String projectRoot = System.getProperty("user.dir");

            // Construct the absolute path: <project_root>/native_libs/<mapped_lib_name>
            Path libPath = Paths.get(projectRoot, "native_libs", mappedLibName);
            File libFile = libPath.toFile();

            if (!libFile.exists()) {
                throw new RuntimeException("Native library not found at: " + libFile.getAbsolutePath() + 
                                           "\nMake sure the file exists and the platform architecture matches.");
            }

            // System.load requires an absolute path
            System.load(libFile.getAbsolutePath());
            isLoaded = true;

            System.out.println("Successfully loaded native physics engine: " + libFile.getAbsolutePath());
            
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Link Error: Failed to load the native physics library. " +
                               "Check if dependencies are missing or if architecture (x86/x64/ARM) mismatches.");
            throw e;
        } catch (Exception e) {
            System.err.println("An unexpected error occurred while loading the native library.");
            throw new RuntimeException(e);
        }
    }

    // =========================================================
    // Native Method Declarations
    // =========================================================
    
    // Example: public static native void initPhysicsWorld();
    // Example: public static native void stepSimulation(float deltaTime);
}