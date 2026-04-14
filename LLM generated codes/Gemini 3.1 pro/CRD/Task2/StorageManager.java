import java.util.Arrays;

// The class responsible for managing data storage
public class StorageManager {
    private final DataCompressor compressor;

    public StorageManager() {
        // Initialize the compressor dependency
        this.compressor = new DataCompressor();
    }

    public void saveToFile(String filename, byte[] rawData) {
        System.out.println("Original data size: " + rawData.length + " bytes.");
        
        // Delegate compression to the native implementation
        byte[] compressedData = compressor.compress(rawData);
        
        System.out.println("Compressed data size: " + compressedData.length + " bytes.");
        // TODO: Logic to write 'compressedData' to the disk using 'filename'
        System.out.println("Successfully saved to: " + filename);
    }

    public byte[] loadFromFile(String filename) {
        // TODO: Logic to read raw bytes from the disk using 'filename'
        byte[] compressedDataReadFromDisk = new byte[]{ /* mock data */ 120, -100, 5, 12 }; 
        
        System.out.println("Decompressing data loaded from storage...");
        
        // Delegate decompression to the native implementation
        return compressor.decompress(compressedDataReadFromDisk);
    }
}