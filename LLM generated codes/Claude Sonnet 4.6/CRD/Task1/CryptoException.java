package com.app.crypto;

/**
 * Thrown when a native cryptographic operation fails.
 *
 * <p>{@link #getErrorCode()} surfaces the raw OpenSSL error code
 * (from {@code ERR_get_error()}) for diagnostic purposes.
 * {@link #getOperation()} identifies which crypto operation triggered
 * the failure, enabling structured error handling and monitoring.</p>
 *
 * <p>This is an unchecked exception — callers that cannot recover
 * (e.g. a corrupted ciphertext or an invalid key) are not forced to
 * declare it, while those needing fine-grained control can catch it.</p>
 */
public class CryptoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Identifies the cryptographic operation that failed.
     * Ordinal values must stay in sync with {@code OP_*} constants
     * in {@code crypto_processor.cpp}.
     */
    public enum Operation {
        ENCRYPT,
        DECRYPT,
        HASH,
        HMAC,
        KEY_DERIVATION,
        RANDOM_BYTES,
        CONTEXT_INIT,
        UNKNOWN
    }

    private final long      opensslErrorCode;
    private final Operation operation;

    // ── constructors ──────────────────────────────────────────────────

    public CryptoException(String message) {
        super(message);
        this.opensslErrorCode = 0L;
        this.operation        = Operation.UNKNOWN;
    }

    public CryptoException(String message, Operation operation) {
        super(message);
        this.opensslErrorCode = 0L;
        this.operation        = operation;
    }

    /**
     * Primary constructor — called from native code via JNI.
     *
     * @param message          human-readable description (may include OpenSSL error string)
     * @param opensslErrorCode raw value from {@code ERR_get_error()}, or 0
     * @param operationOrdinal ordinal of the failing {@link Operation}
     */
    public CryptoException(String message, long opensslErrorCode, int operationOrdinal) {
        super(message + (opensslErrorCode != 0
                ? " [OpenSSL error: 0x" + Long.toHexString(opensslErrorCode) + "]" : ""));
        this.opensslErrorCode = opensslErrorCode;
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
        this.opensslErrorCode = 0L;
        this.operation        = Operation.UNKNOWN;
    }

    // ── accessors ─────────────────────────────────────────────────────

    /**
     * Returns the raw OpenSSL error code ({@code ERR_get_error()}),
     * or {@code 0} if not available.
     */
    public long getOpenSSLErrorCode() { return opensslErrorCode; }

    /** Returns the cryptographic operation that triggered this exception. */
    public Operation getOperation() { return operation; }
}