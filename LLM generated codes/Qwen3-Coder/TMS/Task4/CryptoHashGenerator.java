/**
 * Native cryptographic hash generator for SHA-256 operations
 */
public class CryptoHashGenerator {
    
    static {
        System.loadLibrary("crypto_hash_native");
    }
    
    // Native method to generate SHA-256 hash from byte array
    private native byte[] nativeGenerateSHA256(byte[] input);
    
    // Native method to generate SHA-256 hash from string
    private native byte[] nativeGenerateSHA256FromString(String input);
    
    // Native method to generate SHA-256 hash from file
    private native byte[] nativeGenerateSHA256FromFile(String filePath);
    
    /**
     * Generate SHA-256 hash from byte array input
     * @param input Input data to hash
     * @return SHA-256 hash as byte array
     * @throws IllegalArgumentException if input is null
     */
    public byte[] generateSHA256(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        return nativeGenerateSHA256(input);
    }
    
    /**
     * Generate SHA-256 hash from string input
     * @param input Input string to hash
     * @return SHA-256 hash as byte array
     * @throws IllegalArgumentException if input is null
     */
    public byte[] generateSHA256(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }
        return nativeGenerateSHA256FromString(input);
    }
    
    /**
     * Generate SHA-256 hash from file content
     * @param filePath Path to the file to hash
     * @return SHA-256 hash as byte array
     * @throws IllegalArgumentException if file path is null or empty
     */
    public byte[] generateSHA256FromFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        return nativeGenerateSHA256FromFile(filePath);
    }
    
    /**
     * Convert byte array hash to hexadecimal string representation
     * @param hash SHA-256 hash as byte array
     * @return Hexadecimal string representation
     */
    public String hashToHex(byte[] hash) {
        if (hash == null) {
            return null;
        }
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}