package com.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

/**
 * Portable, two-strategy loader for the {@code native_serializer} library.
 *
 * <h2>Resolution Order</h2>
 * <ol>
 *   <li><b>File-system strategy</b> — looks for {@code native_libs/<libfile>}
 *       in the same directory that contains the running JAR. Best for local
 *       development and exploded deployments.</li>
 *   <li><b>Classpath strategy</b> — when running from a fat JAR the library
 *       is bundled as a resource. It is extracted to a JVM-managed temp file
 *       and loaded from there. Best for single-JAR distribution.</li>
 * </ol>
 *
 * <h2>Platform File Names</h2>
 * <ul>
 *   <li>Linux   → {@code libnative_serializer.so}</li>
 *   <li>macOS   → {@code libnative_serializer.dylib}</li>
 *   <li>Windows → {@code native_serializer.dll}</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>{@link #load()} is {@code synchronized} and guarded by a
 * {@code volatile} flag so that it is safe to call from multiple
 * threads during application start-up.
 */
public final class NativeLibraryLoader {

    private static final String LIBRARY_NAME    = "native_serializer";
    private static final String NATIVE_LIBS_DIR = "native_libs";

    /** Set to {@code true} only after a successful {@link System#load} call. */
    private static volatile boolean loaded = false;

    private NativeLibraryLoader() {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads the {@code native_serializer} library exactly once.
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
                "Ensure the library is compiled and placed in native_libs/.");
    }

    /** Returns {@code true} if the library has already been loaded. */
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
            log("Loaded from file system: " + libFile);
            return true;

        } catch (Exception e) {
            log("File-system strategy failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the directory that contains the running JAR file.
     * Uses {@link CodeSource#getLocation()} so the result is always
     * anchored to the JAR's own location on disk — never to the JVM's
     * working directory.
     */
    private static Path resolveJarDirectory() throws URISyntaxException {
        CodeSource cs = NativeLibraryLoader.class
                .getProtectionDomain().getCodeSource();
        if (cs == null) throw new NativeLoaderException("CodeSource is null.");

        URL loc = cs.getLocation();
        if (loc == null) throw new NativeLoaderException("CodeSource URL is null.");

        // URL → URI → Path handles spaces and special characters in paths.
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
            // System.load() requires an on-disk file, so extract to a temp file.
            Path tmp = Files.createTempFile("native_serializer_", "_lib");
            tmp.toFile().deleteOnExit();          // clean up when the JVM exits
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
        if (os.contains("win"))
            return LIBRARY_NAME + ".dll";
        if (os.contains("mac") || os.contains("darwin"))
            return "lib" + LIBRARY_NAME + ".dylib";
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
        public NativeLoaderException(String msg)                 { super(msg); }
        public NativeLoaderException(String msg, Throwable cause){ super(msg, cause); }
    }
}