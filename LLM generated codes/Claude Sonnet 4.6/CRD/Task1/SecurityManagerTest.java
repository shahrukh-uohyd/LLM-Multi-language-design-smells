package com.app.security;

import com.app.crypto.CryptoException;
import com.app.crypto.CryptoProcessor;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SecurityManager} and {@link CryptoProcessor}.
 * Exercises the full Java → JNI → OpenSSL path.
 *
 * Prerequisites:
 *   1. Build libcrypto_processor.so (cmake --build build)
 *   2. Run with: mvn test -Djava.library.path=lib
 *                or: gradle test (configure nativeLibraryPath in build.gradle)
 */
class SecurityManagerTest {

    /** A fixed 32-byte AES-256 key for deterministic tests. */
    private static final byte[] TEST_KEY = new byte[CryptoProcessor.AES_KEY_SIZE_BYTES];
    static {
        for (int i = 0; i < TEST_KEY.length; i++) TEST_KEY[i] = (byte) i;
    }

    private CryptoProcessor crypto;
    // SecurityManager requires a FileSystemManager — use a lightweight stub here.
    // Full file-operation tests would require the native filesystem_manager lib.

    @BeforeEach
    void setUp() {
        crypto = new CryptoProcessor();
    }

    // ── openSSLVersion ────────────────────────────────────────────────

    @Test
    void openSSLVersion_isNonEmpty() {
        String v = crypto.openSSLVersion();
        assertNotNull(v);
        assertFalse(v.isBlank());
        System.out.println("OpenSSL: " + v);
    }

    // ── AES-256-GCM round-trip ────────────────────────────────────────

    @Test
    void encryptDecrypt_roundTrip_shortMessage() {
        byte[] plain = "Hello, JNI crypto!".getBytes(StandardCharsets.UTF_8);
        byte[] blob  = crypto.encrypt(TEST_KEY, plain, null);
        byte[] recovered = crypto.decrypt(TEST_KEY, blob, null);
        assertArrayEquals(plain, recovered);
    }

    @Test
    void encryptDecrypt_roundTrip_largePayload() {
        byte[] plain = new byte[256 * 1024];
        for (int i = 0; i < plain.length; i++) plain[i] = (byte) i;
        byte[] blob      = crypto.encrypt(TEST_KEY, plain, null);
        byte[] recovered = crypto.decrypt(TEST_KEY, blob,  null);
        assertArrayEquals(plain, recovered);
    }

    @Test
    void encryptDecrypt_withAAD_roundTrip() {
        byte[] plain = "AAD-bound payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad   = "context-label-v1".getBytes(StandardCharsets.UTF_8);
        byte[] blob  = crypto.encrypt(TEST_KEY, plain, aad);
        assertArrayEquals(plain, crypto.decrypt(TEST_KEY, blob, aad));
    }

    @Test
    void encrypt_producesUniqueIVs_forSamePlaintext() {
        byte[] plain = "same input".getBytes(StandardCharsets.UTF_8);
        byte[] blob1 = crypto.encrypt(TEST_KEY, plain, null);
        byte[] blob2 = crypto.encrypt(TEST_KEY, plain, null);
        assertFalse(Arrays.equals(blob1, blob2),
            "Two encryptions of the same plaintext must produce different blobs (random IV)");
    }

    @Test
    void blobSize_isPlaintextPlusOverhead() {
        byte[] plain = "payload".getBytes(StandardCharsets.UTF_8);
        byte[] blob  = crypto.encrypt(TEST_KEY, plain, null);
        int expected = plain.length
                     + CryptoProcessor.GCM_IV_SIZE_BYTES
                     + CryptoProcessor.GCM_TAG_SIZE_BYTES;
        assertEquals(expected, blob.length);
    }

    // ── tamper / authentication failure ──────────────────────────────

