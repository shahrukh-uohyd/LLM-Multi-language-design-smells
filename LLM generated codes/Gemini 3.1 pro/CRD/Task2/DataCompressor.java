// New utility class specifically for compression
public class DataCompressor {
    static {
        // Loads the new native library (e.g., libdatacompression.so or datacompression.dll)
        System.loadLibrary("datacompression");
    }

    // Native method to compress a byte array
    public native byte[] compress(byte[] inputData);

    // Native method to decompress a byte array
    public native byte[] decompress(byte[] compressedData);
}