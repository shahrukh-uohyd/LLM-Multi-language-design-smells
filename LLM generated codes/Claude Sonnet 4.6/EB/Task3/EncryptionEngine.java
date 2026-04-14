import java.util.Base64;

/**
 * Java-side encryption engine.
 * Delegates key initialization and all crypto operations to native C++.
 */
public class EncryptionEngine {

    static {
        System.loadLibrary("NativeEncryptionEngine");
    }

    /* ── Native method declarations ─────────────────────────────────── */

    /**
     * Initializes the native encryption engine with the key stored
     * inside the given SecurityKey object.
     *
     * @param securityKey  the Java SecurityKey whose key material to load
     * @return             true if the engine was successfully initialized
     */
    public native boolean initializeEngine(SecurityKey securityKey);

    /**
     * Encrypts plaintext bytes using the initialized engine.
     *
     * @param plaintext  raw bytes to encrypt
     * @return           ciphertext bytes, or null on failure
     */
    public native byte[] encrypt(byte[] plaintext);

    /**
     * Decrypts ciphertext bytes using the initialized engine.
     *
     * @param ciphertext  raw bytes to decrypt
     * @return            plaintext bytes, or null on failure
     */
    public native byte[] decrypt(byte[] ciphertext);

    /**
     * Returns the current engine status as a human-readable string.
     */
    public native String getEngineStatus();

    /**
     * Securely wipes the key material from the native engine and
     * shuts it down.
     */
    public native void shutdownEngine();

    /* ── Demo main ──────────────────────────────────────────────────── */
    public static void main(String[] args) {

        // 32-byte (256-bit) key — use a secure random source in production
        byte[] rawKey = "01234567890123456789012345678901".getBytes();

        SecurityKey    key    = new SecurityKey(rawKey,
                                               SecurityKey.Algorithm.AES_256,
                                               "key-demo-001");
        EncryptionEngine engine = new EncryptionEngine();

        System.out.println("=== Initialize Engine ===");
        boolean ok = engine.initializeEngine(key);
        System.out.println("Initialized : " + ok);
        System.out.println("Status      : " + engine.getEngineStatus());

        if (ok) {
            String  message   = "Hello, secure world!";
            byte[]  plaintext = message.getBytes();

            System.out.println("\n=== Encrypt ===");
            byte[] ciphertext = engine.encrypt(plaintext);
            System.out.println("Ciphertext (Base64): "
                               + Base64.getEncoder().encodeToString(ciphertext));

            System.out.println("\n=== Decrypt ===");
            byte[] decrypted = engine.decrypt(ciphertext);
            System.out.println("Decrypted text : " + new String(decrypted));

            System.out.println("\n=== Shutdown ===");
            engine.shutdownEngine();
            System.out.println("Status after shutdown: " + engine.getEngineStatus());
        }
    }
}