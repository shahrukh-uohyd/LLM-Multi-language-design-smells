public class NativeCryptoLoader {

    // The static block executes once when the class is loaded
    static {
        // Get the OS name and convert to lowercase for easier matching
        String osName = System.getProperty("os.name").toLowerCase();

        try {
            if (osName.contains("win")) {
                // On Windows, this will look for 'crypto-native.dll'
                System.loadLibrary("crypto-native");
                
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                // On Linux, System.loadLibrary automatically adds the "lib" prefix and ".so" suffix.
                // By passing "crypto-linux", it will correctly look for 'libcrypto-linux.so'
                System.loadLibrary("crypto-linux");
                
            } else {
                // Handle unsupported operating systems (like macOS if not supported)
                throw new UnsupportedOperationException("Unsupported operating system for native AES: " + osName);
            }
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load the native AES crypto library: " + e.getMessage());
            // You can either throw a RuntimeException or fallback to a pure Java AES implementation here
            throw new RuntimeException("Native encryption library missing", e);
        }
    }

    /**
     * Example native method declaration for AES Encryption.
     * The 'native' keyword tells the JVM that the implementation is provided by the loaded C/C++ library.
     */
    public native byte[] encryptAES(byte[] plainText, byte[] key);

    /**
     * Example native method declaration for AES Decryption.
     */
    public native byte[] decryptAES(byte[] cipherText, byte[] key);
}