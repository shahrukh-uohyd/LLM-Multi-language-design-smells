package com.dataanalysis.init;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * Initializes and loads the {@code data_helper} native library for the
 * portable data analysis tool.
 *
 * <h2>Expected Installation Layout</h2>
 * <p>The tool must be installed with the following portable directory structure,
 * regardless of where the user places it on their file system:
 * <pre>
 *   &lt;install-root&gt;/
 *   ├── app/                        ← The execution folder (contains the JAR)
 *   │   └── data-analysis-tool.jar
 *   └── bin/                        ← Sibling of 'app/', contains the native library
 *       ├── libdata_helper.so       ← Linux
 *       ├── libdata_helper.dylib    ← macOS
 *       └── data_helper.dll         ← Windows
 * </pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>The loader resolves the {@code bin/} folder by:
 * <ol>
 *   <li>Locating the directory that contains the running JAR file on disk.</li>
 *   <li>Navigating one level up to the installation root ({@code ..}).</li>
 *   <li>Descending into the {@code bin/} sibling directory.</li>
 *   <li>Building the platform-specific library file name and calling
 *       {@link System#load(String)} with the absolute path.</li>
 * </ol>
 *
 * <p>This approach is fully portable: it works correctly no matter where
 * the user has installed the tool, because the path is always resolved
 * relative to the JAR's own location — never relative to the JVM's
 * working directory.
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link #load()} method is {@code synchronized} and guards against
 * duplicate loads via a {@code volatile} boolean flag. It is safe to call
 * from multiple threads during application startup.
 */
public final class NativeLibraryLoader {

    /** The logical name of the native library (no OS prefix or extension). */
    private static final String LIBRARY_NAME = "data_helper";

    /**
     * The name of the sibling directory that holds the native library.
     * This directory must be a sibling of the folder containing the JAR.
     */
    private static final String BIN_DIR_NAME = "bin";

    /**
     * Guards against loading the library more than once.
     * Declared {@code volatile} so that all threads see the update immediately.
     */
    private static volatile boolean loaded = false;

    // Utility class — do not instantiate.
    private NativeLibraryLoader() {}

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Locates and loads the {@code data_helper} native library.
     *
     * <p>This method is idempotent: once the library has been loaded
     * successfully, subsequent calls return immediately without reloading.
     *
     * @throws NativeLibraryException if the library cannot be located or loaded.
     */
    public static synchronized void load() {
        if (loaded) {
            return; // Already loaded — nothing to do.
        }

        log("Initializing native library: " + LIBRARY_NAME);

        Path jarDir     = resolveJarDirectory();
        Path installRoot = resolveInstallRoot(jarDir);
        Path binDir     = resolveBinDirectory(installRoot);
        Path libraryFile = resolveLibraryFile(binDir);

        loadLibrary(libraryFile);

        loaded = true;
        log("Native library loaded successfully from: " + libraryFile);
    }

    /**
     * Returns {@code true} if the native library has already been loaded.
     *
     * @return {@code true} after a successful call to {@link #load()}.
     */
    public static boolean isLoaded() {
        return loaded;
    }

    // =========================================================================
    // Path Resolution
    // =========================================================================

    /**
     * Resolves the absolute path of the directory that contains the running
     * JAR file.
     *
     * <p>This is the critical step that makes the loader portable. By anchoring
     * to the JAR's own location on disk, the tool is independent of the JVM's
     * current working directory ({@code user.dir}), which can vary depending on
     * how the user launches the application.
     *
     * @return the absolute, normalized path of the directory containing the JAR.
     * @throws NativeLibraryException if the JAR location cannot be determined.
     */
    private static Path resolveJarDirectory() {
        try {
            ProtectionDomain domain = NativeLibraryLoader.class.getProtectionDomain();
            if (domain == null) {
                throw new NativeLibraryException(
                    "Cannot determine JAR location: ProtectionDomain is null. " +
                    "Ensure the application is launched from a standard JAR file.");
            }

            CodeSource codeSource = domain.getCodeSource();
            if (codeSource == null) {
                throw new NativeLibraryException(
                    "Cannot determine JAR location: CodeSource is null. " +
                    "Ensure the application is launched from a standard JAR file.");
            }

            URL location = codeSource.getLocation();
            if (location == null) {
                throw new NativeLibraryException(
                    "Cannot determine JAR location: CodeSource URL is null.");
            }

            // Convert URL → URI → Path to correctly handle spaces and special
            // characters in the installation path (e.g. "C:/My Tools/app/...").
            Path jarPath = Path.of(location.toURI()).toAbsolutePath().normalize();

            // getLocation() points to the JAR file itself; get its parent directory.
            Path jarDir = Files.isDirectory(jarPath) ? jarPath : jarPath.getParent();

            if (jarDir == null) {
                throw new NativeLibraryException(
                    "Resolved JAR path has no parent directory: " + jarPath);
            }

            log("JAR directory resolved to: " + jarDir);
            return jarDir;

        } catch (URISyntaxException e) {
            throw new NativeLibraryException(
                "JAR location URL is not a valid URI. " +
                "Check for illegal characters in the installation path.", e);
        } catch (InvalidPathException e) {
            throw new NativeLibraryException(
                "Cannot convert JAR URL to a file system path.", e);
        }
    }

    /**
     * Resolves the installation root by navigating one level up from the
     * directory that contains the JAR.
     *
     * <p>Given the layout {@code <install-root>/app/data-analysis-tool.jar},
     * this returns {@code <install-root>}.
     *
     * @param jarDir the directory containing the running JAR.
     * @return the parent of {@code jarDir}, i.e. the installation root.
     * @throws NativeLibraryException if {@code jarDir} has no parent.
     */
    private static Path resolveInstallRoot(Path jarDir) {
        Path installRoot = jarDir.getParent();

        if (installRoot == null) {
            throw new NativeLibraryException(
                "Cannot resolve installation root: JAR directory '" + jarDir +
                "' has no parent. " +
                "Ensure the tool is installed with the expected layout: " +
                "<install-root>/app/<jar-file> and <install-root>/bin/<library>.");
        }

        log("Installation root resolved to: " + installRoot);
        return installRoot;
    }

    /**
     * Resolves the {@code bin/} directory from the installation root and
     * validates that it exists and is a directory.
     *
     * @param installRoot the resolved installation root directory.
     * @return the absolute path to the {@code bin/} directory.
     * @throws NativeLibraryException if {@code bin/} does not exist or is not a directory.
     */
    private static Path resolveBinDirectory(Path installRoot) {
        Path binDir = installRoot.resolve(BIN_DIR_NAME).normalize();

        if (!Files.exists(binDir)) {
            throw new NativeLibraryException(
                "Native library directory not found: " + binDir + ". " +
                "Ensure the '" + BIN_DIR_NAME + "/' directory is present as a " +
                "sibling of the application's execution folder.");
        }

        if (!Files.isDirectory(binDir)) {
            throw new NativeLibraryException(
                "Expected a directory at '" + binDir + "', but found a file. " +
                "Check the tool's installation layout.");
        }

        log("Native library directory resolved to: " + binDir);
        return binDir;
    }

    /**
     * Builds the platform-specific library file name, resolves it inside
     * the {@code bin/} directory, and validates that it exists and is readable.
     *
     * <ul>
     *   <li>Linux   → {@code libdata_helper.so}</li>
     *   <li>macOS   → {@code libdata_helper.dylib}</li>
     *   <li>Windows → {@code data_helper.dll}</li>
     * </ul>
     *
     * @param binDir the resolved {@code bin/} directory.
     * @return the absolute path to the native library file.
     * @throws NativeLibraryException if the file does not exist or is not readable.
     */
    private static Path resolveLibraryFile(Path binDir) {
        String fileName = buildPlatformLibraryFileName();
        Path libraryFile = binDir.resolve(fileName).normalize();

        log("Expecting native library file at: " + libraryFile);

        if (!Files.exists(libraryFile)) {
            throw new NativeLibraryException(
                "Native library file not found: " + libraryFile + ". " +
                "Ensure '" + fileName + "' is present in the '" + BIN_DIR_NAME + "/' directory.");
        }

        if (!Files.isReadable(libraryFile)) {
            throw new NativeLibraryException(
                "Native library file is not readable: " + libraryFile + ". " +
                "Check file permissions.");
        }

        return libraryFile;
    }

    // =========================================================================
    // Platform Detection
    // =========================================================================

    /**
     * Builds the platform-specific file name for the {@code data_helper} library.
     *
     * @return the OS-appropriate library file name.
     * @throws NativeLibraryException for unrecognized or unsupported operating systems.
     */
    private static String buildPlatformLibraryFileName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String fileName;

        if (os.contains("win")) {
            fileName = LIBRARY_NAME + ".dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            fileName = "lib" + LIBRARY_NAME + ".dylib";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            fileName = "lib" + LIBRARY_NAME + ".so";
        } else {
            throw new NativeLibraryException(
                "Unsupported operating system: '" + os + "'. " +
                "Cannot determine native library file name for '" + LIBRARY_NAME + "'.");
        }

        log("Platform detected as '" + os + "'; library file name: " + fileName);
        return fileName;
    }

    // =========================================================================
    // Library Loading
    // =========================================================================

    /**
     * Calls {@link System#load(String)} with the absolute path of the library.
     *
     * <p>{@code System.load()} is used instead of {@code System.loadLibrary()}
     * because we have a known absolute path. {@code loadLibrary()} only searches
     * directories listed in {@code java.library.path}, which would require users
     * to configure their environment — defeating the goal of portability.
     *
     * @param libraryFile the absolute, validated path to the native library file.
     * @throws NativeLibraryException if {@link System#load(String)} throws.
     */
    private static void loadLibrary(Path libraryFile) {
        try {
            System.load(libraryFile.toString());
        } catch (UnsatisfiedLinkError e) {
            throw new NativeLibraryException(
                "Failed to load native library from '" + libraryFile + "'. " +
                "The file exists but could not be linked. Common causes: " +
                "(1) Architecture mismatch (e.g., 32-bit library on a 64-bit JVM), " +
                "(2) Missing transitive native dependencies, " +
                "(3) Corrupted library file.", e);
        }
    }

    // =========================================================================
    // Logging
    // =========================================================================

    /**
     * Minimal structured logging to standard output.
     * Replace with your engine's logging framework (SLF4J, Log4j, etc.) as needed.
     *
     * @param message the message to log.
     */
    private static void log(String message) {
        System.out.println("[NativeLibraryLoader] " + message);
    }

    // =========================================================================
    // Custom Exception
    // =========================================================================

    /**
     * Thrown when the {@code data_helper} native library cannot be located
     * or loaded. Wraps lower-level exceptions with actionable diagnostic messages.
     */
    public static final class NativeLibraryException extends RuntimeException {

        public NativeLibraryException(String message) {
            super(message);
        }

        public NativeLibraryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}