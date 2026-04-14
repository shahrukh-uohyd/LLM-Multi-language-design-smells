package com.security.native_crypto;

/**
 * Provides native bindings for creating RSA-PKCS#1 v1.5 / PSS digital
 * signatures using a private key loaded from DER-encoded bytes.
 *
 * <p>The native layer is expected to delegate to a battle-tested C library
 * such as OpenSSL ({@code libcrypto}) or mbedTLS to perform the following
 * steps:</p>
 * <ol>
 *   <li>Parse the DER-encoded PKCS#8 / PKCS#1 private key.</li>
 *   <li>Compute a SHA-256 digest of the message internally.</li>
 *   <li>Apply the RSASSA-PSS or PKCS#1 v1.5 padding scheme.</li>
 *   <li>Perform the modular exponentiation using the private key.</li>
 * </ol>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   byte[] derKey = Files.readAllBytes(Path.of("/secure/private.der"));
 *   byte[] message = "document to sign".getBytes(StandardCharsets.UTF_8);
 *
 *   RsaDigitalSigner signer = new RsaDigitalSigner();
 *   signer.loadPrivateKey(derKey);
 *   byte[] signature = signer.signData(message);
 * }</pre>
 *
 * <p><strong>Security note:</strong> The raw DER key bytes must be obtained
 * from a secure key store and should be zeroed from the Java heap as soon
 * as {@link #loadPrivateKey(byte[])} returns.</p>
 *
 * <p><strong>Thread safety:</strong> A single instance is <em>not</em>
 * thread-safe. Create one instance per thread or synchronise externally.</p>
 */
public class RsaDigitalSigner {

    static {
        // Loads librsa_signer.so (Linux/macOS) or rsa_signer.dll (Windows)
        System.loadLibrary("rsa_signer");
    }

    // ------------------------------------------------------------------
    // Native method declarations
    // ------------------------------------------------------------------

    /**
     * Loads and validates a DER-encoded RSA private key into the native
     * signing context.
     *
     * <p>Must be called before any call to {@link #signData(byte[])}.
     * Internally this parses the ASN.1 DER structure, validates that the
     * key modulus meets the minimum required size ({@code ≥ 2048 bits}),
     * and stores the key material in a native memory region that will not
     * be swapped to disk (via {@code mlock} / {@code VirtualLock}).</p>
     *
     * @param privateKeyDer a DER-encoded RSA private key in PKCS#8 or
     *                      PKCS#1 format; must not be {@code null} or empty
     * @throws CryptoOperationException if the DER data is malformed, the
     *                                  key is too short (&lt; 2048 bits),
     *                                  or {@code mlock} fails
     * @throws IllegalArgumentException if {@code privateKeyDer} is
     *                                  {@code null} or zero-length
     */
    public native void loadPrivateKey(byte[] privateKeyDer) throws CryptoOperationException;

    /**
     * Creates an RSA digital signature over the supplied message bytes.
     *
     * <p>The native layer hashes the message with SHA-256 and signs the
     * digest using the private key loaded by {@link #loadPrivateKey}.
     * The signature scheme (PKCS#1 v1.5 or PSS) is selected at native
     * library compile time.</p>
     *
     * @param message the raw message bytes to sign; must not be {@code null};
     *                the caller is responsible for any encoding (e.g., UTF-8)
     * @return a byte array containing the raw RSA signature; the length equals
     *         the key modulus size in bytes (e.g., 256 bytes for a 2048-bit key)
     * @throws CryptoOperationException if the signing operation fails in the
     *                                  native layer (e.g., padding error,
     *                                  engine fault)
     * @throws IllegalStateException    if {@link #loadPrivateKey(byte[])} has
     *                                  not been called first
     * @throws IllegalArgumentException if {@code message} is {@code null}
     */
    public native byte[] signData(byte[] message) throws CryptoOperationException;

    // ------------------------------------------------------------------
    // Convenience wrappers (pure Java — no JNI overhead)
    // ------------------------------------------------------------------

    /**
     * Validates that a produced signature has the expected byte length for a
     * given RSA key size, providing a quick sanity check before transmitting
     * the signature over the wire.
     *
     * @param signature       the raw signature bytes returned by
     *                        {@link #signData(byte[])}
     * @param rsaKeySizeBits  the RSA key size in bits (e.g., 2048, 4096)
     * @return {@code true} if the signature length matches the expected
     *         modulus size; {@code false} otherwise
     * @throws IllegalArgumentException if {@code rsaKeySizeBits} is not a
     *                                  positive multiple of 8
     */
    public boolean isSignatureLengthValid(byte[] signature, int rsaKeySizeBits) {
        if (rsaKeySizeBits <= 0 || rsaKeySizeBits % 8 != 0) {
            throw new IllegalArgumentException(
                "rsaKeySizeBits must be a positive multiple of 8.");
        }
        int expectedBytes = rsaKeySizeBits / 8;
        return signature != null && signature.length == expectedBytes;
    }
}