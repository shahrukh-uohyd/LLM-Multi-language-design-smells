// PlatformUtils.java
public class PlatformUtils {
    static {
        System.loadLibrary("platform_compression");
    }

    // Existing native platform-specific methods
    public native String getPlatformName();
    public native String getSystemArchitecture();
    public native long getAvailableMemory();
    public native String getTempDirectory();
    public native boolean createProcess(String command);
    public native String getUserName();
    public native String getMachineId();

    // New compression native methods added to this class
    public native byte[] compressData(byte[] data, String algorithm);
    public native byte[] decompressData(byte[] compressedData, String algorithm);
    public native int getCompressionRatio(byte[] originalData, byte[] compressedData);
    public native byte[] compressWithLevel(byte[] data, int compressionLevel);
    public native boolean validateCompressedData(byte[] compressedData);

    // Convenience methods
    public byte[] compressGzip(byte[] data) {
        return compressData(data, "GZIP");
    }

    public byte[] compressDeflate(byte[] data) {
        return compressData(data, "DEFLATE");
    }

    public byte[] decompressGzip(byte[] compressedData) {
        return decompressData(compressedData, "GZIP");
    }

    public byte[] decompressDeflate(byte[] compressedData) {
        return decompressData(compressedData, "DEFLATE");
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}