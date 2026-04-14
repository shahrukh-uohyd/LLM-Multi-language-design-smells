public final class SimEngineNativeLoader {

    private static final String OS_NAME  = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH  = System.getProperty("os.arch").toLowerCase();

    private static volatile boolean loaded = false;

    private SimEngineNativeLoader() {
        // no instances
    }

    /**
     * Loads the appropriate native library based on OS and architecture.
     * Safe to call multiple times.
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String libraryName = resolveLibraryName();

        try {
            System.loadLibrary(libraryName);
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            throw new IllegalStateException(
                    "Failed to load native library '" + libraryName +
                    "' for OS=" + OS_NAME + ", ARCH=" + OS_ARCH,
                    e
            );
        }
    }

    private static String resolveLibraryName() {
        boolean is64Bit = is64Bit();
        boolean isWindows = OS_NAME.contains("win");
        boolean isLinux = OS_NAME.contains("linux");

        if (!isWindows && !isLinux) {
            throw new UnsupportedOperationException(
                    "Unsupported operating system: " + OS_NAME
            );
        }

        if (is64Bit) {
            return "SimEngine64";
        } else {
            return "SimEngine32";
        }
    }

    /**
     * Determines whether the JVM is running in 64-bit mode.
     * This is what matters for JNI compatibility.
     */
    private static boolean is64Bit() {
        // Common values: amd64, x86_64, aarch64
        return OS_ARCH.contains("64");
    }
}
