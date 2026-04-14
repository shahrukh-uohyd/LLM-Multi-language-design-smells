package com.app.crypto;

import java.util.Arrays;
import java.util.Objects;

/**
 * JNI entrypoint for native AES-256-GCM encryption and decryption.
 *
 * <h2>Algorithm</h2>
 * <ul>
 *   <li>Cipher: AES-256 in GCM mode (authenticated encryption)</li>
 *   <li>Key size: 256 bits (32 bytes)</li>
 *   <li>IV/nonce size: 96 bits (12 bytes) — GCM standard recommendation</li>
 *   <li>Authentication tag: 128 bits (16 bytes)</li>
 *   <li>Key derivation: HKDF-SHA-256 (RFC 5869)</li>
 * </ul>
 *
 * <h2>Wire format produced by {@link #encrypt}</h2>
 * <pre>
 *  ┌────────────┬─────────────────────────┬──────────────┐
 *  │  IV 12 B   │  Ciphertext (var len)   │  GCM Tag 16B │
 *  └────────────┴─────────────────────────┴──────────────┘
 * </pre>
 *
 * <p>Callers should prefer the higher-level {@link SecureChannel} API;
 * this class is intentionally package-accessible for unit-testability.</p>
 */
public class CryptoNative {

    // ── algorithm constants ───────────────────────────────────────────

    /** Required AES-256 key length in bytes. */
    public static final int KEY_LENGTH_BYTES  = 32;

    /** GCM nonce/IV length in bytes (NIST SP 800-38D recommended). */
    public static final int IV_LENGTH_BYTES   = 12;

    /** GCM authentication tag length in bytes. */
    public static final int TAG_LENGTH_BYTES  = 16;

    /** Minimum encrypted blob size: IV + at least 1 byte ciphertext + tag. */
    public static final int MIN_CIPHERTEXT_BYTES = IV_LENGTH_BYTES + 1 + TAG_LENGTH_BYTES;

    // ── library loading ───────────────────────────────────────────────

    static {
        try {
            System.loadLibrary("crypto_native");
        } catch (UnsatisfiedLinkError e) {
            throw new ExceptionInInitializerError(
                "Failed to load native crypto library 'crypto_native': " + e.getMessage());
        }
    }

    // ── native declarations ───────────────────────────────────────────

    /**
     * Encrypts {@code plaintext} with AES-256-GCM.
     *
     * <p>A cryptographically random 12-byte IV is generated internally.
     * The returned blob has the layout: {@code IV || ciphertext || GCM-tag}.</p>
     *
     * @param key            AES key — must be exactly {@value #KEY_LENGTH_BYTES} bytes
     * @param plaintext      data to encrypt — must not be null or empty
     * @param aad            additional authenticated data (may be null or empty;
     *                       authenticated but not encrypted)
     * @return               {@code IV || ciphertext || GCM-tag} as a single byte array
     * @throws IllegalArgumentException if key length is wrong or plaintext is null/empty
     * @throws CryptoException          if the native OpenSSL operation fails
     */
    public native byte[] encrypt(byte[] key, byte[] plaintext, byte[] aad);

    /**
     * Decrypts and authenticates a blob produced by {@link #encrypt}.
     *
     * <p>Expected layout: {@code IV (12 B) || ciphertext || GCM-tag (16 B)}.</p>
     *
     * @param key            AES key — must be exactly {@value #KEY_LENGTH_BYTES} bytes
     * @param cipherBlob     {@code IV || ciphertext || GCM-tag}
     * @param aad            additional authenticated data used during encryption
     *                       (must match exactly; may be null or empty)
     * @return               decrypted plaintext bytes
     * @throws IllegalArgumentException if arguments are null or sizes are invalid
     * @throws CryptoException          if authentication fails (tag mismatch) or
     *                                  the native operation otherwise fails
     */
    public native byte[] decrypt(byte[] key, byte[] cipherBlob, byte[] aad);

    /**
     * Derives a cryptographic key from a master secret using HKDF-SHA-256 (RFC 5869).
     *
     * @param inputKeyMaterial  source key material — must not be null or empty
     * @param salt              optional salt (may be null; a zero-filled salt is used if absent)
     * @param info              optional context / application-specific info (may be null)
     * @param outputKeyLength   desired key length in bytes — must be in [1, 255 * 32]
     * @return                  derived key bytes of length {@code outputKeyLength}
     * @throws IllegalArgumentException if parameters are out of range
     * @throws CryptoException          if the HKDF operation fails
     */
    public native byte[] deriveKey(byte[] inputKeyMaterial, byte[] salt,
                                    byte[] info, int outputKeyLength);

    /**
     * Generates {@code count} cryptographically secure random bytes using
     * {@code RAND_bytes} from OpenSSL.
     *
     * @param count number of random bytes requested — must be &gt; 0
     * @return      random byte array of length {@code count}
     * @throws IllegalArgumentException if {@code count} &lt;= 0
     * @throws CryptoException          if the RNG fails
     */
    public native byte[] generateSecureRandom(int count);

    /**
     * Returns the OpenSSL version string linked into the native library.
     *
     * @return version string, e.g. {@code "OpenSSL 3.1.4 24 Oct 2023"}
     */
    public native String openSSLVersion();

    // ── Java-side validation helpers (keep JNI layer thin) ───────────

    /**
     * Validates that {@code key} is non-null and exactly {@value #KEY_LENGTH_BYTES} bytes.
     */
    static void checkKey(byte[] key) {
        Objects.requireNonNull(key, "key must not be null");
        if (key.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                "AES-256 key must be " + KEY_LENGTH_BYTES + " bytes, got: " + key.length);
        }
    }

    /**
     * Validates that a plaintext byte array is non-null and non-empty.
     */
    static void checkPlaintext(byte[] data) {
        Objects.requireNonNull(data, "plaintext must not be null");
        if (data.length == 0) {
            throw new IllegalArgumentException("plaintext must not be empty");
        }
    }

    /**
     * Validates that a cipher blob is large enough to contain IV + data + tag.
     */
    static void checkCipherBlob(byte[] blob) {
        Objects.requireNonNull(blob, "cipherBlob must not be null");
        if (blob.length < MIN_CIPHERTEXT_BYTES) {
            throw new IllegalArgumentException(
                "cipherBlob too short: minimum is " + MIN_CIPHERTEXT_BYTES
                    + " bytes (IV + 1 byte ciphertext + GCM tag), got: " + blob.length);
        }
    }
}