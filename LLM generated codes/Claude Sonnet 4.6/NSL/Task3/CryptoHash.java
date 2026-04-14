/**
 * Java wrapper that exposes native cryptographic hash operations via JNI.
 *
 * Supported algorithms (passed as the {@code algorithm} parameter):
 *   "MD5"     – 128-bit digest  (16 bytes  / 32 hex chars)
 *   "SHA1"    – 160-bit digest  (20 bytes  / 40 hex chars)
 *   "SHA256"  – 256-bit digest  (32 bytes  / 64 hex chars)
 *   "SHA512"  – 512-bit digest  (64 bytes  / 128 hex chars)
 *
 * All algorithms are implemented entirely in native C code (no OpenSSL).
 * The Java side handles validation, result wrapping, and display.
 */
public class CryptoHash {

    // ------------------------------------------------------------------ //
    //  Constants                                                           //
    // ------------------------------------------------------------------ //

    public static final String MD5    = "MD5";
    public static final String SHA1   = "SHA1";
    public static final String SHA256 = "SHA256";
    public static final String SHA512 = "SHA512";

    /** Digest sizes in bytes for each algorithm. */
    private static final java.util.Map<String, Integer> DIGEST_SIZES;
    static {
        DIGEST_SIZES = new java.util.LinkedHashMap<>();
        DIGEST_SIZES.put(MD5,    16);
        DIGEST_SIZES.put(SHA1,   20);
        DIGEST_SIZES.put(SHA256, 32);
        DIGEST_SIZES.put(SHA512, 64);
    }

    // ------------------------------------------------------------------ //
    //  Native library loading                                              //
    // ------------------------------------------------------------------ //

    static {
        /*
         * Shared library must be on java.library.path.
         * Build with build.sh, then run:
         *   java -Djava.library.path=./native/build -cp out Main
         */
        System.loadLibrary("CryptoHash");
    }

    // ------------------------------------------------------------------ //
    //  Native method declarations                                          //
    // ------------------------------------------------------------------ //

    /**
     * Computes an MD5 digest of the supplied data.
     *
     * @param data  input bytes
     * @return      16-byte raw digest
     */
    public native byte[] computeMD5(byte[] data);

    /**
     * Computes a SHA-1 digest of the supplied data.
     *
     * @param data  input bytes
     * @return      20-byte raw digest
     */
    public native byte[] computeSHA1(byte[] data);

    /**
     * Computes a SHA-256 digest of the supplied data.
     *
     * @param data  input bytes
     * @return      32-byte raw digest
     */
    public native byte[] computeSHA256(byte[] data);

    /**
     * Computes a SHA-512 digest of the supplied data.
     *
     * @param data  input bytes
     * @return      64-byte raw digest
     */
    public native byte[] computeSHA512(byte[] data);

    /**
     * Returns version / build information about the native library.
     *
     * @return descriptive string
     */
    public native String getNativeLibraryInfo();

    // ------------------------------------------------------------------ //
    //  High-level API                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Validates input, dispatches to the correct native hasher, and wraps
     * the result in a {@link HashResult}.
     *
     * @param data      bytes to hash  (must not be null)
     * @param algorithm one of MD5 / SHA1 / SHA256 / SHA512
     * @return          {@link HashResult} containing raw bytes and hex string
     * @throws IllegalArgumentException if data is null or algorithm unknown
     */
    public HashResult hash(byte[] data, String algorithm) {
        if (data == null)
            throw new IllegalArgumentException("Input data must not be null");
        if (algorithm == null || !DIGEST_SIZES.containsKey(algorithm.toUpperCase()))
            throw new IllegalArgumentException(
                "Unknown algorithm: " + algorithm +
                ". Supported: MD5, SHA1, SHA256, SHA512");

        long startNs = System.nanoTime();
        byte[] digest;

        switch (algorithm.toUpperCase()) {
            case MD5:    digest = computeMD5(data);    break;
            case SHA1:   digest = computeSHA1(data);   break;
            case SHA256: digest = computeSHA256(data); break;
            case SHA512: digest = computeSHA512(data); break;
            default:
                throw new IllegalArgumentException("Unreachable");
        }

        long elapsedNs = System.nanoTime() - startNs;
        return new HashResult(algorithm.toUpperCase(), digest, elapsedNs, data.length);
    }

    /**
     * Convenience overload that accepts a UTF-8 string.
     *
     * @param text      input string
     * @param algorithm hash algorithm name
     * @return          {@link HashResult}
     */
    public HashResult hash(String text, String algorithm) {
        if (text == null)
            throw new IllegalArgumentException("Input text must not be null");
        try {
            return hash(text.getBytes("UTF-8"), algorithm);
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 is always available in Java
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Static helpers                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Converts a raw digest byte array to a lowercase hex string.
     *
     * @param bytes raw digest
     * @return      lowercase hex string
     */
    public static String toHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }

    /**
     * Returns all supported algorithm names.
     *
     * @return array of algorithm name strings
     */
    public static String[] supportedAlgorithms() {
        return DIGEST_SIZES.keySet().toArray(new String[0]);
    }

    // ------------------------------------------------------------------ //
    //  Result value object                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Immutable result of one hashing operation.
     */
    public static class HashResult {

        private final String algorithm;
        private final byte[] digestBytes;
        private final String digestHex;
        private final long   elapsedNs;
        private final int    inputLength;

        public HashResult(String algorithm, byte[] digestBytes,
                          long elapsedNs, int inputLength) {
            this.algorithm   = algorithm;
            this.digestBytes = digestBytes;
            this.digestHex   = toHex(digestBytes);
            this.elapsedNs   = elapsedNs;
            this.inputLength = inputLength;
        }

        public String getAlgorithm()   { return algorithm; }
        public byte[] getDigestBytes() { return digestBytes; }
        public String getDigestHex()   { return digestHex; }
        public int    getDigestSize()  { return digestBytes != null ? digestBytes.length : 0; }
        public long   getElapsedNs()   { return elapsedNs; }
        public int    getInputLength() { return inputLength; }

        @Override
        public String toString() {
            return String.format(
                "HashResult{algo=%s, inputBytes=%d, digestBits=%d, " +
                "hex=%s, elapsedMs=%.4f}",
                algorithm, inputLength, getDigestSize() * 8,
                digestHex, elapsedNs / 1_000_000.0);
        }
    }
}