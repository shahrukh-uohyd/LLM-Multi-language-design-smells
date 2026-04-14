// Main.java - Example usage
public class Main {
    public static void main(String[] args) {
        PlatformUtils utils = new PlatformUtils();
        DataStorageManager storage = new DataStorageManager();
        CompressionService service = new Service();
        
        // Show platform info
        System.out.println("Platform: " + utils.getPlatformName());
        System.out.println("Architecture: " + utils.getSystemArchitecture());
        System.out.println("Username: " + utils.getUserName());
        
        // Test compression
        String testData = "This is a test string for compression. It contains repeated patterns like ABCABCABC and other repetitive text to make compression effective.";
        byte[] originalData = testData.getBytes();
        
        System.out.println("\nOriginal data size: " + originalData.length + " bytes");
        
        // Test GZIP compression
        byte[] compressedData = utils.compressGzip(originalData);
        System.out.println("Compressed data size: " + compressedData.length + " bytes");
        System.out.println("Compression ratio: " + String.format("%.2f%%", (double)compressedData.length/originalData.length * 100));
        
        // Test decompression
        byte[] decompressedData = utils.decompressGzip(compressedData);
        String decompressedString = new String(decompressedData);
        System.out.println("Decompressed matches original: " + testData.equals(decompressedString));
        
        // Test data storage with compression
        storage.storeCompressedData("test_key", originalData);
        byte[] retrievedData = storage.retrieveCompressedData("test_key");
        String retrievedString = new String(retrievedData);
        System.out.println("Retrieved from storage matches: " + testData.equals(retrievedString));
        
        // Test custom compression levels
        for (int level = 1; level <= 9; level++) {
            byte[] levelCompressed = utils.compressWithLevel(originalData, level);
            System.out.println("Level " + level + " compression: " + levelCompressed.length + " bytes");
        }
        
        // Test validation
        boolean isValid = utils.validateCompressedData(compressedData);
        System.out.println("Compressed data validation: " + isValid);
        
        // Test multi-algorithm comparison
        CompressionService.MultiAlgorithmResult result = service.testMultiAlgorithm(originalData);
        System.out.println("\nGZIP compression ratio: " + String.format("%.2f%%", result.gzipStats.ratio * 100));
        System.out.println("GZIP space savings: " + String.format("%.2f%%", result.gzipStats.savingsPercent));
        System.out.println("DEFLATE compression ratio: " + String.format("%.2f%%", result.deflateStats.ratio * 100));
        System.out.println("DEFLATE space savings: " + String.format("%.2f%%", result.deflateStats.savingsPercent));
        
        System.out.println("\nCached data count: " + storage.getCachedDataCount());
    }
}