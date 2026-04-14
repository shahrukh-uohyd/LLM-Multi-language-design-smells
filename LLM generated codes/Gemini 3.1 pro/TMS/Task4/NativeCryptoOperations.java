public class NativeCryptoOperations {

    // Load the shared C/C++ security library into memory
    static {
        // Loads crypto_native.dll on Windows, libcrypto_native.so on Linux, or libcrypto_native.dylib on macOS
        System.loadLibrary("crypto_native");
    }

    // ---------------------------------------------------------
    // 1. SHA-256 Hasher
    // ---------------------------------------------------------
    public static class Sha256Hasher {
        /**
         * Computes the SHA-256 hash of the provided input data using native hardware 
         * acceleration (e.g., Intel SHA Extensions or ARM CE).
         *
         * @param data The raw input data to hash.
         * @return A 32-byte array containing the SHA-256 hash result.
         */
        public native byte[] computeSha256(byte[] data);
    }

    // ---------------------------------------------------------
    // 2. RSA Digital Signature Creator
    // ---------------------------------------------------------
    public static class RsaSigner {
        /**
         * Creates an RSA digital signature for the given data using a provided private key.
         * The native backend typically delegates this to libraries like OpenSSL or BoringSSL.
         *
         * @param dataToSign      The raw payload or pre-computed hash of the data to be signed.
         * @param privateKeyBytes The RSA private key, usually exported in PKCS#8 DER format.
         * @return The generated RSA digital signature as a byte array.
         */
        public native byte[] createSignature(byte[] dataToSign, byte[] privateKeyBytes);
    }

    // ---------------------------------------------------------
    // 3. Secure Random Number Generator (CSPRNG)
    // ---------------------------------------------------------
    public static class SecureRandomGenerator {
        /**
         * Generates a sequence of cryptographically secure random bytes by directly polling 
         * native OS-level entropy sources (e.g., /dev/urandom on Linux/macOS or 
         * BCryptGenRandom on Windows).
         *
         * @param length The number of random bytes requested.
         * @return A byte array of the specified length containing the secure random data.
         */
        public native byte[] generateRandomBytes(int length);
    }

    // ---------------------------------------------------------
    // Example Usage
    // ---------------------------------------------------------
    public static void main(String[] args) {
        // 1. SHA-256 Example
        Sha256Hasher hasher = new Sha256Hasher();
        byte[] data = "Hello, secure world!".getBytes();
        byte[] hash = hasher.computeSha256(data);
        System.out.println("SHA-256 generated: " + hash.length + " bytes");

        // 2. RSA Signature Example
        RsaSigner signer = new RsaSigner();
        byte[] dummyPrivateKey = new byte[2048 / 8]; // Simulated 2048-bit key bytes
        byte[] signature = signer.createSignature(data, dummyPrivateKey);
        // Signature length depends on key size, e.g., 256 bytes for RSA-2048
        System.out.println("RSA Signature generated. Length: " + (signature != null ? signature.length : 0) + " bytes");

        // 3. Secure Random Example
        SecureRandomGenerator randomGen = new SecureRandomGenerator();
        byte[] iv = randomGen.generateRandomBytes(16); // e.g., generating a 16-byte AES IV
        System.out.println("Generated CSPRNG bytes: " + iv.length + " bytes");
    }
}