    @Test
    void decrypt_tamperedCiphertext_throwsCryptoException() {
        byte[] plain = "sensitive".getBytes(StandardCharsets.UTF_8);
        byte[] blob  = crypto.encrypt(TEST_KEY, plain, null);
        blob[CryptoProcessor.GCM_IV_SIZE_BYTES]++;  // flip one ciphertext byte
        CryptoException ex = assertThrows(CryptoException.class,
            () -> crypto.decrypt(TEST_KEY, blob, null));
        assertEquals(CryptoException.Operation.DECRYPT, ex.getOperation());
    }

    @Test
    void decrypt_tamperedTag_throwsCryptoException() {
        byte[] blob = crypto.encrypt(TEST_KEY,
            "tag-tamper".getBytes(StandardCharsets.UTF_8), null);
        blob[blob.length - 1]++;   // corrupt last tag byte
        assertThrows(CryptoException.class, () -> crypto.decrypt(TEST_KEY, blob, null));
    }

    @Test
    void decrypt_wrongAAD_throwsCryptoException() {
        byte[] plain   = "aad-test".getBytes(StandardCharsets.UTF_8);
        byte[] correctAad = "correct".getBytes(StandardCharsets.UTF_8);
        byte[] wrongAad   = "WRONG  ".getBytes(StandardCharsets.UTF_8);
        byte[] blob    = crypto.encrypt(TEST_KEY, plain, correctAad);
        assertThrows(CryptoException.class,
            () -> crypto.decrypt(TEST_KEY, blob, wrongAad));
    }

    @Test
    void decrypt_wrongKey_throwsCryptoException() {
        byte[] plain    = "wrong-key".getBytes(StandardCharsets.UTF_8);
        byte[] blob     = crypto.encrypt(TEST_KEY, plain, null);
        byte[] otherKey = new byte[CryptoProcessor.AES_KEY_SIZE_BYTES];
        Arrays.fill(otherKey, (byte) 0xFF);
        assertThrows(CryptoException.class,
            () -> crypto.decrypt(otherKey, blob, null));
    }

    // ── SHA-256 / SHA-512 ─────────────────────────────────────────────

