package com.app.security;

import com.app.crypto.CryptoException;
import com.app.crypto.CryptoProcessor;
import com.app.filesystem.FileSystemManager;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles security-related logic for the application.
 *
 * <p>This class is the primary consumer of {@link CryptoProcessor}. It provides
 * a unified security API that integrates cryptographic processing with the
 * existing {@link FileSystemManager} for secure file operations:</p>
 *
 * <ul>
 *   <li>Encrypting and decrypting data payloads (AES-256-GCM)</li>
 *   <li>Hashing data for integrity checks (SHA-256 / SHA-512)</li>
 *   <li>Generating and verifying HMACs for message authentication</li>
 *   <li>Deriving keys from passwords (PBKDF2-HMAC-SHA256)</li>
 *   <li>Encrypting files to disk and decrypting them back</li>
 *   <li>Secure token and session key generation</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 *   FileSystemManager fs       = new FileSystemManager();
 *   SecurityManager   security = new SecurityManager(fs);
 *
 *   // Derive an AES key from a user password
 *   byte[] salt = security.generateSalt();
 *   byte[] key  = security.deriveKey("userPassword", salt, 310_000);
 *
 *   // Encrypt a payload
 *   byte[] payload    = "sensitive data".getBytes(UTF_8);
 *   byte[] cipherBlob = security.encrypt(key, payload);
 *
 *   // Decrypt later
 *   byte[] recovered  = security.decrypt(key, cipherBlob);
 *
 *   // Securely encrypt a file
 *   security.encryptFile("/data/report.pdf", "/data/report.pdf.enc", key);
 *   security.decryptFile("/data/report.pdf.enc", "/data/report_out.pdf", key);
 * }</pre>
 */
public class SecurityManager {

    private static final Logger LOG = Logger.getLogger(SecurityManager.class.getName());

    /** Recommended PBKDF2 iteration count for interactive authentication (NIST SP 800-132). */
    public static final int PBKDF2_ITERATIONS_INTERACTIVE = 310_000;

    /** Recommended PBKDF2 iteration count for offline / at-rest key derivation. */
    public static final int PBKDF2_ITERATIONS_OFFLINE = 1_000_000;

    /** Standard salt length in bytes for PBKDF2 (NIST recommended minimum: 16). */
    public static final int SALT_SIZE_BYTES = 32;

    /** Length of a generated secure token string (hex-encoded 32 random bytes → 64 chars). */
    public static final int TOKEN_RANDOM_BYTES = 32;

    // ── AAD label used when encrypting files ─────────────────────────
    private static final byte[] FILE_ENCRYPTION_AAD =
            "SecurityManager-FileEncryption-v1".getBytes(StandardCharsets.UTF_8);

    // ── fields ────────────────────────────────────────────────────────

    private final FileSystemManager fsManager;
    private final CryptoProcessor   crypto;

    // ── constructors ─────────────────────────────────────────────────

    /**
     * Creates a {@code SecurityManager} using the given filesystem manager.
     *
     * @param fsManager filesystem interface for secure file operations
     */
    public SecurityManager(FileSystemManager fsManager) {
        this(fsManager, new CryptoProcessor());
    }

    /**
     * Full constructor — package-accessible for testing with injected dependencies.
     */
    SecurityManager(FileSystemManager fsManager, CryptoProcessor crypto) {
        this.fsManager = Objects.requireNonNull(fsManager, "fsManager must not be null");
        this.crypto    = Objects.requireNonNull(crypto,    "crypto must not be null");
        LOG.info("SecurityManager initialised — OpenSSL " + crypto.openSSLVersion());
    }

    // ── encryption / decryption ───────────────────────────────────────

    /**
     * Encrypts {@code plaintext} with AES-256-GCM using the supplied key.
     *
     * <p>No AAD is applied. Use {@link #encryptWithContext} to bind the ciphertext
     * to a specific context label.</p>
     *
     * @param key       AES-256 key ({@value CryptoProcessor#AES_KEY_SIZE_BYTES} bytes)
     * @param plaintext data to encrypt
     * @return          {@code IV || Ciphertext || GCM-Tag} blob
     * @throws CryptoException on encryption failure
     */
    public byte[] encrypt(byte[] key, byte[] plaintext) {
        CryptoProcessor.checkAesKey(key);
        CryptoProcessor.checkNonEmpty(plaintext, "plaintext");
        byte[] blob = crypto.encrypt(key, plaintext, null);
        LOG.fine(String.format("encrypt — plaintext=%d B → blob=%d B", plaintext.length, blob.length));
        return blob;
    }

