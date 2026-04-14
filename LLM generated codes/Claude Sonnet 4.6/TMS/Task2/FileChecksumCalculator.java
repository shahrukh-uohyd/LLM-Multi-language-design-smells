package com.hardware.native_utils;

import java.io.File;

/**
 * Provides high-speed native file checksum computation.
 *
 * <p>The native layer is expected to use a hardware-accelerated or
 * highly optimised algorithm (e.g., xxHash, CRC-32C via SSE4.2 intrinsics)
 * that outperforms a pure-Java implementation on large files.</p>
 *
 * <p>Usage:
 * <pre>{@code
 *   FileChecksumCalculator calc = new FileChecksumCalculator();
 *   calc.initChecksumEngine();
 *   String checksum = calc.computeChecksum("/var/data/largefile.bin");
 * }</pre>
 * </p>
 */
public class FileChecksumCalculator {

    static {
        // Loads libfile_checksum.so (Linux/macOS) or file_checksum.dll (Windows)
        System.loadLibrary("file_checksum");
    }

    /**
     * Initialises the native checksum engine.
     *
     * <p>Must be called once before any {@link #computeChecksum(String)} call.
     * Allocates hardware acceleration contexts (e.g., AES-NI, CRC32C pipelines)
     * and performs any one-time lookup-table setup.</p>
     *
     * @throws IllegalStateException if the native engine fails to initialise
     */
    public native void initChecksumEngine();

    /**
     * Computes the checksum of the file at the specified path.
     *
     * <p>The file is read in native memory-mapped chunks for maximum throughput.
     * The returned string is a lowercase hexadecimal digest.</p>
     *
     * @param filePath the absolute or relative path to the target file;
     *                 must not be {@code null} or empty
     * @return a non-null hexadecimal checksum string (e.g., {@code "a3f2c1d0..."})
     * @throws IllegalArgumentException if {@code filePath} is null or blank
     * @throws RuntimeException         if the file cannot be read or the native
     *                                  computation encounters an I/O error
     */
    public native String computeChecksum(String filePath);

    // -----------------------------------------------------------------------
    // Convenience wrapper (pure Java — no JNI overhead)
    // -----------------------------------------------------------------------

    /**
     * Validates the integrity of a file by comparing its live checksum
     * against a known-good expected value.
     *
     * @param filePath         path to the file to verify
     * @param expectedChecksum the expected hexadecimal checksum string
     * @return {@code true} if the computed checksum matches {@code expectedChecksum};
     *         {@code false} otherwise
     */
    public boolean verifyChecksum(String filePath, String expectedChecksum) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must not be null or blank.");
        }
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            throw new IllegalArgumentException("expectedChecksum must not be null or blank.");
        }
        String actual = computeChecksum(filePath);
        return actual.equalsIgnoreCase(expectedChecksum);
    }
}