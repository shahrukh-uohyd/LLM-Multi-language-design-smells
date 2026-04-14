package com.security.native_crypto;

/**
 * Checked exception thrown when any native cryptographic operation fails.
 *
 * <p>All three native subsystems ({@link Sha256Hasher},
 * {@link RsaDigitalSigner}, {@link SecureRandomGenerator}) throw this
 * exception so that callers have a single, predictable type to handle
 * at the security boundary.</p>
 *
 * <p>The {@code cause} chain always wraps the original native error so
 * that stack traces are never silently discarded.</p>
 */
public class CryptoOperationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@code CryptoOperationException} with a detail message.
     *
     * @param message human-readable description of the failure
     */
    public CryptoOperationException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code CryptoOperationException} with a detail message
     * and the underlying cause.
     *
     * @param message human-readable description of the failure
     * @param cause   the original exception or error from the native layer
     */
    public CryptoOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}