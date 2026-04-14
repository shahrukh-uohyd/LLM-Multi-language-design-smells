// FileSystemManager.java
import java.io.*;
import java.nio.ByteBuffer;

public class FileSystemManager {
    static {
        System.loadLibrary("filesystem_crypto"); // Load the shared library containing both file system and crypto functions
    }

    // Existing native file system methods
    public native boolean createFile(String filename);
    public native byte[] readFile(String filename);
    public native boolean writeFile(String filename, byte[] data);
    public native boolean deleteFile(String filename);
    public native long getFileSize(String filename);

    // New cryptographic native methods
    public native byte[] encryptData(byte[] data, byte[] key, String algorithm);
    public native byte[] decryptData(byte[] encryptedData, byte[] key, String algorithm);
    public native byte[] generateHash(byte[] data, String algorithm);
    public native byte[] generateKey(int keyLength);
    public native boolean verifySignature(byte[] data, byte[] signature, byte[] publicKey);

    // Wrapper methods for convenience
    public byte[] encryptWithAES(byte[] data, byte[] key) throws Exception {
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalArgumentException("Invalid AES key length. Must be 16, 24, or 32 bytes.");
        }
        return encryptData(data, key, "AES");
    }

    public byte[] decryptWithAES(byte[] encryptedData, byte[] key) throws Exception {
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalArgumentException("Invalid AES key length. Must be 16, 24, or 32 bytes.");
        }
        return decryptData(encryptedData, key, "AES");
    }

    public byte[] hashSHA256(byte[] data) {
        return generateHash(data, "SHA-256");
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}