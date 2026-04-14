import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class SecurityKey {
    private byte[] encryptionKey;
    private String algorithm;
    private String keyPurpose;
    private long creationTime;
    private boolean isValid;

    public SecurityKey(byte[] keyBytes, String algorithm) {
        this.encryptionKey = keyBytes.clone(); // Clone to prevent external modification
        this.algorithm = algorithm;
        this.keyPurpose = "ENCRYPTION";
        this.creationTime = System.currentTimeMillis();
        this.isValid = true;
    }

    public SecurityKey(String keyHex, String algorithm) {
        this.encryptionKey = hexToBytes(keyHex);
        this.algorithm = algorithm;
        this.keyPurpose = "ENCRYPTION";
        this.creationTime = System.currentTimeMillis();
        this.isValid = true;
    }

    // Factory method to create a random key
    public static SecurityKey generateRandomKey(String algorithm, int keySizeBits) {
        byte[] keyBytes = new byte[keySizeBits / 8];
        new SecureRandom().nextBytes(keyBytes);
        return new SecurityKey(keyBytes, algorithm);
    }

    // Getters
    public byte[] getEncryptionKey() {
        return encryptionKey != null ? encryptionKey.clone() : null;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getKeyPurpose() {
        return keyPurpose;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public boolean isValid() {
        return isValid;
    }

    public void invalidate() {
        this.isValid = false;
        // Clear the key bytes for security
        if (this.encryptionKey != null) {
            for (int i = 0; i < this.encryptionKey.length; i++) {
                this.encryptionKey[i] = 0;
            }
        }
    }

    // Utility method to convert hex string to byte array
    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    // Utility method to convert byte array to hex string
    public String bytesToHex() {
        StringBuilder sb = new StringBuilder();
        if (encryptionKey != null) {
            for (byte b : encryptionKey) {
                sb.append(String.format("%02x", b));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("SecurityKey{algorithm='%s', purpose='%s', valid=%s, size=%d bytes}",
                algorithm, keyPurpose, isValid, encryptionKey != null ? encryptionKey.length : 0);
    }
}