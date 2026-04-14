package com.app.compression;

/**
 * Thrown when a native compression or decompression operation fails.
 *
 * <p>The {@link #getZlibErrorCode()} method exposes the raw zlib return code
 * (e.g. {@code Z_DATA_ERROR}, {@code Z_MEM_ERROR}) for diagnostic purposes.</p>
 */
public class CompressionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Raw zlib error code, or {@code 0} if not applicable. */
    private final int zlibErrorCode;

    public CompressionException(String message) {
        super(message);
        this.zlibErrorCode = 0;
    }

    public CompressionException(String message, int zlibErrorCode) {
        super(message + " (zlib error code: " + zlibErrorCode + ")");
        this.zlibErrorCode = zlibErrorCode;
    }

    public CompressionException(String message, Throwable cause) {
        super(message, cause);
        this.zlibErrorCode = 0;
    }

    /**
     * Returns the raw zlib error code associated with this exception,
     * or {@code 0} if none was recorded.
     */
    public int getZlibErrorCode() {
        return zlibErrorCode;
    }
}