package com.audio;

import com.audio.platform.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Extracts and loads the platform-appropriate native audio-decoder library
 * from the application JAR at runtime.
 *
 * <h2>Loading Strategy</h2>
 * <ol>
 *   <li>Detect host OS + architecture via {@link Platform}.</li>
 *   <li>Locate the bundled binary at
 *       {@code /native/<platform-tag>/<lib-filename>} on the classpath.</li>
 *   <li>Copy it to a JVM-managed temporary file (auto-deleted on exit).</li>
 *   <li>Call {@link System#load(String)} with the absolute temp-file path.</li>
 * </ol>
 *
 * <p>Using {@link System#load(String)} instead of
 * {@link System#loadLibrary(String)} avoids all dependency on
 * {@code java.library.path} and the {@code lib} prefix ambiguity
 * on Linux, making the distribution fully self-contained.</p>
 *
 * <h2>Thread Safety</h2>
 * The public {@link #load()} method is {@code synchronized} and guarded
 * by an {@link AtomicBoolean}, so it is safe to call from multiple
 * threads simultaneously; the actual extraction and linking happen
 * exactly once.
 */
public final class NativeAudioLibraryLoader {

    /** Base name of the native audio library (without OS prefix / extension). */
    static final String LIBRARY_BASE_NAME = "audiodecoder";

    private static final AtomicBoolean LOADED            = new AtomicBoolean(false);
    private static volatile Path       loadedLibraryPath = null;
    private static volatile Platform   detectedPlatform  = null;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads the native audio library for the current platform.
     *
     * <p>Idempotent — subsequent calls after the first successful load
     * return immediately.</p>
     *
     * @throws AudioException             if the platform is unsupported,
     *                                    the resource is missing, or linking fails
     * @throws UnsupportedOperationException if the OS or arch cannot be identified
     */
    public static synchronized void load() {
        if (LOADED.get()) return;

        detectedPlatform = Platform.current();
        log("Detected platform  : " + detectedPlatform);

        String resourcePath = detectedPlatform.resourcePath(LIBRARY_BASE_NAME);
        String fileName     = detectedPlatform.fileName(LIBRARY_BASE_NAME);
        log("Resource path      : " + resourcePath);

        Path tempFile = extractToTemp(resourcePath, fileName);
        linkLibrary(tempFile, fileName);

        loadedLibraryPath = tempFile;
        LOADED.set(true);
        log("Library loaded from: " + tempFile);
    }

    /** Returns {@code true} after a successful call to {@link #load()}. */
    public static boolean isLoaded() {
        return LOADED.get();
    }

    /**
     * Returns the temporary file path from which the library was loaded.
     *
     * @throws IllegalStateException if {@link #load()} has not been called yet
     */
    public static Path getLoadedLibraryPath() {
        if (!LOADED.get()) {
            throw new IllegalStateException(
                    "Native audio library has not been loaded. Call load() first.");
        }
        return loadedLibraryPath;
    }

    /**
     * Returns the {@link Platform} that was detected during the last
     * successful {@link #load()} call.
     *
     * @throws IllegalStateException if {@link #load()} has not been called yet
     */
    public static Platform getDetectedPlatform() {
        if (!LOADED.get()) {
            throw new IllegalStateException(
                    "Native audio library has not been loaded. Call load() first.");
        }
        return detectedPlatform;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Copies the JAR-embedded resource to a temp file on the local filesystem.
     *
     * @param resourcePath classpath path to the native binary
     * @param fileName     original filename (used to preserve the OS extension)
     * @return path to the extracted temp file
     * @throws AudioException if the resource is absent or the copy fails
     */
    private static Path extractToTemp(String resourcePath, String fileName) {
        // Preserve the OS-correct file extension so the dynamic linker is happy
        int dotIdx  = fileName.lastIndexOf('.');
        String stem = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
        String ext  = dotIdx > 0 ? fileName.substring(dotIdx)    : "";

        try (InputStream in = NativeAudioLibraryLoader.class.getResourceAsStream(resourcePath)) {

            if (in == null) {
                throw new AudioException(buildMissingResourceMessage(resourcePath));
            }

            Path tempFile = Files.createTempFile(stem + "_", ext);
            tempFile.toFile().deleteOnExit();   // guaranteed cleanup on normal JVM shutdown
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);

            log("Extracted to temp  : " + tempFile + "  (" + Files.size(tempFile) + " bytes)");
            return tempFile;

        } catch (IOException e) {
            throw new AudioException(
                "I/O error while extracting native audio library from JAR resource '"
                + resourcePath + "': " + e.getMessage(), e);
        }
    }

    /**
     * Calls {@link System#load(String)} and wraps any {@link UnsatisfiedLinkError}
     * with a diagnostically rich {@link AudioException}.
     *
     * @param tempFile absolute path to the extracted library file
     * @param fileName original filename (for error messages)
     * @throws AudioException on link failure
     */
    private static void linkLibrary(Path tempFile, String fileName) {
        try {
            System.load(tempFile.toAbsolutePath().toString());
        } catch (UnsatisfiedLinkError e) {
            throw new AudioException(buildLinkErrorMessage(tempFile, fileName, e), e);
        }
    }

    // -------------------------------------------------------------------------
    // Diagnostic message builders
    // -------------------------------------------------------------------------

    private static String buildMissingResourceMessage(String resourcePath) {
        return "\n╔══════════════════════════════════════════════════════════════╗\n"
             + "║   FATAL – Native Audio Library Resource Not Found in JAR     ║\n"
             + "╚══════════════════════════════════════════════════════════════╝\n"
             + "  Attempted resource : " + resourcePath                          + "\n"
             + "  Platform tag       : " + (detectedPlatform != null
                                            ? detectedPlatform.tag() : "unknown") + "\n"
             + "Resolution checklist:\n"
             + "  1. Compile the native library for platform '"
                 + (detectedPlatform != null ? detectedPlatform.tag() : "?") + "'.\n"
             + "  2. Place the file at src/main/resources" + resourcePath + ".\n"
             + "  3. Rebuild the JAR so the resource is included.\n";
    }

    private static String buildLinkErrorMessage(Path tempFile, String fileName,
                                                 UnsatisfiedLinkError cause) {
        return "\n╔══════════════════════════════════════════════════════════════╗\n"
             + "║   FATAL – Native Audio Library Linking Failed                ║\n"
             + "╚══════════════════════════════════════════════════════════════╝\n"
             + "  Library file  : " + tempFile   + "\n"
             + "  Platform      : " + (detectedPlatform != null
                                       ? detectedPlatform : "unknown") + "\n"
             + "  Linker error  : " + cause.getMessage()                + "\n"
             + "Resolution checklist:\n"
             + "  1. Verify the library was compiled for this OS and architecture.\n"
             + "  2. Check for missing transitive native dependencies:\n"
             + "       Linux  : ldd "   + tempFile + "\n"
             + "       macOS  : otool -L " + tempFile + "\n"
             + "       Windows: Use Dependency Walker or 'dumpbin /dependents'.\n"
             + "  3. Ensure the temp directory is executable (noexec mount?).\n";
    }

    private static void log(String msg) {
        System.out.println("[NativeAudioLibraryLoader] " + msg);
    }

    private NativeAudioLibraryLoader() {}
}