    /**
     * Encrypts {@code plaintext} and binds the ciphertext to {@code contextLabel}
     * via GCM Additional Authenticated Data (AAD).
     *
     * <p>The same {@code contextLabel} must be provided to {@link #decryptWithContext}
     * or decryption will fail with a GCM tag mismatch.</p>
     *
     * @param key          AES-256 key
     * @param plaintext    data to encrypt
     * @param contextLabel application-specific label (e.g. {@code "user-session-v1"})
     * @return             {@code IV || Ciphertext || GCM-Tag} blob
     */
    public byte[] encryptWithContext(byte[] key, byte[] plaintext, String contextLabel) {
        CryptoProcessor.checkAesKey(key);
        CryptoProcessor.checkNonEmpty(plaintext, "plaintext");
        Objects.requireNonNull(contextLabel, "contextLabel must not be null");
        byte[] aad  = contextLabel.getBytes(StandardCharsets.UTF_8);
        byte[] blob = crypto.encrypt(key, plaintext, aad);
        LOG.fine(String.format("encryptWithContext — context=%s plaintext=%d B → blob=%d B",
                contextLabel, plaintext.length, blob.length));
        return blob;
    }

    /**
     * Decrypts a blob produced by {@link #encrypt}.
     *
     * @param key        AES-256 key
     * @param cipherBlob {@code IV || Ciphertext || GCM-Tag}
     * @return           decrypted plaintext
     * @throws CryptoException if GCM authentication fails or decryption fails
     */
    public byte[] decrypt(byte[] key, byte[] cipherBlob) {
        CryptoProcessor.checkAesKey(key);
        CryptoProcessor.checkCipherBlob(cipherBlob);
        byte[] plain = crypto.decrypt(key, cipherBlob, null);
        LOG.fine(String.format("decrypt — blob=%d B → plaintext=%d B", cipherBlob.length, plain.length));
        return plain;
    }

    /**
     * Decrypts a blob produced by {@link #encryptWithContext}.
     *
     * @param key          AES-256 key
     * @param cipherBlob   {@code IV || Ciphertext || GCM-Tag}
     * @param contextLabel must exactly match the label used during encryption
     * @return             decrypted plaintext
     * @throws CryptoException if GCM authentication fails (wrong key, wrong context, or tampered)
     */
    public byte[] decryptWithContext(byte[] key, byte[] cipherBlob, String contextLabel) {
        CryptoProcessor.checkAesKey(key);
        CryptoProcessor.checkCipherBlob(cipherBlob);
        Objects.requireNonNull(contextLabel, "contextLabel must not be null");
        byte[] aad = contextLabel.getBytes(StandardCharsets.UTF_8);
        return crypto.decrypt(key, cipherBlob, aad);
    }

    // ── hashing ───────────────────────────────────────────────────────

    /**
     * Computes the SHA-256 digest of {@code data}.
     *
     * @param data input bytes
     * @return     32-byte SHA-256 digest
     */
    public byte[] sha256(byte[] data) {
        CryptoProcessor.checkNonEmpty(data, "data");
        return crypto.computeHash(data, CryptoProcessor.HASH_SHA256);
    }

    /**
     * Computes the SHA-512 digest of {@code data}.
     *
     * @param data input bytes
     * @return     64-byte SHA-512 digest
     */
    public byte[] sha512(byte[] data) {
        CryptoProcessor.checkNonEmpty(data, "data");
        return crypto.computeHash(data, CryptoProcessor.HASH_SHA512);
    }

    /**
     * Returns the hex-encoded SHA-256 digest of {@code data} (64 hex characters).
     *
     * @param data input bytes
     * @return     lower-case hex string of the SHA-256 digest
     */
    public String sha256Hex(byte[] data) {
        return HexFormat.of().formatHex(sha256(data));
    }

    /**
     * Verifies that {@code data} matches the provided SHA-256 {@code expectedDigest}
     * using a constant-time comparison to prevent timing attacks.
     *
     * @param data           data to verify
     * @param expectedDigest expected 32-byte SHA-256 digest
     * @return               {@code true} if digests match
     */
    public boolean verifyHash(byte[] data, byte[] expectedDigest) {
        CryptoProcessor.checkNonEmpty(data,           "data");
        CryptoProcessor.checkNonEmpty(expectedDigest, "expectedDigest");
        byte[] actual = sha256(data);
        return constantTimeEquals(actual, expectedDigest);
    }

    // ── HMAC ──────────────────────────────────────────────────────────

