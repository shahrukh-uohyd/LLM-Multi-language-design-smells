import java.util.Locale;

/**
 * JNI wrapper responsible for detecting the host OS and CPU architecture,
 * then loading the correct native simulation-engine library at runtime.
 *
 * <p>Supported matrix:</p>
 * <pre>
 *  ┌─────────────┬──────────┬───────────────────┐
 *  │ OS          │ Arch     │ Library loaded     │
 *  ├─────────────┼──────────┼───────────────────┤
 *  │ Windows     │ 64-bit   │ SimEngine64        │
 *  │ Windows     │ 32-bit   │ SimEngine32        │
 *  │ Linux       │ 64-bit   │ SimEngine64        │
 *  │ Linux       │ 32-bit   │ SimEngine32        │
 *  └─────────────┴──────────┴───────────────────┘
 * </pre>
 *
 * <p>Place the compiled native libraries in a directory that is listed in
 * {@code java.library.path}, then launch the JVM with:</p>
 * <pre>
 *   java -Djava.library.path=./libs -jar sim-engine.jar
 * </pre>
 *
 * <p>Expected native library filenames on disk:</p>
 * <ul>
 *   <li>Windows 64-bit : {@code SimEngine64.dll}</li>
 *   <li>Windows 32-bit : {@code SimEngine32.dll}</li>
 *   <li>Linux 64-bit   : {@code libSimEngine64.so}</li>
 *   <li>Linux 32-bit   : {@code libSimEngine32.so}</li>
 * </ul>
 */
public final class SimEngineLoader {

    // -----------------------------------------------------------------------
    // Library names (without OS-specific prefix / extension)
    // -----------------------------------------------------------------------
    private static final String LIB_64_BIT = "SimEngine64";
    private static final String LIB_32_BIT = "SimEngine32";

    // -----------------------------------------------------------------------
    // System property snapshots (resolved once, at class-load time)
    // -----------------------------------------------------------------------
    private static final String OS_NAME =
            System.getProperty("os.name",    "unknown").toLowerCase(Locale.ROOT);
    private static final String OS_ARCH =
            System.getProperty("os.arch",    "unknown").toLowerCase(Locale.ROOT);
    private static final String DATA_MODEL =
            System.getProperty("sun.arch.data.model", "unknown");   // "32" or "64"

    // -----------------------------------------------------------------------
    // Enums
    // -----------------------------------------------------------------------

    /** Supported operating systems. */
    public enum OperatingSystem {
        WINDOWS, LINUX, UNSUPPORTED;
    }

    /** JVM / CPU address-space width. */
    public enum Architecture {
        BITS_64, BITS_32, UNSUPPORTED;
    }

    // -----------------------------------------------------------------------
    // Detection helpers
    // -----------------------------------------------------------------------

    /**
     * Detects the host operating system.
     *
     * @return {@link OperatingSystem} enum constant
     */
    public static OperatingSystem detectOS() {
        if (OS_NAME.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (OS_NAME.contains("nux")
                || OS_NAME.contains("nix")
                || OS_NAME.contains("aix")) {
            return OperatingSystem.LINUX;
        }
        return OperatingSystem.UNSUPPORTED;
    }

    /**
     * Detects the JVM / CPU architecture using two independent signals:
     * <ol>
     *   <li>{@code sun.arch.data.model} – most reliable when available ("32" / "64")</li>
     *   <li>{@code os.arch}             – fallback; covers amd64, x86_64, i386, etc.</li>
     * </ol>
     *
     * @return {@link Architecture} enum constant
     */
    public static Architecture detectArchitecture() {
        // Primary signal -------------------------------------------------
        if ("64".equals(DATA_MODEL)) {
            return Architecture.BITS_64;
        }
        if ("32".equals(DATA_MODEL)) {
            return Architecture.BITS_32;
        }

        // Fallback signal ------------------------------------------------
        if (OS_ARCH.contains("64")                    // amd64, x86_64, aarch64 …
                || OS_ARCH.equals("aarch64")
                || OS_ARCH.equals("ppc64")
                || OS_ARCH.equals("ppc64le")
                || OS_ARCH.equals("s390x")) {
            return Architecture.BITS_64;
        }
        if (OS_ARCH.contains("86")                    // x86, i386, i486, i586, i686
                || OS_ARCH.equals("arm")
                || OS_ARCH.equals("ppc")) {
            return Architecture.BITS_32;
        }

        return Architecture.UNSUPPORTED;
    }

    // -----------------------------------------------------------------------
    // Library name resolution
    // -----------------------------------------------------------------------

    /**
     * Resolves the correct native library name for the detected OS + architecture.
     *
     * @return library base name suitable for {@link System#loadLibrary(String)}
     * @throws UnsupportedOperationException for an unsupported OS or architecture
     */
    public static String resolveLibraryName() {
        OperatingSystem os   = detectOS();
        Architecture    arch = detectArchitecture();

        if (os == OperatingSystem.UNSUPPORTED) {
            throw new UnsupportedOperationException(
                "Unsupported operating system: \"" + OS_NAME + "\". "
                + "Only Windows and Linux are currently supported."
            );
        }
        if (arch == Architecture.UNSUPPORTED) {
            throw new UnsupportedOperationException(
                "Unsupported CPU architecture: os.arch=\"" + OS_ARCH + "\", "
                + "sun.arch.data.model=\"" + DATA_MODEL + "\". "
                + "Only 32-bit and 64-bit architectures are supported."
            );
        }

        return (arch == Architecture.BITS_64) ? LIB_64_BIT : LIB_32_BIT;
    }

    // -----------------------------------------------------------------------
    // Library loading
    // -----------------------------------------------------------------------

    /**
     * Loads the appropriate SimEngine native library for the current
     * OS + architecture combination.
     *
     * <p>The JVM searches for the library file in the directories listed in
     * the {@code java.library.path} system property. Ensure the correct
     * compiled library is present before calling this method.</p>
     *
     * @throws UnsupportedOperationException if the platform is not supported
     * @throws SimEngineLoadException        if the library file cannot be found or linked
     */
    public static void loadNativeLibrary() {
        String libraryName = resolveLibraryName();   // throws if platform unsupported

        logInfo("Host OS          : " + OS_NAME);
        logInfo("Host arch        : " + OS_ARCH);
        logInfo("JVM data model   : " + DATA_MODEL + "-bit");
        logInfo("Detected OS      : " + detectOS());
        logInfo("Detected arch    : " + detectArchitecture());
        logInfo("Loading library  : " + libraryName);

        try {
            System.loadLibrary(libraryName);
            logInfo("Successfully loaded native library: " + libraryName);
        } catch (UnsatisfiedLinkError e) {
            String libraryPath = System.getProperty("java.library.path", "<not set>");
            throw new SimEngineLoadException(
                "Failed to load native library '" + libraryName + "'.\n"
                + "  java.library.path = " + libraryPath + "\n"
                + "  Verify the library file exists in one of those directories.\n"
                + "  Cause: " + e.getMessage(),
                e
            );
        }
    }

    // -----------------------------------------------------------------------
    // Static initialiser – triggers automatic load when class is first used.
    // Comment out this block if you prefer explicit loading via
    // SimEngineLoader.loadNativeLibrary() instead.
    // -----------------------------------------------------------------------
    static {
        loadNativeLibrary();
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private static void logInfo(String message) {
        System.out.println("[SimEngineLoader] " + message);
    }

    /** Prevent instantiation – this is a pure utility class. */
    private SimEngineLoader() {}
}