import java.io.*;
import java.util.Base64;

public class ArchiveManager {
    
    /**
     * Creates a binary header for the given document metadata
     * @param metadata The document metadata to serialize
     * @return Binary header as byte array
     */
    public static byte[] createBinaryHeader(DocumentMetadata metadata) {
        return metadata.serializeToBinaryHeader();
    }
    
    /**
     * Restores document metadata from a binary header
     * @param headerData The binary header data
     * @return A new DocumentMetadata object
     */
    public static DocumentMetadata restoreFromBinaryHeader(byte[] headerData) {
        DocumentMetadata metadata = new DocumentMetadata(0, 0L, false, 0);
        metadata.deserializeFromBinaryHeader(headerData);
        return metadata;
    }
    
    /**
     * Saves document metadata to a file with binary header
     * @param metadata The document metadata
     * @param filePath Path to save the header
     * @throws IOException If there's an error writing the file
     */
    public static void saveHeaderToFile(DocumentMetadata metadata, String filePath) throws IOException {
        byte[] headerData = createBinaryHeader(metadata);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(headerData);
        }
    }
    
    /**
     * Loads document metadata from a binary header file
     * @param filePath Path to the header file
     * @return The restored DocumentMetadata object
     * @throws IOException If there's an error reading the file
     */
    public static DocumentMetadata loadHeaderFromFile(String filePath) throws IOException {
        byte[] headerData = new byte[20]; // Fixed size header
        try (FileInputStream fis = new FileInputStream(filePath)) {
            int bytesRead = fis.read(headerData);
            if (bytesRead != 20) {
                throw new IOException("Invalid header file size");
            }
        }
        return restoreFromBinaryHeader(headerData);
    }
    
    /**
     * Prints header info in hex format for debugging
     * @param headerData The binary header data
     */
    public static void printHeaderHex(byte[] headerData) {
        System.out.print("Binary Header: ");
        for (byte b : headerData) {
            System.out.printf("%02X ", b & 0xFF);
        }
        System.out.println();
    }
}