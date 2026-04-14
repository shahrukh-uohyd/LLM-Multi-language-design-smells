package com.solver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Extracts the correct native solver library from the JAR resources at runtime
 * and loads it using {@link System#load(String)}.
 *
 * <h2>Loading strategy</h2>
 * <ol>
 *   <li>Detect host OS + architecture via {@link Platform}.</li>
 *   <li>Locate the library binary bundled at
 *       {@code /native/<platform-tag>/<lib-filename>} inside the JAR.</li>
 *   <li>Copy it to a JVM-managed temp file (auto-deleted on JVM exit).</li>
 *   <li>Call {@link System#load(String)} with the absolute temp-file path.</li>
 * </ol>
 *
 * <p>This approach works regardless of {@code java.library.path} configuration,
 * making it ideal for self-contained JAR distributions.</p>
 */
public final class NativeLibraryLoader {

    private static final String BASE_LIBRARY_NAME  = "solver";
    private static final AtomicBoolean LOADED       = new AtomicBoolean(false);

    /** Loaded library's temp-file path – retained for diagnostics. */
    private static volatile Path loadedLibraryPath = null;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads the native solver library. Safe to call multiple times;
     * the actual load operation is performed only once.
     *
     * @throws SolverException if extraction or linking fails
     */
    public static synchronized void load() {
        if (LOADED.get()) return;   // idempotent

        Platform platform = Platform.current();
        log("Detected platform : " + platform);

        String fileName    = platform.nativeFileName(BASE_LIBRARY_NAME);
        String resourceDir = platform.resourceDirectory();
        String resourcePath = "/" + resourceDir + fileName;

        log("Resource path     : " + resourcePath);

        Path tempFile = extractToTemp(resourcePath, fileName);
        loadFromPath(tempFile, fileName);

        loadedLibraryPath = tempFile;
        LOADED.set(true);
        log("Successfully loaded native solver library from: " + tempFile);
    }

    /**
     * Returns {@code true} if the library has been loaded successfully.
     */
    public static boolean isLoaded() {
        return LOADED.get();
    }

    /**
     * Returns the temp-file path from which the library was loaded.
     *
     * @throws IllegalStateException if {@link #load()} has not yet been called
     */
    public static Path getLoadedLibraryPath() {
        if (!LOADED.get()) {
            throw new IllegalStateException("Native library has not been loaded yet.");
        }
        return loadedLibraryPath;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Copies the JAR resource at {@code resourcePath} to a temporary file.
     *
     * @param resourcePath classpath resource path (e.g. {@code /native/linux-x86_64/libsolver.so})
     * @param fileName     base file name used to name the temp file
     * @return {@link Path} of the written temp file
     * @throws SolverException if the resource is missing or the copy fails
     */
    private static Path extractToTemp(String resourcePath, String fileName) {
        // Derive suffix so the OS keeps the correct extension
        int dotIdx = fileName.lastIndexOf('.');
        String prefix = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
        String suffix = dotIdx > 0 ? fileName.substring(dotIdx)    : "";

        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new SolverException(
                    "Native library resource not found in JAR: " + resourcePath + "\n"
                    + "Ensure the library is bundled for platform: "
                    + Platform.current().platformTag()
                );
            }

            Path tempFile = Files.createTempFile(prefix + "_", suffix);
            tempFile.toFile().deleteOnExit();   // clean up on normal JVM exit

            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log("Extracted library : " + resourcePath + " → " + tempFile);
            return tempFile;

        } catch (IOException e) {
            throw new SolverException(
                "Failed to extract native library '" + resourcePath + "' to temp directory: "
                + e.getMessage(), e
            );
        }
    }

    /**
     * Calls {@link System#load(String)} on the extracted temp file.
     *
     * @throws SolverException wrapping the original {@link UnsatisfiedLinkError}
     */
    private static void loadFromPath(Path tempFile, String fileName) {
        try {
            System.load(tempFile.toAbsolutePath().toString());
        } catch (UnsatisfiedLinkError e) {
            throw new SolverException(
                "Failed to link native library '" + fileName + "' from: " + tempFile + "\n"
                + "Possible causes:\n"
                + "  • Library compiled for a different OS/architecture than: "
                    + Platform.current().platformTag() + "\n"
                + "  • Missing transitive native dependencies.\n"
                + "  • Insufficient file permissions on the temp directory.\n"
                + "Cause: " + e.getMessage(), e
            );
        }
    }

    private static void log(String msg) {
        System.out.println("[NativeLibraryLoader] " + msg);
    }

    private NativeLibraryLoader() {}
}