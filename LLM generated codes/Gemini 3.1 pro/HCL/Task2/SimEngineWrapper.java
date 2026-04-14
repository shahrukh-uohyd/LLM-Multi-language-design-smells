public class SimEngineWrapper {

    static {
        // Retrieve system properties and convert to lowercase for robust matching
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        // 1. Validate the Operating System
        boolean isWindows = osName.contains("win");
        boolean isLinux = osName.contains("nux") || osName.contains("nix");

        if (!isWindows && !isLinux) {
            throw new UnsupportedOperationException("Unsupported Operating System: " + osName 
                + ". The Simulation Engine currently only targets Windows and Linux.");
        }

        // 2. Determine the JVM Architecture
        // Common 64-bit values: "amd64", "x86_64", "aarch64"
        // Common 32-bit values: "x86", "i386", "i686"
        boolean is64Bit = osArch.contains("64");
        
        // 3. Select the appropriate library name
        String libName = is64Bit ? "SimEngine64" : "SimEngine32";

        // 4. Load the library
        try {
            /* 
             * System.loadLibrary automatically handles platform-specific prefixes and suffixes:
             * - On Windows 64-bit: Looks for "SimEngine64.dll"
             * - On Linux 64-bit: Looks for "libSimEngine64.so"
             * - On Windows 32-bit: Looks for "SimEngine32.dll"
             * - On Linux 32-bit: Looks for "libSimEngine32.so"
             */
            System.loadLibrary(libName);
            
        } catch (UnsatisfiedLinkError e) {
            System.err.println("CRITICAL ERROR: Failed to load the native simulation library: " + libName);
            System.err.println("Detected OS: " + System.getProperty("os.name") + ", Arch: " + System.getProperty("os.arch"));
            System.err.println("Please ensure the appropriate .dll or .so file is in your java.library.path");
            throw new RuntimeException("Native Simulation Engine initialization failed.", e);
        }
    }

    /**
     * Initializes the simulation engine's internal state.
     */
    public native void initSimulation();

    /**
     * Advances the simulation by one tick/step.
     */
    public native void tick(double deltaTime);
    
    /**
     * Cleans up native memory when the simulation is destroyed.
     */
    public native void destroySimulation();
}