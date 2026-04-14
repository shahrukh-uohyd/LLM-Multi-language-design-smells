public class SimulationEngineLoader {
    
    private static final String WINDOWS_64BIT_LIB = "SimEngine64";
    private static final String WINDOWS_32BIT_LIB = "SimEngine32";
    private static final String LINUX_64BIT_LIB = "libSimEngine64";
    private static final String LINUX_32BIT_LIB = "libSimEngine32";
    
    private static boolean isLoaded = false;
    
    public static synchronized void loadLibrary() {
        if (isLoaded) {
            return;
        }
        
        String osName = System.getProperty("os.name").toLowerCase();
        String architecture = System.getProperty("os.arch").toLowerCase();
        String libraryName;
        
        boolean is64Bit = architecture.contains("64") || 
                         architecture.equals("aarch64") || 
                         architecture.equals("armv8");
        
        if (osName.contains("win")) {
            libraryName = is64Bit ? WINDOWS_64BIT_LIB : WINDOWS_32BIT_LIB;
        } else if (osName.contains("linux")) {
            libraryName = is64Bit ? LINUX_64BIT_LIB : LINUX_32BIT_LIB;
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }
        
        try {
            System.loadLibrary(libraryName);
            isLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load native library: " + libraryName, e);
        }
    }
}