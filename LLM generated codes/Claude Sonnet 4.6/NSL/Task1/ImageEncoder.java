/**
 * Java wrapper that exposes native image-encoding operations via JNI.
 *
 * Supported formats (passed as the `format` parameter):
 *   "base64"  – encodes raw pixel bytes to a Base64 string
 *   "rle"     – encodes raw pixel bytes with run-length encoding
 *   "hex"     – encodes raw pixel bytes to a lowercase hex string
 */
public class ImageEncoder {

    // ------------------------------------------------------------------ //
    //  Native library loading                                              //
    // ------------------------------------------------------------------ //

    static {
        /*
         * The shared library must be on java.library.path.
         * Build with build.sh, then run with:
         *   java -Djava.library.path=./native/build ImageEncoder
         */
        System.loadLibrary("ImageEncoder");
    }

    // ------------------------------------------------------------------ //
    //  Native method declarations                                          //
    // ------------------------------------------------------------------ //

    /**
     * Encodes raw pixel data to a Base64 string using the native library.
     *
     * @param pixelData  raw image bytes (e.g. RGBA row-major order)
     * @param width      image width in pixels
     * @param height     image height in pixels
     * @param channels   bytes per pixel (1=grayscale, 3=RGB, 4=RGBA)
     * @return           Base64-encoded string
     */
    public native String encodeToBase64(byte[] pixelData, int width, int height, int channels);

    /**
     * Encodes raw pixel data with run-length encoding (RLE).
     *
     * @param pixelData  raw image bytes
     * @param width      image width in pixels
     * @param height     image height in pixels
     * @param channels   bytes per pixel
     * @return           RLE byte array [count, value, count, value, ...]
     */
    public native byte[] encodeWithRLE(byte[] pixelData, int width, int height, int channels);

    /**
     * Encodes raw pixel data to a lowercase hexadecimal string.
     *
     * @param pixelData  raw image bytes
     * @param width      image width in pixels
     * @param height     image height in pixels
     * @param channels   bytes per pixel
     * @return           hex string (two characters per byte)
     */
    public native String encodeToHex(byte[] pixelData, int width, int height, int channels);

    /**
     * Returns metadata about the native encoding library.
     *
     * @return descriptive string (name, version, build info)
     */
    public native String getNativeLibraryInfo();

    // ------------------------------------------------------------------ //
    //  Validation helper (pure Java)                                       //
    // ------------------------------------------------------------------ //

    /**
     * Validates pixel array consistency before any native call.
     */
    public void validateImageData(byte[] pixelData, int width, int height, int channels) {
        if (pixelData == null)
            throw new IllegalArgumentException("pixelData must not be null");
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Width and height must be positive");
        if (channels < 1 || channels > 4)
            throw new IllegalArgumentException("channels must be between 1 and 4");

        long expected = (long) width * height * channels;
        if (pixelData.length != expected)
            throw new IllegalArgumentException(
                "pixelData length " + pixelData.length +
                " does not match width*height*channels = " + expected);
    }

    // ------------------------------------------------------------------ //
    //  High-level encode entry point                                       //
    // ------------------------------------------------------------------ //

    /**
     * Validates, then delegates to the appropriate native encoder.
     *
     * @param pixelData  raw image bytes
     * @param width      image width
     * @param height     image height
     * @param channels   bytes per pixel
     * @param format     "base64", "rle", or "hex"
     * @return           an {@link EncodingResult}
     */
    public EncodingResult encode(byte[] pixelData, int width, int height,
                                 int channels, String format) {
        validateImageData(pixelData, width, height, channels);

        long startNs = System.nanoTime();

        switch (format.toLowerCase()) {
            case "base64": {
                String encoded = encodeToBase64(pixelData, width, height, channels);
                return new EncodingResult(format, encoded.getBytes(), encoded,
                                          System.nanoTime() - startNs);
            }
            case "rle": {
                byte[] encoded = encodeWithRLE(pixelData, width, height, channels);
                return new EncodingResult(format, encoded, null,
                                          System.nanoTime() - startNs);
            }
            case "hex": {
                String encoded = encodeToHex(pixelData, width, height, channels);
                return new EncodingResult(format, encoded.getBytes(), encoded,
                                          System.nanoTime() - startNs);
            }
            default:
                throw new IllegalArgumentException(
                    "Unknown format: " + format +
                    ". Supported: base64, rle, hex");
        }
    }

    // ------------------------------------------------------------------ //
    //  Result type                                                         //
    // ------------------------------------------------------------------ //

    /** Holds the result of one encoding operation. */
    public static class EncodingResult {
        private final String format;
        private final byte[] encodedBytes;
        private final String encodedString;   // null for binary-only formats
        private final long   elapsedNs;

        public EncodingResult(String format, byte[] encodedBytes,
                              String encodedString, long elapsedNs) {
            this.format        = format;
            this.encodedBytes  = encodedBytes;
            this.encodedString = encodedString;
            this.elapsedNs     = elapsedNs;
        }

        public String getFormat()        { return format; }
        public byte[] getEncodedBytes()  { return encodedBytes; }
        public String getEncodedString() { return encodedString; }
        public long   getElapsedNs()     { return elapsedNs; }
        public int    getEncodedSize()   { return encodedBytes != null ? encodedBytes.length : 0; }

        @Override
        public String toString() {
            return String.format(
                "EncodingResult{format='%s', encodedSize=%d bytes, elapsedMs=%.3f}",
                format, getEncodedSize(), elapsedNs / 1_000_000.0);
        }
    }
}