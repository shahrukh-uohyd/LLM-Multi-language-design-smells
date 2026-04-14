public class NativeCryptoLoader {
    
    private static final String WINDOWS_LIBRARY_NAME = "crypto-native";
    private static final String LINUX_LIBRARY_NAME = "libcrypto-linux";
    private static final String MAC_LIBRARY_NAME = "libcrypto-mac"; // Added for completeness
    
    private static boolean isLoaded = false;
    
    public static synchronized void loadLibrary() {
        if (isLoaded) {
            return;
        }
        
        String osName = System.getProperty("os.name").toLowerCase();
        String libraryName;
        
        if (osName.contains("win")) {
            libraryName = WINDOWS_LIBRARY_NAME;
        } else if (osName.contains("linux")) {
            libraryName = LINUX_LIBRARY_NAME;
        } else if (osName.contains("mac")) {
            libraryName = MAC_LIBRARY_NAME;
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }
        
        try {
            System.loadLibrary(libraryName);
            isLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load native library: " + libraryName, e);
        }
    }
}