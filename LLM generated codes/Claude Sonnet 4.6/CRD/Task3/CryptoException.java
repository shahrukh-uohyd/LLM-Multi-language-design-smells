package com.app.crypto;

/**
 * Thrown when a native encryption or decryption operation fails.
 *
 * <p>The {@link #getErrorCode()} method surfaces the raw OpenSSL error
 * code (from {@code ERR_get_error()}) for diagnostic and logging purposes.
 * The {@link #getOperation()} method identifies which operation failed.</p>
 *
 * <p>This is an unchecked exception — callers that cannot sensibly recover
 * (e.g. a corrupted ciphertext) are not forced to handle it, while callers
 * that need fine-grained control can catch it explicitly.</p>
 */
public class CryptoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** The crypto operation that triggered this exception. */
    public enum Operation {
        ENCRYPT,
        DECRYPT,
        KEY_DERIVATION,
        CONTEXT_INIT,
        UNKNOWN
    }

    private final long   errorCode;
    private final Operation operation;

    // ── constructors ──────────────────────────────────────────────────

    public CryptoException(String message) {
        super(message);
        this.errorCode = 0L;
        this.operation = Operation.UNKNOWN;
    }

    public CryptoException(String message, Operation operation) {
        super(message);
        this.errorCode = 0L;
        this.operation = operation;
    }

    /**
     * Primary constructor called from native code via JNI.
     *
     * @param message   human-readable description (may include OpenSSL error string)
     * @param errorCode raw OpenSSL error code ({@code ERR_get_error()}) or 0
     * @param operation the operation that failed (ordinal of {@link Operation})
     */
    public CryptoException(String message, long errorCode, int operationOrdinal) {
        super(message + (errorCode != 0 ? " [OpenSSL error: 0x"
                + Long.toHexString(errorCode) + "]" : ""));
        this.errorCode = errorCode;
        Operation op;
        try {
            op = Operation.values()[operationOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            op = Operation.UNKNOWN;
        }
        this.operation = op;
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 0L;
        this.operation = Operation.UNKNOWN;
    }

    // ── accessors ─────────────────────────────────────────────────────

    /**
     * Returns the raw OpenSSL error code, or {@code 0} if not available.
     */
    public long getErrorCode() { return errorCode; }

    /**
     * Returns which operation produced this exception.
     */
    public Operation getOperation() { return operation; }
}