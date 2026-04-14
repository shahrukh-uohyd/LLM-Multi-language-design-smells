package com.app.comms;

import com.app.crypto.CryptoException;
import com.app.crypto.CryptoNative;
import com.app.diagnostics.SystemLogger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages encrypted communication channels for the application.
 *
 * <p>This class is the primary consumer of {@link CryptoNative}. It handles:
 * <ul>
 *   <li>Session-key derivation from a shared master secret</li>
 *   <li>Encrypting outbound payloads</li>
 *   <li>Decrypting and authenticating inbound payloads</li>
 *   <li>Secure key rotation</li>
 *   <li>Diagnostic logging through the existing {@link SystemLogger}</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 *   byte[] masterSecret = ...; // exchanged via TLS handshake / KMS
 *   SecureChannel channel = new SecureChannel("server-auth", masterSecret);
 *
 *   // Sender side
 *   byte[] payload    = "sensitive payload".getBytes(UTF_8);
 *   byte[] cipherBlob = channel.send(payload);
 *
 *   // Receiver side (same channel instance or reconstructed with same key)
 *   byte[] plaintext  = channel.receive(cipherBlob);
 * }</pre>
 */
public class SecureChannel implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(SecureChannel.class.getName());

    /** Logging priority constant matching Android/syslog INFO level. */
    private static final int LOG_PRIORITY_INFO  = 4;
    private static final int LOG_PRIORITY_ERROR = 6;

    // ── HKDF context info labels ──────────────────────────────────────
    private static final byte[] HKDF_INFO_ENCRYPT =
        "SecureChannel-v1-encrypt".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_INFO_DECRYPT =
        "SecureChannel-v1-decrypt".getBytes(StandardCharsets.UTF_8);

    // ── fields ────────────────────────────────────────────────────────

    /** Human-readable label for diagnostics (e.g. "server-auth", "peer-sync"). */
    private final String              channelId;

    private final CryptoNative        cryptoNative;
    private final SystemLogger        sysLogger;

    /** Derived session encryption key (never leaves this object). */
    private volatile byte[]           sessionEncryptKey;

    /** Derived session decryption key (may equal encryptKey for symmetric channels). */
    private volatile byte[]           sessionDecryptKey;

    private volatile boolean          closed = false;

    // ── constructors ─────────────────────────────────────────────────

    /**
     * Creates a {@code SecureChannel} and derives session keys from the given
     * master secret using HKDF-SHA-256.
     *
     * @param channelId    identifier used in diagnostic logs
     * @param masterSecret shared secret from which session keys are derived;
     *                     must not be null or empty; caller should zero it after
     *                     this constructor returns
     * @throws CryptoException if key derivation fails
     */
    public SecureChannel(String channelId, byte[] masterSecret) {
        this(channelId, masterSecret, null, new CryptoNative(), new SystemLogger());
    }

    /**
     * Creates a {@code SecureChannel} with an explicit HKDF salt.
     *
     * @param channelId    identifier used in diagnostic logs
     * @param masterSecret shared secret
     * @param hkdfSalt     optional HKDF salt (may be null)
     */
    public SecureChannel(String channelId, byte[] masterSecret, byte[] hkdfSalt) {
        this(channelId, masterSecret, hkdfSalt, new CryptoNative(), new SystemLogger());
    }

    /**
     * Full constructor — package-accessible for testing with mock dependencies.
     */
    SecureChannel(String channelId, byte[] masterSecret, byte[] hkdfSalt,
                  CryptoNative cryptoNative, SystemLogger sysLogger) {
        this.channelId    = Objects.requireNonNull(channelId, "channelId must not be null");
        this.cryptoNative = Objects.requireNonNull(cryptoNative);
        this.sysLogger    = Objects.requireNonNull(sysLogger);

        CryptoNative.checkPlaintext(masterSecret); // reuse non-null / non-empty check

        // Derive two independent session keys (encrypt + decrypt)
        // so that a single compromised key does not expose both directions.
        this.sessionEncryptKey = cryptoNative.deriveKey(
            masterSecret, hkdfSalt, HKDF_INFO_ENCRYPT, CryptoNative.KEY_LENGTH_BYTES);

        this.sessionDecryptKey = cryptoNative.deriveKey(
            masterSecret, hkdfSalt, HKDF_INFO_DECRYPT, CryptoNative.KEY_LENGTH_BYTES);

        log(LOG_PRIORITY_INFO, "SecureChannel[" + channelId + "] initialised — "
            + "OpenSSL " + cryptoNative.openSSLVersion());
    }

    // ── public API ───────────────────────────────────────────────────

    /**
     * Encrypts {@code plaintext} using AES-256-GCM and returns the wire-format blob
     * ({@code IV || ciphertext || GCM-tag}).
     *
     * <p>The channel ID is included as Additional Authenticated Data (AAD) so that
     * the ciphertext is bound to this specific channel and cannot be replayed on
     * a different one.</p>
     *
     * @param plaintext data to encrypt; must not be null or empty
     * @return          encrypted blob ready for transmission
     * @throws IllegalStateException if the channel has been closed
     * @throws CryptoException       if encryption fails
     */
    public byte[] send(byte[] plaintext) {
        checkOpen();
        CryptoNative.checkPlaintext(plaintext);

        byte[] blob = cryptoNative.encrypt(sessionEncryptKey, plaintext, aadFor(channelId));

        log(LOG_PRIORITY_INFO, String.format(
            "SecureChannel[%s] send — plaintext=%d encrypted=%d bytes",
            channelId, plaintext.length, blob.length));

        return blob;
    }

    /**
     * Decrypts and authenticates a blob produced by {@link #send}.
     *
     * @param cipherBlob {@code IV || ciphertext || GCM-tag}; must not be null
     * @return           decrypted plaintext bytes
     * @throws IllegalStateException    if the channel has been closed
     * @throws IllegalArgumentException if the blob is too short
     * @throws CryptoException          if authentication fails (tampered data,
     *                                  wrong key, wrong AAD) or decryption fails
     */
    public byte[] receive(byte[] cipherBlob) {
        checkOpen();
        CryptoNative.checkCipherBlob(cipherBlob);

        byte[] plaintext = cryptoNative.decrypt(sessionDecryptKey, cipherBlob, aadFor(channelId));

        log(LOG_PRIORITY_INFO, String.format(
            "SecureChannel[%s] receive — ciphertext=%d decrypted=%d bytes",
            channelId, cipherBlob.length, plaintext.length));

        return plaintext;
    }

    /**
     * Rotates the session keys derived from a new master secret.
     *
     * <p>After rotation all subsequent {@link #send}/{@link #receive} calls use
     * the new keys. In-flight messages encrypted with the old key must be
     * decrypted before rotation or handled by the caller.</p>
     *
     * @param newMasterSecret new shared secret
     * @param newHkdfSalt     optional HKDF salt for the new derivation (may be null)
     * @throws CryptoException if key derivation fails
     */
    public synchronized void rotateKeys(byte[] newMasterSecret, byte[] newHkdfSalt) {
        checkOpen();
        CryptoNative.checkPlaintext(newMasterSecret);

        byte[] newEncryptKey = cryptoNative.deriveKey(
            newMasterSecret, newHkdfSalt, HKDF_INFO_ENCRYPT, CryptoNative.KEY_LENGTH_BYTES);
        byte[] newDecryptKey = cryptoNative.deriveKey(
            newMasterSecret, newHkdfSalt, HKDF_INFO_DECRYPT, CryptoNative.KEY_LENGTH_BYTES);

        // Atomically swap — zero old keys before releasing references
        zeroKey(sessionEncryptKey);
        zeroKey(sessionDecryptKey);

        sessionEncryptKey = newEncryptKey;
        sessionDecryptKey = newDecryptKey;

        log(LOG_PRIORITY_INFO, "SecureChannel[" + channelId + "] keys rotated successfully");
    }

    /**
     * Generates {@code count} cryptographically secure random bytes sourced from
     * OpenSSL's {@code RAND_bytes}.
     *
     * @param count number of bytes — must be &gt; 0
     * @return      random bytes
     */
    public byte[] generateRandom(int count) {
        checkOpen();
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
        return cryptoNative.generateSecureRandom(count);
    }

    /**
     * Returns the channel identifier.
     */
    public String getChannelId() { return channelId; }

    /**
     * Returns {@code true} if this channel has not yet been {@link #close}d.
     */
    public boolean isOpen() { return !closed; }

    /**
     * Zeros session keys in memory and marks the channel as closed.
     *
     * <p>Implements {@link AutoCloseable} for use in try-with-resources.</p>
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            zeroKey(sessionEncryptKey);
            zeroKey(sessionDecryptKey);
            sessionEncryptKey = null;
            sessionDecryptKey = null;
            closed = true;
            log(LOG_PRIORITY_INFO, "SecureChannel[" + channelId + "] closed and keys zeroed");
        }
    }

    // ── private helpers ───────────────────────────────────────────────

    /** Returns the AAD bytes for the given channel ID. */
    private static byte[] aadFor(String channelId) {
        return channelId.getBytes(StandardCharsets.UTF_8);
    }

    /** Overwrites key material with zeros to reduce in-memory exposure window. */
    private static void zeroKey(byte[] key) {
        if (key != null) Arrays.fill(key, (byte) 0);
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException(
            "SecureChannel[" + channelId + "] is closed");
    }

    private void log(int priority, String message) {
        try {
            sysLogger.logToSystem(priority, "SecureChannel", message);
        } catch (Exception ignored) {
            // Defensive: never let a logging failure break crypto operations
        }
        LOG.log(priority >= LOG_PRIORITY_ERROR ? Level.SEVERE : Level.INFO, message);
    }
}