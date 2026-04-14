import java.util.Locale;

/**
 * Loads the third-party native graphics library at class-initialization time.
 *
 * <p>The library may be installed under any of the following base names,
 * depending on the environment and OS:</p>
 * <ol>
 *   <li>{@code graphics_core}</li>
 *   <li>{@code libgraphics_core}</li>
 *   <li>{@code graphics_core_v1}</li>
 * </ol>
 *
 * <p>The static initialiser tries each name in the order listed above.
 * The first successful load wins; remaining candidates are skipped.
 * If every candidate fails an {@link UnsatisfiedLinkError}, a single
 * {@link GraphicsLibraryLoadException} is thrown that aggregates all
 * per-candidate error messages so the developer has full diagnostic
 * information in one place.</p>
 *
 * <p><b>Expected file names on disk</b></p>
 * <pre>
 *  Candidate name       Windows                    Linux / macOS
 *  ─────────────────    ──────────────────────     ────────────────────────────
 *  graphics_core        graphics_core.dll          libgraphics_core.so
 *  libgraphics_core     liblibgraphics_core.so *   liblibgraphics_core.so *
 *  graphics_core_v1     graphics_core_v1.dll       libgraphics_core_v1.so
 * </pre>
 * <p>* On Linux, {@code System.loadLibrary("libgraphics_core")} maps to
 * {@code liblibgraphics_core.so}. If your file is literally named
 * {@code libgraphics_core.so}, use {@link System#load(String)} with an
 * absolute path instead, or rename the candidate to {@code "graphics_core"}
 * so the JVM resolves it to {@code libgraphics_core.so} automatically.</p>
 *
 * <p><b>Placing libraries on the search path</b></p>
 * <pre>
 *   java -Djava.library.path=/path/to/native/libs -jar your-app.jar
 * </pre>
 */
public final class GraphicsCoreLibraryLoader {

    // -----------------------------------------------------------------------
    // Candidate library names – tried in this exact order.
    // -----------------------------------------------------------------------
    private static final String[] LIBRARY_CANDIDATES = {
        "graphics_core",       // most common / standard name
        "libgraphics_core",    // some Linux packagers keep the "lib" prefix
        "graphics_core_v1"     // versioned / legacy installs
    };

    // -----------------------------------------------------------------------
    // Snapshot of relevant system properties (resolved once).
    // -----------------------------------------------------------------------
    private static final String OS_NAME =
            System.getProperty("os.name",            "unknown").toLowerCase(Locale.ROOT);
    private static final String OS_ARCH =
            System.getProperty("os.arch",            "unknown").toLowerCase(Locale.ROOT);
    private static final String JAVA_LIBRARY_PATH =
            System.getProperty("java.library.path",  "<not set>");

    // -----------------------------------------------------------------------
    // Loading outcome – accessible after the static initialiser has run.
    // -----------------------------------------------------------------------

    /** The library name that was successfully loaded, or {@code null} if none. */
    private static volatile String loadedLibraryName = null;

    // -----------------------------------------------------------------------
    // Static initialiser
    //
    // This block runs exactly once, when the JVM first loads this class.
    // It iterates over every candidate name and calls System.loadLibrary().
    // The first successful load sets `loadedLibraryName` and breaks the loop.
    // If every attempt fails, all per-candidate errors are bundled into a
    // single GraphicsLibraryLoadException and rethrown.
    // -----------------------------------------------------------------------
    static {
        logInfo("=== GraphicsCoreLibraryLoader – native library initialisation ===");
        logInfo("OS name          : " + OS_NAME);
        logInfo("OS arch          : " + OS_ARCH);
        logInfo("java.library.path: " + JAVA_LIBRARY_PATH);

        // Accumulate one failure record per candidate.
        LoadAttempt[] attempts = new LoadAttempt[LIBRARY_CANDIDATES.length];

        for (int i = 0; i < LIBRARY_CANDIDATES.length; i++) {
            String candidate = LIBRARY_CANDIDATES[i];
            logInfo(String.format("Attempt [%d/%d] – loading library: '%s' …",
                    i + 1, LIBRARY_CANDIDATES.length, candidate));

            try {
                System.loadLibrary(candidate);
                // ── SUCCESS ───────────────────────────────────────────────
                loadedLibraryName = candidate;
                attempts[i] = LoadAttempt.success(candidate);
                logInfo("SUCCESS – loaded '" + candidate + "'.");
                break;

            } catch (UnsatisfiedLinkError e) {
                // ── FAILURE – record and continue to next candidate ───────
                attempts[i] = LoadAttempt.failure(candidate, e);
                logWarn("FAILED  – '" + candidate + "': " + e.getMessage());
            }
        }

        // If no candidate succeeded, fail loudly with aggregated diagnostics.
        if (loadedLibraryName == null) {
            logError("All " + LIBRARY_CANDIDATES.length
                    + " library candidate(s) failed to load. "
                    + "Throwing GraphicsLibraryLoadException.");
            throw new GraphicsLibraryLoadException(attempts, OS_NAME, OS_ARCH, JAVA_LIBRARY_PATH);
        }

        logInfo("=== Initialisation complete. Active library: '"
                + loadedLibraryName + "' ===");
    }

    // -----------------------------------------------------------------------
    // Public query API
    // -----------------------------------------------------------------------

    /**
     * Returns the library name that was successfully loaded.
     *
     * @return base library name passed to {@link System#loadLibrary(String)}
     * @throws IllegalStateException if this class failed to initialise
     *         (should not happen in practice – the static initialiser would
     *         already have thrown before any caller could reach this method)
     */
    public static String getLoadedLibraryName() {
        if (loadedLibraryName == null) {
            throw new IllegalStateException(
                "No native graphics library was loaded. "
                + "Check earlier startup logs for details."
            );
        }
        return loadedLibraryName;
    }

    /**
     * Returns {@code true} if the native library has been loaded successfully.
     * Useful for optional / degraded-mode checks in calling code.
     */
    public static boolean isLibraryLoaded() {
        return loadedLibraryName != null;
    }

    // -----------------------------------------------------------------------
    // Logging helpers (replace with SLF4J / Log4j in production)
    // -----------------------------------------------------------------------

    private static void logInfo(String msg)  { System.out.println("[INFO ] [GraphicsCoreLoader] " + msg); }
    private static void logWarn(String msg)  { System.out.println("[WARN ] [GraphicsCoreLoader] " + msg); }
    private static void logError(String msg) { System.err.println ("[ERROR] [GraphicsCoreLoader] " + msg); }

    /** Utility class – no instances. */
    private GraphicsCoreLibraryLoader() {}
}