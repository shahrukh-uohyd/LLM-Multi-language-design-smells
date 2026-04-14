import java.util.Arrays;

/**
 * DataPrivacyModule
 *
 * Provides hardware-backed key generation and authenticated health-data
 * encryption using the native cryptographic engine via {@link WearableBridge}.
 *
 * <p>Key material never crosses the JNI boundary; only opaque key handles
 * are managed at the Java layer, ensuring that sensitive key bytes remain
 * within the device's Trusted Execution Environment (TEE) or Secure Element.
 */
public class DataPrivacyModule {

    // ----------------------------------------------------------------
    // Supported algorithm identifiers (passed verbatim to native layer)
    // ----------------------------------------------------------------
    /** AES-256 in Galois/Counter Mode — recommended default. */
    public static final String ALGO_AES_256_GCM       = "AES-256-GCM";
    /** ChaCha20-Poly1305 — preferred on CPUs without AES hardware acceleration. */
    public static final String ALGO_CHACHA20_POLY1305 = "CHACHA20-POLY1305";

    /** Byte length of the IV/nonce prepended by the native layer (AES-GCM). */
    public static final int IV_LENGTH_BYTES  = 12;
    /** Byte length of the GCM/Poly1305 authentication tag. */
    public static final int TAG_LENGTH_BYTES = 16;

    private final WearableBridge bridge;

    /**
     * @param bridge  Shared {@link WearableBridge} instance.
     */
    public DataPrivacyModule(WearableBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("WearableBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Generates a new cryptographic key and stores it in the native
     * hardware-backed key store under the given alias.
     *
     * @param algorithm  Algorithm identifier (e.g. {@link #ALGO_AES_256_GCM}).
     * @param keyAlias   Unique alias for the key. Re-using an alias rotates
     *                   the key atomically.
     * @return           Opaque key handle for use with
     *                   {@link #encryptHealthData}, or {@code null} on failure.
     */
    public String generateSecureKey(String algorithm, String keyAlias) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException(
                "Algorithm identifier must not be null or blank.");
        }
        if (keyAlias == null || keyAlias.isBlank()) {
            throw new IllegalArgumentException(
                "Key alias must not be null or blank.");
        }

        System.out.printf("[Privacy] Generating %s key with alias '%s'...%n",
                algorithm, keyAlias);

        // ── Native call ──────────────────────────────────────────────
        String keyHandle = bridge.generateSecureKey(algorithm, keyAlias);
        // ────────────────────────────────────────────────────────────

        if (keyHandle == null) {
            System.err.printf("[Privacy] ✗ ERROR: Key generation failed for "
                    + "alias '%s'. TEE may be unavailable.%n", keyAlias);
        } else {
            // Never log the handle value itself — treat it as a secret reference.
            System.out.printf("[Privacy] ✓ Key generated for alias '%s'. "
                    + "Handle length: %d chars.%n", keyAlias, keyHandle.length());
        }
        return keyHandle;
    }

    /**
     * Encrypts a health-data record using the key identified by
     * {@code keyHandle}.
     *
     * <p>The returned ciphertext format is:
     * <pre>
     *   [ IV/nonce (12 B) | ciphertext (N B) | auth tag (16 B) ]
     * </pre>
     *
     * @param plaintext  Raw health-record bytes (must not be empty).
     * @param keyHandle  Opaque handle from {@link #generateSecureKey}.
     * @param aadBytes   Additional Authenticated Data; pass an empty
     *                   {@code byte[]} to omit.
     * @return           Authenticated ciphertext, or {@code null} on failure.
     */
    public byte[] encryptHealthData(byte[] plaintext,
                                    String keyHandle,
                                    byte[] aadBytes) {
        if (plaintext == null || plaintext.length == 0) {
            throw new IllegalArgumentException(
                "Plaintext must not be null or empty.");
        }
        if (keyHandle == null || keyHandle.isBlank()) {
            throw new IllegalArgumentException(
                "Key handle must not be null or blank.");
        }
        // Treat null AAD as empty to simplify native-layer contracts.
        byte[] safeAad = (aadBytes != null) ? aadBytes : new byte[0];

        System.out.printf("[Privacy] Encrypting %d bytes of health data "
                + "(AAD length: %d bytes)...%n",
                plaintext.length, safeAad.length);

        // ── Native call ──────────────────────────────────────────────
        byte[] ciphertext = bridge.encryptHealthData(plaintext, keyHandle, safeAad);
        // ────────────────────────────────────────────────────────────

        if (ciphertext == null) {
            System.err.println("[Privacy] ✗ ERROR: encryptHealthData() returned null.");
        } else {
            int expectedMinLen = IV_LENGTH_BYTES + TAG_LENGTH_BYTES + 1;
            if (ciphertext.length < expectedMinLen) {
                System.err.printf("[Privacy] ⚠  Ciphertext suspiciously short "
                        + "(%d bytes). Discarding.%n", ciphertext.length);
                return null;
            }
            System.out.printf("[Privacy] ✓ Encrypted. Output: %d bytes "
                    + "(IV=%d B + ciphertext=%d B + tag=%d B).%n",
                    ciphertext.length, IV_LENGTH_BYTES,
                    ciphertext.length - IV_LENGTH_BYTES - TAG_LENGTH_BYTES,
                    TAG_LENGTH_BYTES);
        }
        return ciphertext;
    }

    /**
     * Convenience: generate a new key then immediately encrypt a health record.
     *
     * <p>Useful for a "generate-and-store" pattern where each health session
     * uses a fresh key.
     *
     * @param algorithm    Encryption algorithm identifier.
     * @param keyAlias     Alias under which the new key is stored.
     * @param plaintext    Raw health record to encrypt.
     * @param aadBytes     Additional Authenticated Data (may be empty).
     * @return             {@link EncryptionBundle} holding both the key handle
     *                     and the resulting ciphertext, or {@code null} if
     *                     either operation failed.
     */
    public EncryptionBundle generateKeyAndEncrypt(String algorithm,
                                                  String keyAlias,
                                                  byte[] plaintext,
                                                  byte[] aadBytes) {
        System.out.println("[Privacy] === Generate Key & Encrypt Pipeline ===");

        String keyHandle = generateSecureKey(algorithm, keyAlias);
        if (keyHandle == null) {
            System.err.println("[Privacy] Pipeline aborted — key generation failed.");
            return null;
        }

        byte[] ciphertext = encryptHealthData(plaintext, keyHandle, aadBytes);
        if (ciphertext == null) {
            System.err.println("[Privacy] Pipeline aborted — encryption failed.");
            return null;
        }

        EncryptionBundle bundle = new EncryptionBundle(keyHandle, ciphertext);
        System.out.printf("[Privacy] ✓ Bundle ready: %s%n", bundle);
        System.out.println("[Privacy] === Pipeline Complete ===");
        return bundle;
    }

    // ----------------------------------------------------------------
    // Nested result type
    // ----------------------------------------------------------------

    /**
     * Pairs an opaque key handle with its resulting ciphertext for
     * safe handoff to storage or transmission layers.
     */
    public static class EncryptionBundle {
        /** Opaque handle referencing the key in the native store. */
        public final String keyHandle;
        /** Authenticated ciphertext (IV + ciphertext + tag). */
        public final byte[] ciphertext;

        public EncryptionBundle(String keyHandle, byte[] ciphertext) {
            this.keyHandle  = keyHandle;
            this.ciphertext = ciphertext;
        }

        @Override
        public String toString() {
            return String.format(
                "EncryptionBundle{keyHandleLen=%d, ciphertextLen=%d bytes}",
                keyHandle.length(), ciphertext.length);
        }
    }
}