package com.imagefilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

/**
 * Portable, two-strategy loader for the {@code image_filter} native library.
 *
 * <h2>Resolution Order</h2>
 * <ol>
 *   <li><b>File-system strategy:</b> Looks for the library in a {@code native_libs/}
 *       sub-directory that sits alongside the running JAR on disk.
 *       Preferred during development and in exploded deployments.</li>
 *   <li><b>Classpath strategy:</b> When running from a fat-JAR, extracts the
 *       bundled library to a secure temp file and loads it from there.</li>
 * </ol>
 *
 * <h2>Platform File Names</h2>
 * <ul>
 *   <li>Linux   → {@code libimage_filter.so}</li>
 *   <li>macOS   → {@code libimage_filter.dylib}</li>
 *   <li>Windows → {@code image_filter.dll}</li>
 * </ul>
 */
public final class NativeLibraryLoader {

    private static final String LIBRARY_NAME    = "image_filter";
    private static final String NATIVE_LIBS_DIR = "native_libs";

    /** Guards against loading the library more than once across threads. */
    private static volatile boolean loaded = false;

    private NativeLibraryLoader() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads the {@code image_filter} native library exactly once.
     * Subsequent calls are silent no-ops.
     *
     * @throws NativeLoaderException if the library cannot be located or loaded.
     */
    public static synchronized void load() {
        if (loaded) return;

        log("Initializing native library: " + LIBRARY_NAME);

        if (tryLoadFromFileSystem()) { loaded = true; return; }
        if (tryLoadFromClasspath())  { loaded = true; return; }

        throw new NativeLoaderException(
                "Could not locate '" + platformFileName() + "' in " +
                NATIVE_LIBS_DIR + "/ or on the classpath. " +
                "Ensure the native library is compiled and packaged.");
    }

    /** Returns {@code true} if the library is already loaded. */
    public static boolean isLoaded() { return loaded; }

    // -------------------------------------------------------------------------
    // Strategy 1 — file system (development / exploded layout)
    // -------------------------------------------------------------------------

    private static boolean tryLoadFromFileSystem() {
        try {
            Path libFile = resolveJarDirectory()
                    .resolve(NATIVE_LIBS_DIR)
                    .resolve(platformFileName())
                    .toAbsolutePath()
                    .normalize();

            if (!Files.exists(libFile) || !Files.isReadable(libFile)) {
                log("Not found on file system: " + libFile);
                return false;
            }
            System.load(libFile.toString());
            log("Loaded from file system:  " + libFile);
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

        URL loc = cs.getLocation();
        if (loc == null) throw new NativeLoaderException("CodeSource URL is null.");

        Path p = Path.of(loc.toURI()).toAbsolutePath().normalize();
        return Files.isDirectory(p) ? p : p.getParent();
    }

    // -------------------------------------------------------------------------
    // Strategy 2 — classpath / JAR extraction
    // -------------------------------------------------------------------------

    private static boolean tryLoadFromClasspath() {
        String resource = "/" + NATIVE_LIBS_DIR + "/" + platformFileName();
        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                log("Not found on classpath: " + resource);
                return false;
            }
            Path tmp = Files.createTempFile("image_filter_", "_native");
            tmp.toFile().deleteOnExit();
            try (OutputStream out = Files.newOutputStream(tmp)) {
                in.transferTo(out);
            }
            System.load(tmp.toAbsolutePath().toString());
            log("Loaded from classpath (tmp=" + tmp + "): " + resource);
            return true;
        } catch (IOException e) {
            log("Classpath strategy failed: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Platform helpers
    // -------------------------------------------------------------------------

    static String platformFileName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win"))                                 return LIBRARY_NAME + ".dll";
        if (os.contains("mac") || os.contains("darwin"))       return "lib" + LIBRARY_NAME + ".dylib";
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

    /** Thrown when the native library cannot be found or loaded. */
    public static final class NativeLoaderException extends RuntimeException {
        public NativeLoaderException(String message)                  { super(message); }
        public NativeLoaderException(String message, Throwable cause) { super(message, cause); }
    }
}