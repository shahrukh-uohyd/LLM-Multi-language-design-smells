import java.util.Locale;

/**
 * Handles loading the correct native library for AES encryption
 * based on the user's operating system at runtime.
 */
public class NativeLibraryLoader {

    // Library names per OS (without platform-specific prefix/extension)
    private static final String WINDOWS_LIBRARY_NAME = "crypto-native";
    private static final String LINUX_LIBRARY_NAME   = "libcrypto-linux";

    // Detected OS name (lower-cased once for reuse)
    private static final String OS_NAME =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    /**
     * Detects the current operating system.
     */
    public enum OperatingSystem {
        WINDOWS,
        LINUX,
        UNSUPPORTED
    }

    /**
     * Resolves the current {@link OperatingSystem}.
     *
     * @return the detected OS enum value
     */
    public static OperatingSystem detectOS() {
        if (OS_NAME.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (OS_NAME.contains("nux") || OS_NAME.contains("nix") || OS_NAME.contains("aix")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.UNSUPPORTED;
        }
    }

    /**
     * Returns the platform-specific native library name.
     *
     * <ul>
     *   <li>Windows → {@value #WINDOWS_LIBRARY_NAME}</li>
     *   <li>Linux   → {@value #LINUX_LIBRARY_NAME}</li>
     * </ul>
     *
     * @return the library name without file extension or path
     * @throws UnsupportedOperationException if the OS is not supported
     */
    public static String resolveLibraryName() {
        switch (detectOS()) {
            case WINDOWS:
                return WINDOWS_LIBRARY_NAME;
            case LINUX:
                return LINUX_LIBRARY_NAME;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported operating system: " + OS_NAME
                );
        }
    }

    /**
     * Loads the appropriate native AES encryption library for the current OS.
     *
     * <p>The JVM looks for the library in the directories listed in the
     * {@code java.library.path} system property. Ensure the correct library
     * file is present there before calling this method.</p>
     *
     * @throws UnsupportedOperationException if the OS is not supported
     * @throws UnsatisfiedLinkError          if the library cannot be found or loaded
     */
    public static void loadNativeLibrary() {
        String libraryName = resolveLibraryName();
        System.out.println("[NativeLibraryLoader] Detected OS  : " + OS_NAME);
        System.out.println("[NativeLibraryLoader] Loading library: " + libraryName);

        try {
            System.loadLibrary(libraryName);
            System.out.println("[NativeLibraryLoader] Successfully loaded: " + libraryName);
        } catch (UnsatisfiedLinkError e) {
            throw new UnsatisfiedLinkError(
                    "Failed to load native library '" + libraryName + "'. " +
                    "Ensure the library is present in java.library.path. Cause: " + e.getMessage()
            );
        }
    }

    // -----------------------------------------------------------------------
    // Static initialiser – load the library as soon as this class is used.
    // Remove this block if you prefer explicit loading via loadNativeLibrary().
    // -----------------------------------------------------------------------
    static {
        loadNativeLibrary();
    }

    // Prevent instantiation
    private NativeLibraryLoader() {}
}