    /**
     * Computes HMAC-SHA256 of {@code data} with {@code key}.
     *
     * @param key  HMAC key
     * @param data data to authenticate
     * @return     32-byte HMAC output
     */
    public byte[] hmacSha256(byte[] key, byte[] data) {
        CryptoProcessor.checkNonEmpty(key,  "key");
        CryptoProcessor.checkNonEmpty(data, "data");
        return crypto.computeHmacSha256(key, data);
    }

    /**
     * Verifies an HMAC-SHA256 tag using a constant-time comparison.
     *
     * @param key          HMAC key
     * @param data         data that was authenticated
     * @param expectedHmac expected 32-byte HMAC
     * @return             {@code true} if the tag matches
     */
    public boolean verifyHmac(byte[] key, byte[] data, byte[] expectedHmac) {
        CryptoProcessor.checkNonEmpty(key,          "key");
        CryptoProcessor.checkNonEmpty(data,         "data");
        CryptoProcessor.checkNonEmpty(expectedHmac, "expectedHmac");
        byte[] computed = hmacSha256(key, data);
        return constantTimeEquals(computed, expectedHmac);
    }

    // ── key derivation ────────────────────────────────────────────────

    /**
     * Derives a {@value CryptoProcessor#AES_KEY_SIZE_BYTES}-byte AES-256 key
     * from a password using PBKDF2-HMAC-SHA256.
     *
     * @param password   plaintext password
     * @param salt       cryptographic salt (use {@link #generateSalt()} to create one)
     * @param iterations PBKDF2 iteration count (see {@link #PBKDF2_ITERATIONS_INTERACTIVE})
     * @return           derived AES-256 key
     */
    public byte[] deriveKey(String password, byte[] salt, int iterations) {
        Objects.requireNonNull(password, "password must not be null");
        if (password.isEmpty()) throw new IllegalArgumentException("password must not be empty");
        Objects.requireNonNull(salt, "salt must not be null");
        return crypto.deriveKeyPBKDF2(
                password.getBytes(StandardCharsets.UTF_8),
                salt, iterations, CryptoProcessor.AES_KEY_SIZE_BYTES);
    }

    /**
     * Derives a key of {@code outputKeyLength} bytes from raw {@code keyMaterial}
     * using PBKDF2-HMAC-SHA256 with a single iteration (key stretching / formatting).
     *
     * @param keyMaterial   source key bytes
     * @param salt          domain-separation salt
     * @param outputKeyLength desired key length in bytes
     * @return              derived key bytes
     */
    public byte[] deriveKeyFromMaterial(byte[] keyMaterial, byte[] salt, int outputKeyLength) {
        CryptoProcessor.checkNonEmpty(keyMaterial, "keyMaterial");
        Objects.requireNonNull(salt, "salt must not be null");
        return crypto.deriveKeyPBKDF2(keyMaterial, salt, 1, outputKeyLength);
    }

    // ── random generation ─────────────────────────────────────────────

    /**
     * Generates a {@value #SALT_SIZE_BYTES}-byte cryptographic salt for PBKDF2.
     *
     * @return random salt bytes
     */
    public byte[] generateSalt() {
        return crypto.generateSecureRandom(SALT_SIZE_BYTES);
    }

