package com.security.native_crypto;

import java.nio.charset.StandardCharsets;

/**
 * Central manager that composes {@link Sha256Hasher}, {@link RsaDigitalSigner},
 * and {@link SecureRandomGenerator} into a unified, lifecycle-managed API.
 *
 * <p>Enforces a strict initialisation order:</p>
 * <ol>
 *   <li>Seed the CSPRNG first (no key material generated before entropy
 *       is available).</li>
 *   <li>Initialise the SHA-256 engine.</li>
 *   <li>Load the RSA private key.</li>
 * </ol>
 *
 * <p>All public methods guard against use-before-initialise with a clear
 * {@link IllegalStateException} instead of a JVM crash or silent failure.</p>
 */
public class CryptographyManager {

    private final Sha256Hasher          sha256Hasher;
    private final RsaDigitalSigner      rsaSigner;
    private final SecureRandomGenerator rng;

    private boolean initialised = false;

    public CryptographyManager() {
        this.sha256Hasher = new Sha256Hasher();
        this.rsaSigner    = new RsaDigitalSigner();
        this.rng          = new SecureRandomGenerator();
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Initialises all native cryptographic components in the correct order.
     *
     * <p>Must be called once at application startup before any other method.
     * The RSA private key must be provided as DER-encoded bytes.</p>
     *
     * @param privateKeyDer DER-encoded RSA private key (PKCS#8 or PKCS#1)
     * @throws CryptoOperationException if any native initialisation step fails
     * @throws IllegalArgumentException if {@code privateKeyDer} is null or empty
     */
    public void initialise(byte[] privateKeyDer) throws CryptoOperationException {
        if (privateKeyDer == null || privateKeyDer.length == 0) {
            throw new IllegalArgumentException("privateKeyDer must not be null or empty.");
        }

        // 1. Seed the CSPRNG — must come first; key generation depends on entropy
        rng.seedGenerator();

        // 2. Initialise the SHA-256 digest engine
        sha256Hasher.initDigestEngine();

        // 3. Load the RSA private key into the native signing context
        rsaSigner.loadPrivateKey(privateKeyDer);

        initialised = true;
        System.out.println("CryptographyManager initialised successfully.");
    }

    // ------------------------------------------------------------------
    // SHA-256
    // ------------------------------------------------------------------

    /**
     * Computes the SHA-256 hash of the supplied data and returns a
     * lowercase hex string.
     *
     * @param data raw bytes to hash; must not be {@code null}
     * @return 64-character lowercase hex SHA-256 digest
     * @throws CryptoOperationException if the native hash computation fails
     * @throws IllegalStateException    if {@link #initialise} has not been called
     */
    public String computeSha256Hex(byte[] data) throws CryptoOperationException {
        ensureInitialised();
        byte[] digest = sha256Hasher.computeHash(data);
        return sha256Hasher.toHexString(digest);
    }

    /**
     * Convenience overload that hashes a UTF-8 string.
     *
     * @param text the string to hash; must not be {@code null}
     * @return 64-character lowercase hex SHA-256 digest
     * @throws CryptoOperationException if the native hash computation fails
     */
    public String computeSha256Hex(String text) throws CryptoOperationException {
        ensureInitialised();
        if (text == null) {
            throw new IllegalArgumentException("text must not be null.");
        }
        return computeSha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // RSA Signing
    // ------------------------------------------------------------------

    /**
     * Signs the supplied message with the loaded RSA private key and returns
     * the raw signature bytes.
     *
     * @param message raw message bytes to sign; must not be {@code null}
     * @return raw RSA signature bytes
     * @throws CryptoOperationException if the native signing operation fails
     * @throws IllegalStateException    if {@link #initialise} has not been called
     */
    public byte[] signMessage(byte[] message) throws CryptoOperationException {
        ensureInitialised();
        byte[] signature = rsaSigner.signData(message);
        System.out.printf("Message signed — signature length: %d bytes.%n", signature.length);
        return signature;
    }

    /**
     * Signs a UTF-8 string message and validates the resulting signature
     * length against the expected RSA key size.
     *
     * @param message        the string message to sign
     * @param rsaKeySizeBits the RSA key size in bits (e.g., 2048, 4096)
     * @return validated raw RSA signature bytes
     * @throws CryptoOperationException if signing fails or the signature
     *                                  length is unexpected
     */
    public byte[] signAndValidate(String message, int rsaKeySizeBits)
            throws CryptoOperationException {
        ensureInitialised();
        byte[] signature = rsaSigner.signData(
                message.getBytes(StandardCharsets.UTF_8));
        if (!rsaSigner.isSignatureLengthValid(signature, rsaKeySizeBits)) {
            throw new CryptoOperationException(
                String.format("Unexpected signature length %d for a %d-bit key.",
                    signature.length, rsaKeySizeBits));
        }
        return signature;
    }

    // ------------------------------------------------------------------
    // Secure Random
    // ------------------------------------------------------------------

    /**
     * Generates {@code byteCount} cryptographically secure random bytes.
     *
     * @param byteCount number of bytes to generate (1 – 65 536)
     * @return random byte array
     * @throws CryptoOperationException if the native CSPRNG fails
     * @throws IllegalStateException    if {@link #initialise} has not been called
     */
    public byte[] generateRandomBytes(int byteCount) throws CryptoOperationException {
        ensureInitialised();
        return rng.generateSecureBytes(byteCount);
    }

    /**
     * Generates a printable random hex token of the specified byte width.
     *
     * @param byteCount number of raw bytes (the hex string will be twice as long)
     * @return lowercase hex token string
     * @throws CryptoOperationException if the native CSPRNG fails
     */
    public String generateHexToken(int byteCount) throws CryptoOperationException {
        ensureInitialised();
        return rng.generateHexToken(byteCount);
    }

    /**
     * Generates 32 bytes of cryptographically secure AES-256 key material.
     *
     * @return 32-byte array of key material
     * @throws CryptoOperationException if the native CSPRNG fails
     */
    public byte[] generateAes256Key() throws CryptoOperationException {
        ensureInitialised();
        return rng.generateAes256KeyMaterial();
    }

    // ------------------------------------------------------------------
    // Entry point (smoke test)
    // ------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        // Load DER-encoded RSA private key from a secure source
        byte[] privateKeyDer = loadDerKey("/secure/private.der"); // placeholder

        CryptographyManager crypto = new CryptographyManager();
        crypto.initialise(privateKeyDer);

        // SHA-256
        String digest = crypto.computeSha256Hex("Hello, Security!");
        System.out.println("SHA-256 : " + digest);

        // RSA Signing
        byte[] signature = crypto.signAndValidate("Critical document payload", 2048);
        System.out.printf("RSA Signature: %d bytes%n", signature.length);

        // Secure Random
        String sessionToken = crypto.generateHexToken(32); // 256-bit token
        System.out.println("Session Token: " + sessionToken);

        byte[] aesKey = crypto.generateAes256Key();
        System.out.printf("AES-256 Key  : %d bytes%n", aesKey.length);
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private void ensureInitialised() {
        if (!initialised) {
            throw new IllegalStateException(
                "CryptographyManager has not been initialised. Call initialise() first.");
        }
    }

    private static byte[] loadDerKey(String path) {
        // Placeholder — in production, read from a hardware security module (HSM),
        // OS keystore, or encrypted vault rather than the filesystem directly.
        throw new UnsupportedOperationException("Provide a real DER key loader.");
    }
}