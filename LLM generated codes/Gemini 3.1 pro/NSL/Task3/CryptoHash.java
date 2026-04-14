public class CryptoHash {

    // Load the native C library during class initialization
    static {
        // The library will be named libcryptohash.so (Linux), libcryptohash.dylib (Mac), or cryptohash.dll (Windows)
        System.loadLibrary("cryptohash");
    }

    /**
     * Native method to compute the SHA-256 hash of a given byte array.
     * 
     * @param inputData The raw data to be hashed.
     * @return A 32-byte array representing the SHA-256 hash.
     */
    public native byte[] computeSHA256(byte[] inputData);

    public static void main(String[] args) {
        CryptoHash crypto = new CryptoHash();

        String message = "Hello, JNI Cryptography!";
        System.out.println("Original Message: " + message);

        // Convert the string to bytes and call the native hashing method
        byte[] inputBytes = message.getBytes();
        byte[] hashBytes = crypto.computeSHA256(inputBytes);

        // Convert the returned raw bytes to a Hexadecimal string for display
        String hexHash = bytesToHex(hashBytes);
        
        System.out.println("SHA-256 Hash    : " + hexHash);
    }

    /**
     * Helper method to convert a byte array to a hexadecimal string.
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}