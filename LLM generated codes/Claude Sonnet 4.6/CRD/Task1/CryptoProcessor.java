package com.app.crypto;

import java.util.Objects;

/**
 * JNI entrypoint for native cryptographic processing operations.
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li><b>AES-256-GCM</b>  — Authenticated encryption with associated data (AEAD).
 *       Wire format: {@code IV(12B) || Ciphertext || GCM-Tag(16B)}.</li>
 *   <li><b>SHA-256 / SHA-512</b> — Cryptographic digests of arbitrary data.</li>
 *   <li><b>HMAC-SHA256</b>  — Keyed-hash message authentication code.</li>
 *   <li><b>PBKDF2-SHA256</b>— Password-based key derivation (RFC 2898).</li>
 *   <li><b>Secure random</b> — Cryptographically random bytes via OpenSSL {@code RAND_bytes}.</li>
 * </ul>
 *
 * <h2>AES-256-GCM wire format</h2>
 * <pre>
 *  ┌─────────────┬──────────────────────────┬────────────────┐
 *  │  IV  (12 B) │  Ciphertext  (variable)  │  GCM Tag (16 B)│
 *  └─────────────┴──────────────────────────┴────────────────┘
 * </pre>
 *
 * <p>Callers should prefer the higher-level {@link com.app.security.SecurityManager} API.
 * This class is intentionally package-accessible for testability.</p>
 */
public class CryptoProcessor {

    // ── algorithm / size constants ────────────────────────────────────

    /** Required AES-256 key size in bytes. */
    public static final int AES_KEY_SIZE_BYTES   = 32;

    /** GCM nonce / IV size in bytes (NIST SP 800-38D recommended). */
    public static final int GCM_IV_SIZE_BYTES    = 12;

    /** GCM authentication tag size in bytes. */
    public static final int GCM_TAG_SIZE_BYTES   = 16;

    /** SHA-256 digest output size in bytes. */
    public static final int SHA256_SIZE_BYTES    = 32;

    /** SHA-512 digest output size in bytes. */
    public static final int SHA512_SIZE_BYTES    = 64;

    /** HMAC-SHA256 output size in bytes. */
    public static final int HMAC_SHA256_SIZE_BYTES = 32;

    /**
     * Minimum size of a valid AES-GCM cipher blob:
     * IV (12) + at least 1 byte of ciphertext + tag (16).
     */
    public static final int MIN_CIPHER_BLOB_BYTES =
            GCM_IV_SIZE_BYTES + 1 + GCM_TAG_SIZE_BYTES;

    // ── hash algorithm selector constants ────────────────────────────

    /** Selects SHA-256 for {@link #computeHash(byte[], int)}. */
    public static final int HASH_SHA256 = 0;

    /** Selects SHA-512 for {@link #computeHash(byte[], int)}. */
    public static final int HASH_SHA512 = 1;

    // ── library loading ───────────────────────────────────────────────

    static {
        try {
            System.loadLibrary("crypto_processor");
        } catch (UnsatisfiedLinkError e) {
            throw new ExceptionInInitializerError(
                    "Failed to load native crypto library 'crypto_processor': "
                    + e.getMessage());
        }
    }

    // ── native declarations ───────────────────────────────────────────

    /**
     * Encrypts {@code plaintext} with AES-256-GCM.
     *
     * <p>A cryptographically random 12-byte IV is generated internally.
     * The returned blob layout is: {@code IV || Ciphertext || GCM-Tag}.</p>
     *
     * @param key       AES key — must be exactly {@value #AES_KEY_SIZE_BYTES} bytes
     * @param plaintext data to encrypt — must not be null or empty
     * @param aad       additional authenticated data; authenticated but not encrypted
     *                  (may be null or empty)
     * @return          {@code IV || Ciphertext || GCM-Tag} as a single byte array
     * @throws IllegalArgumentException if key length is wrong or plaintext is null/empty
     * @throws CryptoException          if the native OpenSSL operation fails
     */
    public native byte[] encrypt(byte[] key, byte[] plaintext, byte[] aad);

    /**
     * Decrypts and authenticates a blob produced by {@link #encrypt}.
     *
     * @param key        AES key — must be exactly {@value #AES_KEY_SIZE_BYTES} bytes
     * @param cipherBlob {@code IV || Ciphertext || GCM-Tag}
     * @param aad        additional authenticated data used during encryption
     *                   (must match exactly; may be null or empty)
     * @return           decrypted plaintext bytes
     * @throws IllegalArgumentException if arguments are null or sizes are wrong
     * @throws CryptoException          if GCM tag verification fails (tampered data)
     *                                  or any native operation fails
     */
    public native byte[] decrypt(byte[] key, byte[] cipherBlob, byte[] aad);

