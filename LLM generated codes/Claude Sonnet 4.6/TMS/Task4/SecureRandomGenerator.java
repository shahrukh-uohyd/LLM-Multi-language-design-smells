package com.security.native_crypto;

/**
 * Provides native bindings for generating cryptographically secure
 * pseudo-random byte sequences.
 *
 * <p>The native layer is expected to seed directly from the operating
 * system's entropy source:</p>
 * <ul>
 *   <li><strong>Linux</strong>   – {@code getrandom(2)} syscall or
 *                                  {@code /dev/urandom}</li>
 *   <li><strong>Windows</strong> – {@code BCryptGenRandom} (CNG API)</li>
 *   <li><strong>macOS</strong>   – {@code getentropy(2)}</li>
 * </ul>
 *
 * <p>After seeding, a fast CSPRNG (e.g., ChaCha20 or AES-CTR in DRBG mode)
 * is used to expand the entropy for high-throughput generation, avoiding
 * repeated expensive kernel calls.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   SecureRandomGenerator rng = new SecureRandomGenerator();
 *   rng.seedGenerator();
 *   byte[] nonce = rng.generateSecureBytes(16);  // 128-bit nonce
 *   byte[] key   = rng.generateSecureBytes(32);  // 256-bit AES key material
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> A single instance is <em>not</em>
 * thread-safe. Create one instance per thread or synchronise externally.</p>
 */
public class SecureRandomGenerator {

    static {
        // Loads libsecure_random.so (Linux/macOS) or secure_random.dll (Windows)
        System.loadLibrary("secure_random");
    }

    // ------------------------------------------------------------------
    // Native method declarations
    // ------------------------------------------------------------------

    /**
     * Seeds the native CSPRNG from the operating system's entropy source.
     *
     * <p>Must be called once before any call to
     * {@link #generateSecureBytes(int)}.  The seed is collected from
     * the OS entropy pool (see class-level documentation for
     * platform-specific sources).  A minimum of 256 bits of entropy
     * is gathered before the generator is considered ready.</p>
     *
     * @throws CryptoOperationException if the OS entropy source is
     *                                  unavailable, returns insufficient
     *                                  bytes, or the CSPRNG self-test
     *                                  (NIST SP 800-90A health check) fails
     */
    public native void seedGenerator() throws CryptoOperationException;

    /**
     * Generates the requested number of cryptographically secure random bytes.
     *
     * <p>The output is suitable for use as:</p>
     * <ul>
     *   <li>Symmetric encryption keys (AES-128, AES-256)</li>
     *   <li>Initialisation vectors (IVs) and nonces</li>
     *   <li>Salt values for password hashing (bcrypt, Argon2)</li>
     *   <li>CSRF / session tokens</li>
     * </ul>
     *
     * @param byteCount the number of random bytes to generate;
     *                  must be between {@code 1} and {@code 65536} inclusive
     * @return a {@code byte[]} of length {@code byteCount} filled with
     *         cryptographically secure random data; never {@code null}
     * @throws CryptoOperationException if the CSPRNG output fails its
     *                                  continuous health test or if the
     *                                  native expansion step faults
     * @throws IllegalStateException    if {@link #seedGenerator()} has
     *                                  not been called first
     * @throws IllegalArgumentException if {@code byteCount} is &lt; 1 or
     *                                  &gt; 65536
     */
    public native byte[] generateSecureBytes(int byteCount) throws CryptoOperationException;

    // ------------------------------------------------------------------
    // Convenience wrappers (pure Java — no JNI overhead)
    // ------------------------------------------------------------------

    /** Maximum byte count accepted per {@link #generateSecureBytes} call. */
    public static final int MAX_BYTES_PER_CALL = 65_536;

    /**
     * Generates a random byte array and encodes it as a lowercase hexadecimal
     * string — useful for producing printable tokens or nonces.
     *
     * @param byteCount number of random bytes (pre-encoding); must satisfy
     *                  {@code 1 ≤ byteCount ≤ } {@link #MAX_BYTES_PER_CALL}
     * @return lowercase hex string of length {@code byteCount * 2}
     * @throws CryptoOperationException if the native generation fails
     */
    public String generateHexToken(int byteCount) throws CryptoOperationException {
        byte[] raw = generateSecureBytes(byteCount);
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Generates a random byte array of a size appropriate for use as an
     * AES-256 key (32 bytes / 256 bits).
     *
     * @return a 32-byte array of cryptographically secure random data
     * @throws CryptoOperationException if the native generation fails
     */
    public byte[] generateAes256KeyMaterial() throws CryptoOperationException {
        return generateSecureBytes(32);
    }
}