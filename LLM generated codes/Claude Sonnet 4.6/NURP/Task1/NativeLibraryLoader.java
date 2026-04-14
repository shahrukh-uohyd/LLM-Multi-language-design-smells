package com.engine.native_support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Handles the loading of the native physics_core library at runtime.
 *
 * <p>The native library file is expected to be located in the {@code native_libs/}
 * directory at the root of the project. This class supports loading from:
 * <ol>
 *   <li>The file system (during development/local runs)</li>
 *   <li>The classpath/JAR (when packaged as an executable JAR)</li>
 * </ol>
 *
 * <p>Supported platforms:
 * <ul>
 *   <li>Linux   : {@code libphysics_core.so}</li>
 *   <li>macOS   : {@code libphysics_core.dylib}</li>
 *   <li>Windows : {@code physics_core.dll}</li>
 * </ul>
 */
public class NativeLibraryLoader {

    /** The logical name of the native library (no prefix/extension). */
    private static final String LIBRARY_NAME = "physics_core";

    /** Relative path to the native_libs directory from the project root. */
    private static final String NATIVE_LIBS_DIR = "native_libs";

    /** Tracks whether the library has already been successfully loaded. */
    private static volatile boolean loaded = false;

    // Private constructor — this is a utility class.
    private NativeLibraryLoader() {}

    /**
     * Loads the {@code physics_core} native library.
     *
     * <p>First attempts to load from the local file system ({@code native_libs/} directory).
     * If not found there, falls back to extracting the library from the JAR/classpath
     * and loading it from a temporary file.
     *
     * @throws RuntimeException if the library cannot be located or loaded.
     */
    public static synchronized void load() {
        if (loaded) {
            return; // Already loaded; do nothing.
        }

        try {
            // Strategy 1: Load directly from the native_libs directory on the file system.
            if (loadFromFileSystem()) {
                loaded = true;
                System.out.println("[NativeLibraryLoader] Successfully loaded '" + LIBRARY_NAME
                        + "' from file system.");
                return;
            }

            // Strategy 2: Extract from classpath/JAR and load from a temp file.
            if (loadFromClasspath()) {
                loaded = true;
                System.out.println("[NativeLibraryLoader] Successfully loaded '" + LIBRARY_NAME
                        + "' from classpath/JAR.");
                return;
            }

            throw new RuntimeException(
                    "[NativeLibraryLoader] Could not find native library '" + LIBRARY_NAME
                    + "'. Ensure the library file is present in the '" + NATIVE_LIBS_DIR
                    + "/' directory or bundled in the JAR.");

        } catch (IOException e) {
            throw new RuntimeException(
                    "[NativeLibraryLoader] I/O error while loading native library '"
                    + LIBRARY_NAME + "'.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Attempts to load the library from the {@code native_libs/} directory
     * located in the current working directory (project root).
     *
     * @return {@code true} if the library was found and loaded successfully.
     */
    private static boolean loadFromFileSystem() {
        String libraryFileName = getLibraryFileName();
        Path libraryPath = Path.of(NATIVE_LIBS_DIR, libraryFileName);

        if (!Files.exists(libraryPath)) {
            System.out.println("[NativeLibraryLoader] Library not found at: "
                    + libraryPath.toAbsolutePath());
            return false;
        }

        System.load(libraryPath.toAbsolutePath().toString());
        return true;
    }

    /**
     * Attempts to locate the library on the classpath (e.g., inside a JAR),
     * copy it to a temporary file, and load it from there.
     *
     * @return {@code true} if the library was found on the classpath and loaded.
     * @throws IOException if a temporary file cannot be created or written to.
     */
    private static boolean loadFromClasspath() throws IOException {
        String libraryFileName = getLibraryFileName();
        // Classpath resource path mirrors the native_libs directory structure.
        String resourcePath = "/" + NATIVE_LIBS_DIR + "/" + libraryFileName;

        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.out.println("[NativeLibraryLoader] Library not found on classpath: "
                        + resourcePath);
                return false;
            }

            // Extract to a temp file so System.load() can access it.
            Path tempFile = Files.createTempFile("physics_core_", "_native");
            tempFile.toFile().deleteOnExit(); // Clean up on JVM exit.

            try (OutputStream out = Files.newOutputStream(tempFile)) {
                in.transferTo(out);
            }

            System.load(tempFile.toAbsolutePath().toString());
            return true;
        }
    }

    /**
     * Returns the platform-specific file name for the native library.
     *
     * <ul>
     *   <li>Linux   → {@code libphysics_core.so}</li>
     *   <li>macOS   → {@code libphysics_core.dylib}</li>
     *   <li>Windows → {@code physics_core.dll}</li>
     * </ul>
     *
     * @return the platform-specific library file name.
     * @throws UnsupportedOperationException for unrecognized operating systems.
     */
    private static String getLibraryFileName() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return LIBRARY_NAME + ".dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            return "lib" + LIBRARY_NAME + ".dylib";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "lib" + LIBRARY_NAME + ".so";
        } else {
            throw new UnsupportedOperationException(
                    "[NativeLibraryLoader] Unsupported operating system: " + os);
        }
    }
}