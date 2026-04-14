public class NativeSolver {

    // 1. Static initializer to load the native library before any operations
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        
        String libName;
        
        // Determine the highly specific OS and Architecture library name
        if (osName.contains("win")) {
            libName = "solver_win_" + (osArch.contains("64") ? "64" : "32");
        } else if (osName.contains("mac")) {
            libName = "solver_mac_" + (osArch.contains("aarch64") ? "arm64" : "x86_64");
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            libName = "solver_linux_" + (osArch.contains("64") ? "64" : "32");
        } else {
            throw new UnsupportedOperationException("Unsupported OS for Native Solver: " + osName);
        }

        try {
            // Attempt to load the architecture-specific library
            // e.g., looks for solver_win_64.dll, libsolver_linux_64.so, etc.
            System.loadLibrary(libName);
            System.out.println("Successfully loaded native library: " + libName);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: Could not load specific library '" + libName + "'. Trying generic name 'solver'...");
            try {
                // Fallback to a generic library name if the specific one is missing
                // This relies on Java's built-in platform prefix/suffix resolution
                System.loadLibrary("solver");
                System.out.println("Successfully loaded generic native library: solver");
            } catch (UnsatisfiedLinkError e2) {
                System.err.println("CRITICAL: Failed to load the native solver library.");
                System.err.println("Ensure your java.library.path is configured correctly.");
                throw new RuntimeException("Native library initialization failed.", e2);
            }
        }
    }

    // 2. Native method declarations (implemented in C/C++)
    private native void initSolver();
    private native double[] executeSolve(double[][] matrix, double[] constants);
    private native void destroySolver();

    // 3. Safe Java wrappers to abstract JNI details from the rest of the app
    public NativeSolver() {
        // Initialize native memory or engine states when the object is created
        initSolver();
    }

    /**
     * Solves a system of linear equations using the native library.
     */
    public double[] solve(double[][] matrix, double[] constants) {
        if (matrix == null || constants == null) {
            throw new IllegalArgumentException("Matrix and constants cannot be null");
        }
        if (matrix.length != constants.length) {
            throw new IllegalArgumentException("Matrix rows must match the number of constants");
        }
        
        // Call the JNI method
        return executeSolve(matrix, constants);
    }

    /**
     * Cleans up native memory. Should be called when the solver is no longer needed.
     */
    public void close() {
        destroySolver();
    }
}