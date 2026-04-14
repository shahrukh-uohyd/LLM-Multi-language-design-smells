/**
 * Holds the raw encryption key and its associated metadata.
 * Passed into the native layer to initialize the encryption engine.
 */
public class SecurityKey {

    public enum Algorithm { AES_128, AES_256 }

    private final byte[]    keyBytes;       // Raw key material
    private final Algorithm algorithm;      // Algorithm identifier
    private final String    keyId;          // Unique key identifier / label
    private       boolean   revoked;        // Whether this key has been revoked

    public SecurityKey(byte[] keyBytes, Algorithm algorithm, String keyId) {
        if (keyBytes == null || keyBytes.length == 0)
            throw new IllegalArgumentException("keyBytes must not be null or empty");
        this.keyBytes  = keyBytes.clone();  // Defensive copy
        this.algorithm = algorithm;
        this.keyId     = keyId;
        this.revoked   = false;
    }

    /* ── Getters (called from C++ via JNI) ─────────────────────────── */
    public byte[]    getKeyBytes()  { return keyBytes.clone(); }
    public String    getAlgorithm() { return algorithm.name(); }
    public String    getKeyId()     { return keyId;            }
    public boolean   isRevoked()    { return revoked;          }

    /** Revoke this key so it can no longer be used. */
    public void revoke() { this.revoked = true; }
}