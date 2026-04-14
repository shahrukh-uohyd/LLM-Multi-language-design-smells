package com.app.compression;

/**
 * JNI entrypoint for native data compression operations.
 *
 * <p>Wraps low-level zlib compress/decompress through a dedicated native
 * library ("compression_native") so that it is completely decoupled from
 * the existing platform_utils library.</p>
 *
 * <p>Callers should prefer the higher-level {@link DataStorage} API; this
 * class is intentionally package-accessible for testability.</p>
 */
public class CompressionNative {

    // ---------------------------------------------------------------
    // Compression-level constants (mirror zlib values)
    // ---------------------------------------------------------------

    /** Use zlib's default compression level (typically 6). */
    public static final int LEVEL_DEFAULT = -1;

    /** No compression — data is stored as-is. Fastest, no size reduction. */
    public static final int LEVEL_NO_COMPRESSION = 0;

    /** Fastest compression. Minimal CPU at the cost of ratio. */
    public static final int LEVEL_BEST_SPEED = 1;

    /** Best compression ratio. Highest CPU usage. */
    public static final int LEVEL_BEST_COMPRESSION = 9;

    // ---------------------------------------------------------------
    // Library loading
    // ---------------------------------------------------------------

    static {
        try {
            System.loadLibrary("compression_native");
        } catch (UnsatisfiedLinkError e) {
            throw new ExceptionInInitializerError(
                "Failed to load native compression library: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Native method declarations
    // ---------------------------------------------------------------

    /**
     * Compresses {@code data} using zlib DEFLATE.
     *
     * @param data             raw bytes to compress; must not be null or empty
     * @param compressionLevel one of {@link #LEVEL_DEFAULT}, {@link #LEVEL_NO_COMPRESSION},
     *                         {@link #LEVEL_BEST_SPEED}, {@link #LEVEL_BEST_COMPRESSION},
     *                         or any integer in [0, 9]
     * @return compressed bytes (zlib format, with 2-byte header + Adler-32 checksum)
     * @throws IllegalArgumentException  if {@code data} is null/empty or level is out of range
     * @throws CompressionException      if the native compression operation fails
     */
    public native byte[] compress(byte[] data, int compressionLevel);

    /**
     * Decompresses {@code data} previously compressed by {@link #compress}.
     *
     * @param compressedData zlib-compressed bytes; must not be null or empty
     * @param originalSize   hint for the expected decompressed size (&gt; 0).
     *                       Pass {@code -1} if unknown (native side will grow buffer).
     * @return decompressed raw bytes
     * @throws IllegalArgumentException  if {@code compressedData} is null/empty
     * @throws CompressionException      if the data is corrupt or decompression fails
     */
    public native byte[] decompress(byte[] compressedData, int originalSize);

    /**
     * Returns the maximum number of bytes that {@link #compress} can produce
     * for an input of {@code inputSize} bytes. Useful for pre-allocating buffers.
     *
     * @param inputSize number of bytes of uncompressed input (&gt;= 0)
     * @return upper-bound byte count for the compressed output
     * @throws IllegalArgumentException if {@code inputSize} is negative
     */
    public native long compressBound(long inputSize);

    /**
     * Returns the zlib version string linked into the native library.
     *
     * @return version string, e.g. {@code "1.2.13"}
     */
    public native String zlibVersion();

    // ---------------------------------------------------------------
    // Java-side validation helpers (keep JNI layer thin)
    // ---------------------------------------------------------------

    /**
     * Validates compression level; throws {@link IllegalArgumentException} if invalid.
     */
    static void checkLevel(int level) {
        if (level != LEVEL_DEFAULT && (level < 0 || level > 9)) {
            throw new IllegalArgumentException(
                "compressionLevel must be -1 (default) or in [0, 9], got: " + level);
        }
    }

    /**
     * Validates that a byte array is non-null and non-empty.
     */
    static void checkData(byte[] data, String paramName) {
        if (data == null) {
            throw new IllegalArgumentException(paramName + " must not be null");
        }
        if (data.length == 0) {
            throw new IllegalArgumentException(paramName + " must not be empty");
        }
    }
}