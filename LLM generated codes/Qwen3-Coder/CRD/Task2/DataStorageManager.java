// DataStorageManager.java
import java.util.HashMap;
import java.util.Map;

public class DataStorageManager {
    private PlatformUtils platformUtils;
    private Map<String, byte[]> cachedData;
    
    public DataStorageManager() {
        this.platformUtils = new PlatformUtils();
        this.cachedData = new HashMap<>();
    }

    /**
     * Stores data after compressing it
     */
    public boolean storeCompressedData(String key, byte[] data) {
        try {
            byte[] compressedData = platformUtils.compressGzip(data);
            cachedData.put(key, compressedData);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to store compressed data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves and decompresses stored data
     */
    public byte[] retrieveCompressedData(String key) {
        try {
            byte[] compressedData = cachedData.get(key);
            if (compressedData == null) {
                return null;
            }
            
            return platformUtils.decompressGzip(compressedData);
        } catch (Exception e) {
            System.err.println("Failed to retrieve compressed data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Compresses and stores data to a file (simulated)
     */
    public boolean saveCompressedToFile(String filename, byte[] data, String algorithm) {
        try {
            byte[] compressedData = platformUtils.compressData(data, algorithm);
            // Simulate file writing - in real app, you'd write to actual file
            System.out.println("Compressed data saved to: " + filename);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save compressed file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads and decompresses data from a file (simulated)
     */
    public byte[] loadCompressedFromFile(String filename, String algorithm) {
        try {
            // Simulate file reading - in real app, you'd read from actual file
            byte[] dummyData = ("Simulated file data for " + filename).getBytes();
            byte[] compressedData = platformUtils.compressData(dummyData, algorithm);
            return platformUtils.decompressData(compressedData, algorithm);
        } catch (Exception e) {
            System.err.println("Failed to load compressed file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Calculates compression ratio for data
     */
    public double calculateCompressionRatio(byte[] originalData) {
        try {
            byte[] compressedData = platformUtils.compressGzip(originalData);
            if (originalData.length == 0) {
                return 0.0;
            }
            return (double) compressedData.length / originalData.length;
        } catch (Exception e) {
            System.err.println("Failed to calculate compression ratio: " + e.getMessage());
            return -1.0;
        }
    }

    /**
     * Validates compressed data integrity
     */
    public boolean validateStoredData(String key) {
        byte[] compressedData = cachedData.get(key);
        if (compressedData == null) {
            return false;
        }
        return platformUtils.validateCompressedData(compressedData);
    }

    /**
     * Stores data with custom compression level
     */
    public boolean storeWithCustomLevel(String key, byte[] data, int level) {
        try {
            byte[] compressedData = platformUtils.compressWithLevel(data, level);
            cachedData.put(key, compressedData);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to store data with custom level: " + e.getMessage());
            return false;
        }
    }

    public int getCachedDataCount() {
        return cachedData.size();
    }
}