    /**
     * Computes a cryptographic hash of {@code data}.
     *
     * @param data          data to hash — must not be null or empty
     * @param hashAlgorithm one of {@link #HASH_SHA256} or {@link #HASH_SHA512}
     * @return              digest bytes ({@value #SHA256_SIZE_BYTES} or
     *                      {@value #SHA512_SIZE_BYTES} bytes depending on algorithm)
     * @throws IllegalArgumentException if data is null/empty or algorithm is unknown
     * @throws CryptoException          if the native hash operation fails
     */
    public native byte[] computeHash(byte[] data, int hashAlgorithm);

    /**
     * Computes an HMAC-SHA256 of {@code data} using {@code key}.
     *
     * @param key  HMAC key — must not be null or empty
     * @param data data to authenticate — must not be null or empty
     * @return     {@value #HMAC_SHA256_SIZE_BYTES}-byte HMAC output
     * @throws IllegalArgumentException if key or data is null/empty
     * @throws CryptoException          if the native HMAC operation fails
     */
    public native byte[] computeHmacSha256(byte[] key, byte[] data);

    /**
     * Derives a cryptographic key from a password using PBKDF2-HMAC-SHA256 (RFC 2898).
     *
     * @param password        password bytes — must not be null or empty
     * @param salt            cryptographic salt — must not be null (recommend ≥ 16 bytes)
     * @param iterations      number of PBKDF2 iterations — must be &gt; 0
     *                        (recommend ≥ 100 000 for interactive, ≥ 1 000 000 for offline)
     * @param outputKeyLength desired key length in bytes — must be in [1, 1024]
     * @return                derived key bytes of length {@code outputKeyLength}
     * @throws IllegalArgumentException if any parameter is out of range
     * @throws CryptoException          if the native PBKDF2 operation fails
     */
    public native byte[] deriveKeyPBKDF2(byte[] password, byte[] salt,
                                          int iterations, int outputKeyLength);

    /**
     * Generates {@code count} cryptographically secure random bytes using
     * OpenSSL {@code RAND_bytes}.
     *
     * @param count number of random bytes — must be &gt; 0
     * @return      random byte array of length {@code count}
     * @throws IllegalArgumentException if {@code count} &lt;= 0
     * @throws CryptoException          if the RNG fails
     */
    public native byte[] generateSecureRandom(int count);

    /**
     * Returns the OpenSSL version string linked into the native library,
     * e.g. {@code "OpenSSL 3.1.4 24 Oct 2023"}.
     */
    public native String openSSLVersion();

    // ── Java-side validation helpers (keep JNI layer thin) ───────────

    /** Validates an AES-256 key: non-null, exactly {@value #AES_KEY_SIZE_BYTES} bytes. */
    static void checkAesKey(byte[] key) {
        Objects.requireNonNull(key, "AES key must not be null");
        if (key.length != AES_KEY_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "AES-256 key must be " + AES_KEY_SIZE_BYTES
                    + " bytes, got: " + key.length);
        }
    }

    /** Validates that a byte array is non-null and non-empty. */
    static void checkNonEmpty(byte[] data, String paramName) {
        Objects.requireNonNull(data, paramName + " must not be null");
        if (data.length == 0) {
            throw new IllegalArgumentException(paramName + " must not be empty");
        }
    }

    /** Validates that a cipher blob has the minimum required size. */
    static void checkCipherBlob(byte[] blob) {
        Objects.requireNonNull(blob, "cipherBlob must not be null");
        if (blob.length < MIN_CIPHER_BLOB_BYTES) {
            throw new IllegalArgumentException(
                    "cipherBlob too short: minimum is " + MIN_CIPHER_BLOB_BYTES
                    + " bytes (IV + 1B ciphertext + GCM tag), got: " + blob.length);
        }
    }

    /** Validates a hash algorithm selector. */
    static void checkHashAlgorithm(int algorithm) {
        if (algorithm != HASH_SHA256 && algorithm != HASH_SHA512) {
            throw new IllegalArgumentException(
                    "hashAlgorithm must be HASH_SHA256 (0) or HASH_SHA512 (1), got: "
                    + algorithm);
        }
    }

    /** Validates PBKDF2 parameters. */
    static void checkPBKDF2Params(byte[] password, byte[] salt,
                                    int iterations, int outputKeyLength) {
        checkNonEmpty(password, "password");
        Objects.requireNonNull(salt, "salt must not be null");
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be > 0, got: " + iterations);
        }
        if (outputKeyLength <= 0 || outputKeyLength > 1024) {
            throw new IllegalArgumentException(
                    "outputKeyLength must be in [1, 1024], got: " + outputKeyLength);
        }
    }
}