    /**
     * Generates a URL-safe, hex-encoded secure token (64 hex characters).
     * Suitable for session tokens, CSRF tokens, or API keys.
     *
     * @return 64-character lowercase hex token
     */
    public String generateSecureToken() {
        byte[] bytes = crypto.generateSecureRandom(TOKEN_RANDOM_BYTES);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Generates {@code count} cryptographically secure random bytes.
     *
     * @param count number of bytes
     * @return      random bytes
     */
    public byte[] generateRandom(int count) {
        if (count <= 0) throw new IllegalArgumentException("count must be > 0, got: " + count);
        return crypto.generateSecureRandom(count);
    }

    // ── secure file operations (integrates FileSystemManager) ────────

    /**
     * Reads a file, encrypts its contents with AES-256-GCM, and writes
     * the cipher blob to {@code destPath}.
     *
     * @param srcPath  path to the plaintext file to read
     * @param destPath path to write the encrypted output
     * @param key      AES-256 key ({@value CryptoProcessor#AES_KEY_SIZE_BYTES} bytes)
     * @throws CryptoException       on encryption failure
     * @throws IllegalStateException if the source file cannot be read or dest cannot be written
     */
    public void encryptFile(String srcPath, String destPath, byte[] key) {
        CryptoProcessor.checkAesKey(key);
        Objects.requireNonNull(srcPath,  "srcPath must not be null");
        Objects.requireNonNull(destPath, "destPath must not be null");

        byte[] plaintext = readFileBytes(srcPath);
        byte[] blob      = crypto.encrypt(key, plaintext, FILE_ENCRYPTION_AAD);
        writeFileBytes(destPath, blob);

        LOG.info(String.format("encryptFile — src=%s (%d B) → dest=%s (%d B)",
                srcPath, plaintext.length, destPath, blob.length));

        // Zero plaintext buffer before GC can observe it
        Arrays.fill(plaintext, (byte) 0);
    }

    /**
     * Reads an encrypted blob file, decrypts it with AES-256-GCM, and writes
     * the plaintext to {@code destPath}.
     *
     * @param srcPath  path to the encrypted blob file
     * @param destPath path to write the decrypted plaintext
     * @param key      AES-256 key
     * @throws CryptoException       if GCM authentication fails (tampered or wrong key)
     * @throws IllegalStateException if a file cannot be read or written
     */
    public void decryptFile(String srcPath, String destPath, byte[] key) {
        CryptoProcessor.checkAesKey(key);
        Objects.requireNonNull(srcPath,  "srcPath must not be null");
        Objects.requireNonNull(destPath, "destPath must not be null");

        byte[] blob      = readFileBytes(srcPath);
        CryptoProcessor.checkCipherBlob(blob);
        byte[] plaintext = crypto.decrypt(key, blob, FILE_ENCRYPTION_AAD);
        writeFileBytes(destPath, plaintext);

        LOG.info(String.format("decryptFile — src=%s (%d B) → dest=%s (%d B)",
                srcPath, blob.length, destPath, plaintext.length));

        Arrays.fill(plaintext, (byte) 0);
    }

    /**
     * Computes the SHA-256 digest of a file's contents and returns it as a hex string.
     *
     * @param filePath path to the file
     * @return         64-character lowercase hex SHA-256 digest
     */
    public String hashFile(String filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        byte[] contents = readFileBytes(filePath);
        String hex      = sha256Hex(contents);
        LOG.fine("hashFile — path=" + filePath + " sha256=" + hex);
        return hex;
    }

    // ── accessors ─────────────────────────────────────────────────────

    /** Returns the OpenSSL version string linked into the native library. */
    public String getNativeLibraryVersion() {
        return crypto.openSSLVersion();
    }

    // ── private helpers ───────────────────────────────────────────────

    /**
     * Reads an entire file into a byte array via the native {@link FileSystemManager}.
     */
    private byte[] readFileBytes(String path) {
        long size = fsManager.getFileSize(path);
        if (size < 0) {
            throw new IllegalStateException("Cannot determine size of file: " + path);
        }
        if (size == 0) {
            throw new IllegalStateException("File is empty: " + path);
        }
        if (size > Integer.MAX_VALUE) {
            throw new IllegalStateException("File too large to read into memory: " + path);
        }

        byte[] buffer = new byte[(int) size];
        int fd        = fsManager.openFile(path, 0 /* O_RDONLY */);
        if (fd < 0) {
            throw new IllegalStateException("Failed to open file for reading: " + path);
        }
        try {
            int totalRead = 0;
            while (totalRead < buffer.length) {
                int n = fsManager.readFile(fd, buffer, totalRead, buffer.length - totalRead);
                if (n <= 0) break;
                totalRead += n;
            }
            if (totalRead != buffer.length) {
                throw new IllegalStateException(
                        "Short read on file " + path + ": expected " + buffer.length
                        + " bytes, got " + totalRead);
            }
        } finally {
            fsManager.closeFile(fd);
        }
        return buffer;
    }

    /**
     * Writes a byte array to a file via the native {@link FileSystemManager}.
     */
    private void writeFileBytes(String path, byte[] data) {
        int fd = fsManager.openFile(path, 1 /* O_WRONLY | O_CREAT | O_TRUNC */);
        if (fd < 0) {
            throw new IllegalStateException("Failed to open file for writing: " + path);
        }
        try {
            int totalWritten = 0;
            while (totalWritten < data.length) {
                int n = fsManager.writeFile(fd, data, totalWritten, data.length - totalWritten);
                if (n <= 0) {
                    throw new IllegalStateException(
                            "Write failed on file " + path + " at offset " + totalWritten);
                }
                totalWritten += n;
            }
        } finally {
            fsManager.closeFile(fd);
        }
    }

    /**
     * Constant-time byte array comparison — prevents timing-based side-channel attacks.
     * Returns {@code true} only if both arrays are equal in length and content.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        if (a.length != b.length)  return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= (a[i] ^ b[i]);
        }
        return diff == 0;
    }
}