    @Test
    void sha256_knownVector() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        byte[] digest = crypto.computeHash(
            new byte[]{}, CryptoProcessor.HASH_SHA256);
        // Java side rejects empty arrays — use a known non-empty vector instead:
        // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2ec73b00361bbef0469348423f656b5c1d08
        byte[] abc = "abc".getBytes(StandardCharsets.UTF_8);
        String hex  = HexFormat.of().formatHex(
            crypto.computeHash(abc, CryptoProcessor.HASH_SHA256));
        assertEquals("ba7816bf8f01cfea414140de5dae2ec73b00361bbef0469348423f656b5c1d08", hex);
    }

    @Test
    void sha256_outputLength() {
        byte[] d = crypto.computeHash("test".getBytes(StandardCharsets.UTF_8),
                                       CryptoProcessor.HASH_SHA256);
        assertEquals(CryptoProcessor.SHA256_SIZE_BYTES, d.length);
    }

    @Test
    void sha512_outputLength() {
        byte[] d = crypto.computeHash("test".getBytes(StandardCharsets.UTF_8),
                                       CryptoProcessor.HASH_SHA512);
        assertEquals(CryptoProcessor.SHA512_SIZE_BYTES, d.length);
    }

    @Test
    void hash_differentInputs_produceDifferentDigests() {
        byte[] d1 = crypto.computeHash("a".getBytes(StandardCharsets.UTF_8),
                                        CryptoProcessor.HASH_SHA256);
        byte[] d2 = crypto.computeHash("b".getBytes(StandardCharsets.UTF_8),
                                        CryptoProcessor.HASH_SHA256);
        assertFalse(Arrays.equals(d1, d2));
    }

    // ── HMAC-SHA256 ───────────────────────────────────────────────────

    @Test
    void hmacSha256_outputLength() {
        byte[] mac = crypto.computeHmacSha256(
            "key".getBytes(StandardCharsets.UTF_8),
            "data".getBytes(StandardCharsets.UTF_8));
        assertEquals(CryptoProcessor.HMAC_SHA256_SIZE_BYTES, mac.length);
    }

    @Test
    void hmacSha256_deterministicForSameInputs() {
        byte[] key  = "k".getBytes(StandardCharsets.UTF_8);
        byte[] data = "d".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(
            crypto.computeHmacSha256(key, data),
            crypto.computeHmacSha256(key, data));
    }

    @Test
    void hmacSha256_differentKeys_produceDifferentMacs() {
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        byte[] m1   = crypto.computeHmacSha256("key1".getBytes(StandardCharsets.UTF_8), data);
        byte[] m2   = crypto.computeHmacSha256("key2".getBytes(StandardCharsets.UTF_8), data);
        assertFalse(Arrays.equals(m1, m2));
    }

    // ── PBKDF2 ────────────────────────────────────────────────────────

    @Test
    void pbkdf2_outputLength_matchesRequest() {
        byte[] key = crypto.deriveKeyPBKDF2(
            "password".getBytes(StandardCharsets.UTF_8),
            "salt1234".getBytes(StandardCharsets.UTF_8),
            1000, 32);
        assertEquals(32, key.length);
    }

    @Test
    void pbkdf2_deterministicForSameInputs() {
        byte[] pass = "passphrase".getBytes(StandardCharsets.UTF_8);
        byte[] salt = "somesalt".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(
            crypto.deriveKeyPBKDF2(pass, salt, 1000, 32),
            crypto.deriveKeyPBKDF2(pass, salt, 1000, 32));
    }

    @Test
    void pbkdf2_differentSalts_produceDifferentKeys() {
        byte[] pass  = "password".getBytes(StandardCharsets.UTF_8);
        byte[] salt1 = "salt-one".getBytes(StandardCharsets.UTF_8);
        byte[] salt2 = "salt-two".getBytes(StandardCharsets.UTF_8);
        assertFalse(Arrays.equals(
            crypto.deriveKeyPBKDF2(pass, salt1, 1000, 32),
            crypto.deriveKeyPBKDF2(pass, salt2, 1000, 32)));
    }

    @Test
    void pbkdf2_variousOutputLengths() {
        byte[] pass = "p".getBytes(StandardCharsets.UTF_8);
        byte[] salt = "s".getBytes(StandardCharsets.UTF_8);
        for (int len : new int[]{16, 24, 32, 48, 64}) {
            assertEquals(len,
                crypto.deriveKeyPBKDF2(pass, salt, 100, len).length,
                "PBKDF2 output length mismatch for requested=" + len);
        }
    }

    // ── generateSecureRandom ──────────────────────────────────────────

    @Test
    void generateRandom_producesRequestedLength() {
        for (int len : new int[]{1, 16, 32, 64, 256}) {
            assertEquals(len, crypto.generateSecureRandom(len).length);
        }
    }

    @Test
    void generateRandom_twoCallsDiffer() {
        byte[] r1 = crypto.generateSecureRandom(32);
        byte[] r2 = crypto.generateSecureRandom(32);
        assertFalse(Arrays.equals(r1, r2), "Random outputs should differ");
    }

    // ── error handling ────────────────────────────────────────────────

    @Test
    void encrypt_nullKey_throwsNPE() {
        assertThrows(NullPointerException.class,
            () -> crypto.encrypt(null, new byte[]{1}, null));
    }

    @Test
    void encrypt_wrongKeyLength_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
            () -> crypto.encrypt(new byte[16], new byte[]{1}, null));
    }

    @Test
    void decrypt_tooShortBlob_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
            () -> crypto.decrypt(TEST_KEY, new byte[5], null));
    }

    @Test
    void hash_invalidAlgorithm_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
            () -> crypto.computeHash(new byte[]{1}, 99));
    }

    @Test
    void pbkdf2_zeroIterations_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
            () -> crypto.deriveKeyPBKDF2(
                "p".getBytes(StandardCharsets.UTF_8),
                "s".getBytes(StandardCharsets.UTF_8), 0, 32));
    }

    @Test
    void generateRandom_zeroCount_throwsIAE() {
        assertThrows(IllegalArgumentException.class,
            () -> crypto.generateSecureRandom(0));
    }
}