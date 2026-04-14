/**
 * High-speed file checksum calculator using native implementation
 */
public class HighSpeedChecksumCalculator {
    
    static {
        System.loadLibrary("highspeed_checksum_native");
    }
    
    // Native method to calculate checksum for a file
    private native String calculateFileChecksum(String filePath);
    
    // Native method to calculate checksum for byte array data
    private native String calculateDataChecksum(byte[] data);
    
    // Native method to calculate checksum for a portion of byte array
    private native String calculateDataChecksumRange(byte[] data, int offset, int length);
    
    /**
     * Calculate checksum for a file
     * @param filePath Path to the file to calculate checksum for
     * @return Checksum string (e.g., MD5, SHA-256 hash)
     * @throws IllegalArgumentException if file path is null or invalid
     */
    public String getFileChecksum(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        return calculateFileChecksum(filePath);
    }
    
    /**
     * Calculate checksum for byte array data
     * @param data Byte array to calculate checksum for
     * @return Checksum string
     * @throws IllegalArgumentException if data is null
     */
    public String getDataChecksum(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        return calculateDataChecksum(data);
    }
    
    /**
     * Calculate checksum for a portion of byte array
     * @param data Byte array to calculate checksum for
     * @param offset Starting offset in the array
     * @param length Number of bytes to include in calculation
     * @return Checksum string
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String getDataChecksumRange(byte[] data, int offset, int length) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IllegalArgumentException("Invalid offset or length parameters");
        }
        return calculateDataChecksumRange(data, offset, length);
    }
}