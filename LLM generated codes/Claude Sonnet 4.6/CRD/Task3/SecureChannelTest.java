package com.app.comms;

import com.app.crypto.CryptoException;
import com.app.crypto.CryptoNative;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SecureChannel}.
 * Exercises the full Java → JNI → OpenSSL path.
 *
 * Run after building libcrypto_native with:
 *   mvn test  OR  gradle test
 *
 * The native library must be on java.library.path:
 *   -Djava.library.path=lib
 */
class SecureChannelTest {

    private static final byte[] MASTER_SECRET =
        "this-is-a-32-byte-master-secret!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HKDF_SALT =
        "test-salt-value".getBytes(StandardCharsets.UTF_8);

    private SecureChannel channel;

    @BeforeEach
    void setUp() {
        channel = new SecureChannel("test-channel", MASTER_SECRET, HKDF_SALT);
    }

    @AfterEach
    void tearDown() {
        channel.close();
    }

    // ── openSSLVersion ────────────────────────────────────────────────

    @Test
    void openSSLVersion_isNonEmpty() {
        String version = new CryptoNative().openSSLVersion();
        assertNotNull(version);
        assertFalse(version.isBlank());
        System.out.println("OpenSSL version: " + version);
    }

    // ── send / receive round-trip ─────────────────────────────────────

    @Test
    void roundTrip_shortMessage() {
        byte[] original = "Hello, SecureChannel!".getBytes(StandardCharsets.UTF_8);
        byte[] cipher   = channel.send(original);
        byte[] result   = channel.receive(cipher);
        assertArrayEquals(original, result, "round-trip must produce identical bytes");
    }

    @Test
    void roundTrip_binaryData() {
        byte[] original = new byte[4096];
        for (int i = 0; i < original.length; i++) original[i] = (byte) i;
        assertArrayEquals(original, channel.receive(channel.send(original)));
    }

    @Test
    void roundTrip_singleByte() {
        byte[] single = {(byte) 0xFF};
        assertArrayEquals(single, channel.receive(channel.send(single)));
    }

    @Test
    void encrypt_producesUniqueCiphertexts_forSamePlaintext() {
        byte[] pt = "same plaintext".getBytes(StandardCharsets.UTF_8);
        byte[] c1 = channel.send(pt);
        byte[] c2 = channel.send(pt);
        assertFalse(Arrays.equals(c1, c2),
            "each encrypt call must use a fresh IV → different ciphertext");
    }

    @Test
    void ciphertext_isLongerThanPlaintext() {
        byte[] pt = "payload".getBytes(StandardCharsets.UTF_8);
        byte[] ct = channel.send(pt);
        // IV (12) + ciphertext (same as plaintext) + tag (16)
        assertEquals(pt.length + CryptoNative.IV_LENGTH_BYTES + CryptoNative.TAG_LENGTH_BYTES,
                     ct.length);
    }

    // ── authentication / tamper detection ─────────────────────────────

    @Test
    void tampered_ciphertext_throwsCryptoException() {
        byte[] ct = channel.send("sensitive".getBytes(StandardCharsets.UTF_8));
        ct[CryptoNative.IV_LENGTH_BYTES]++; // flip one bit of the ciphertext
        assertThrows(CryptoException.class, () -> channel.receive(ct),
            "GCM tag verification must reject tampered ciphertext");
    }

    @Test
    void tampered_tag_throwsCryptoException() {
        byte[] ct = channel.send("sensitive".getBytes(StandardCharsets.UTF_8));
        ct[ct.length - 1]++; // corrupt the last tag byte
        assertThrows(CryptoException.class, () -> channel.receive(ct));
    }

    @Test
    void tampered_iv_throwsCryptoException() {
        byte[] ct = channel.send("sensitive".getBytes(StandardCharsets.UTF_8));
        ct[0]++; // corrupt first IV byte
        assertThrows(CryptoException.class, () -> channel.receive(ct));
    }

    // ── key derivation ────────────────────────────────────────────────

    @Test
    void deriveKey_differentInfoProducesDifferentKeys() {
        CryptoNative cn = new CryptoNative();
        byte[] k1 = cn.deriveKey(MASTER_SECRET, HKDF_SALT,
            "context-A".getBytes(StandardCharsets.UTF_8), 32);
        byte[] k2 = cn.deriveKey(MASTER_SECRET, HKDF_SALT,
            "context-B".getBytes(StandardCharsets.UTF_8), 32);
        assertFalse(Arrays.equals(k1, k2));
    }

    @Test
    void deriveKey_deterministic() {
        CryptoNative cn = new CryptoNative();
        byte[] info = "deterministic".getBytes(StandardCharsets.UTF_8);
        byte[] k1 = cn.deriveKey(MASTER_SECRET, HKDF_SALT, info, 32);
        byte[] k2 = cn.deriveKey(MASTER_SECRET, HKDF_SALT, info, 32);
        assertArrayEquals(k1, k2, "HKDF must be deterministic for identical inputs");
    }

    // ── key rotation ──────────────────────────────────────────────────

    @Test
    void rotateKeys_newChannelDecryptsAfterRotation() {
        byte[] newSecret = "another-32-byte-master-secret!!".getBytes(StandardCharsets.UTF_8);
        channel.rotateKeys(newSecret, null);
        byte[] original = "post-rotation payload".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(original, channel.receive(channel.send(original)));
    }

    @Test
    void oldCiphertext_invalidAfterRotation() {
        byte[] original = "before rotation".getBytes(StandardCharsets.UTF_8);
        byte[] oldCipher = channel.send(original);

        // Rotate to new key
        byte[] newSecret = "another-32-byte-master-secret!!".getBytes(StandardCharsets.UTF_8);
        channel.rotateKeys(newSecret, null);

        // Old ciphertext was encrypted with old key — must fail authentication
        assertThrows(CryptoException.class, () -> channel.receive(oldCipher));
    }

    // ── channel lifecycle ─────────────────────────────────────────────

    @Test
    void closedChannel_throwsIllegalStateException() {
        channel.close();
        assertThrows(IllegalStateException.class,
            () -> channel.send("x".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void close_isIdempotent() {
        assertDoesNotThrow(() -> { channel.close(); channel.close(); });
    }

    // ── error handling ────────────────────────────────────────────────

    @Test
    void send_nullPlaintextThrows() {
        assertThrows(NullPointerException.class, () -> channel.send(null));
    }

    @Test
    void receive_tooShortBlobThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> channel.receive(new byte[5]));
    }

    @Test
    void generateRandom_producesCorrectLength() {
        for (int len : new int[]{1, 16, 32, 64, 256}) {
            assertEquals(len, channel.generateRandom(len).length);
        }
    }

    @Test
    void generateRandom_twoCallsDiffer() {
        byte[] r1 = channel.generateRandom(32);
        byte[] r2 = channel.generateRandom(32);
        assertFalse(Arrays.equals(r1, r2), "random bytes should not repeat");
    }
}