package com.security.native_crypto;

/**
 * Provides native bindings for computing SHA-256 message digests.
 *
 * <p>The native layer is expected to use a hardware-accelerated SHA-NI
 * path (Intel SHA Extensions / ARMv8 SHA2) when available, falling back
 * to a highly-optimised software implementation otherwise.  This delivers
 * throughput significantly above what the JVM's built-in
 * {@code java.security.MessageDigest} can achieve on large inputs.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   Sha256Hasher hasher = new Sha256Hasher();
 *   hasher.initDigestEngine();
 *   byte[] hash = hasher.computeHash("Hello, World!".getBytes(StandardCharsets.UTF_8));
 *   System.out.println(HexFormat.of().formatHex(hash)); // 64-char hex string
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> A single instance is <em>not</em>
 * thread-safe. Create one instance per thread or synchronise externally.</p>
 */
public class Sha256Hasher {

    static {
        // Loads libsha256_hasher.so (Linux/macOS) or sha256_hasher.dll (Windows)
        System.loadLibrary("sha256_hasher");
    }

    // ------------------------------------------------------------------
    // Native method declarations
    // ------------------------------------------------------------------

    /**
     * Initialises the native SHA-256 digest engine.
     *
     * <p>Must be called exactly once before any call to
     * {@link #computeHash(byte[])}.  Internally this probes CPU feature
     * flags (SHA-NI, AVX2), allocates the hardware acceleration context,
     * and performs a known-answer self-test to verify the implementation.</p>
     *
     * @throws CryptoOperationException if the CPU is unsupported, the
     *                                  self-test fails, or context
     *                                  allocation is refused by the OS
     */
    public native void initDigestEngine() throws CryptoOperationException;

    /**
     * Computes the SHA-256 hash of the supplied input data.
     *
     * <p>The input is processed in a single pass through the native
     * compression function.  For streaming / incremental hashing, call
     * this method with pre-assembled chunks.</p>
     *
     * @param data the raw bytes to hash; must not be {@code null};
     *             an empty array produces the well-known SHA-256 hash
     *             of the empty string
     * @return a 32-byte ({@code 256}-bit) array containing the SHA-256
     *         digest; never {@code null}
     * @throws CryptoOperationException if the native compression step
     *                                  encounters an internal error
     * @throws IllegalStateException    if {@link #initDigestEngine()} has
     *                                  not been called first
     * @throws IllegalArgumentException if {@code data} is {@code null}
     */
    public native byte[] computeHash(byte[] data) throws CryptoOperationException;

    // ------------------------------------------------------------------
    // Convenience wrappers (pure Java — no JNI overhead)
    // ------------------------------------------------------------------

    /**
     * Converts a raw 32-byte SHA-256 digest into a lowercase hexadecimal
     * string without any native call.
     *
     * @param hashBytes a 32-byte digest produced by {@link #computeHash}
     * @return 64-character lowercase hex string
     * @throws IllegalArgumentException if {@code hashBytes} is not exactly
     *                                  32 bytes
     */
    public String toHexString(byte[] hashBytes) {
        if (hashBytes == null || hashBytes.length != 32) {
            throw new IllegalArgumentException(
                "hashBytes must be a non-null 32-byte SHA-256 digest.");
        }
        StringBuilder sb = new StringBuilder(64);
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Convenience method that initialises the engine, hashes {@code data},
     * and returns the lowercase hex digest in a single call.
     *
     * @param data the raw bytes to hash
     * @return 64-character lowercase hex SHA-256 digest
     * @throws CryptoOperationException if the native layer reports an error
     */
    public String hashToHex(byte[] data) throws CryptoOperationException {
        initDigestEngine();
        byte[] digest = computeHash(data);
        return toHexString(digest);
    }
}