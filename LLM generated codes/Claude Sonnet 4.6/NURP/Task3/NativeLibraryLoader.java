package com.mathengine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

/**
 * Portable loader for the {@code math_engine} native library.
 *
 * <h2>Two-Strategy Resolution</h2>
 * <ol>
 *   <li><b>File system:</b> Looks in {@code native_libs/} relative to the JAR's
 *       own directory on disk. Ideal for development and exploded deployments.</li>
 *   <li><b>Classpath / JAR:</b> Extracts the library from inside the JAR to a
 *       secure temporary file and loads it from there. Ideal for distribution.</li>
 * </ol>
 *
 * <h2>Platform File Names</h2>
 * <ul>
 *   <li>Linux   → {@code libmath_engine.so}</li>
 *   <li>macOS   → {@code libmath_engine.dylib}</li>
 *   <li>Windows → {@code math_engine.dll}</li>
 * </ul>
 */
public final class NativeLibraryLoader {

    private static final String LIBRARY_NAME   = "math_engine";
    private static final String NATIVE_LIBS_DIR = "native_libs";

    private static volatile boolean loaded = false;

    private NativeLibraryLoader() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads the {@code math_engine} native library exactly once.
     * Subsequent calls are no-ops.
     *
     * @throws NativeLoaderException if the library cannot be located or loaded.
     */
    public static synchronized void load() {
        if (loaded) return;

        log("Initializing native library: " + LIBRARY_NAME);

        if (tryLoadFromFileSystem()) {
            loaded = true;
            return;
        }
        if (tryLoadFromClasspath()) {
            loaded = true;
            return;
        }

        throw new NativeLoaderException(
                "Could not locate native library '" + platformFileName() + "' " +
                "in native_libs/ directory or on the classpath. " +
                "Ensure the library is compiled and packaged correctly.");
    }

    public static boolean isLoaded() {
        return loaded;
    }

    // -------------------------------------------------------------------------
    // Strategy 1 — File system (development / exploded layout)
    // -------------------------------------------------------------------------

    private static boolean tryLoadFromFileSystem() {
        try {
            Path jarDir  = resolveJarDirectory();
            Path libFile = jarDir.resolve(NATIVE_LIBS_DIR)
                                 .resolve(platformFileName())
                                 .normalize()
                                 .toAbsolutePath();

            if (!Files.exists(libFile) || !Files.isReadable(libFile)) {
                log("Not found on file system: " + libFile);
                return false;
            }

            System.load(libFile.toString());
            log("Loaded from file system: " + libFile);
            return true;

        } catch (Exception e) {
            log("File-system strategy failed: " + e.getMessage());
            return false;
        }
    }

    private static Path resolveJarDirectory() throws URISyntaxException {
        CodeSource cs = NativeLibraryLoader.class
                            .getProtectionDomain().getCodeSource();
        if (cs == null) throw new NativeLoaderException("CodeSource is null.");

        URL location = cs.getLocation();
        if (location == null) throw new NativeLoaderException("CodeSource URL is null.");

        Path p = Path.of(location.toURI()).toAbsolutePath().normalize();
        return Files.isDirectory(p) ? p : p.getParent();
    }

    // -------------------------------------------------------------------------
    // Strategy 2 — Classpath / JAR extraction
    // -------------------------------------------------------------------------

    private static boolean tryLoadFromClasspath() {
        String resourcePath = "/" + NATIVE_LIBS_DIR + "/" + platformFileName();
        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                log("Not found on classpath: " + resourcePath);
                return false;
            }

            Path tmp = Files.createTempFile("math_engine_", "_native");
            tmp.toFile().deleteOnExit();

            try (OutputStream out = Files.newOutputStream(tmp)) {
                in.transferTo(out);
            }

            System.load(tmp.toAbsolutePath().toString());
            log("Loaded from classpath (temp: " + tmp + "): " + resourcePath);
            return true;

        } catch (IOException e) {
            log("Classpath strategy failed: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Platform helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the OS-specific file name for the native library.
     *
     * @throws NativeLoaderException for unrecognized operating systems.
     */
    static String platformFileName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win"))                                   return LIBRARY_NAME + ".dll";
        if (os.contains("mac") || os.contains("darwin"))         return "lib" + LIBRARY_NAME + ".dylib";
        if (os.contains("nix") || os.contains("nux") || os.contains("aix"))
                                                                  return "lib" + LIBRARY_NAME + ".so";
        throw new NativeLoaderException("Unsupported OS: " + os);
    }

    private static void log(String msg) {
        System.out.println("[NativeLibraryLoader] " + msg);
    }

    // -------------------------------------------------------------------------
    // Exception
    // -------------------------------------------------------------------------

    /** Thrown when the native library cannot be located or loaded. */
    public static final class NativeLoaderException extends RuntimeException {
        public NativeLoaderException(String message)                  { super(message); }
        public NativeLoaderException(String message, Throwable cause) { super(message, cause); }
    }
}