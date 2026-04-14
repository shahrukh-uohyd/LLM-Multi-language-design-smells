// DiagnosticLogger.java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DiagnosticLogger {
    static {
        System.loadLibrary("diagnostic_crypto");
    }

    // Existing native diagnostic methods
    public native void logDiagnosticMessage(String message);
    public native String getSystemDiagnostics();
    public native boolean writeLogToFile(String filename, String message);
    public native String getProcessInfo();
    public native long getMemoryUsage();
    public native String getSystemTime();
    public native boolean flushLogs();

    // New encryption/decryption native methods
    public native byte[] encryptData(byte[] data, byte[] key, String algorithm);
    public native byte[] decryptData(byte[] encryptedData, byte[] key, String algorithm);
    public native byte[] generateKey(int keyLength);
    public native byte[] hashData(byte[] data, String algorithm);
    public native boolean verifyHash(byte[] data, byte[] expectedHash, String algorithm);
    public native byte[] signData(byte[] data, byte[] privateKey);
    public native boolean verifySignature(byte[] data, byte[] signature, byte[] publicKey);

    // Convenience methods for encryption
    public byte[] encryptAES(byte[] data, byte[] key) {
        return encryptData(data, key, "AES");
    }

    public byte[] decryptAES(byte[] encryptedData, byte[] key) {
        return decryptData(encryptedData, key, "AES");
    }

    public byte[] encryptRSA(byte[] data, byte[] key) {
        return encryptData(data, key, "RSA");
    }

    public byte[] decryptRSA(byte[] encryptedData, byte[] key) {
        return decryptData(encryptedData, key, "RSA");
    }

    public byte[] hashSHA256(byte[] data) {
        return hashData(data, "SHA-256");
    }

    public byte[] hashSHA512(byte[] data) {
        return hashData(data, "SHA-512");
    }

    // Logging with encryption
    public boolean logEncryptedMessage(String message, byte[] key) {
        try {
            byte[] encryptedMessage = encryptAES(message.getBytes(), key);
            logDiagnosticMessage("ENCRYPTED:" + bytesToHex(encryptedMessage));
            return true;
        } catch (Exception e) {
            System.err.println("Failed to log encrypted message: " + e.getMessage());
            return false;
        }
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

    // Enhanced diagnostic methods
    public void logDiagnosticWithTimestamp(String message) {
        String timestampedMessage = "[" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "] " + message;
        logDiagnosticMessage(timestampedMessage);
    }

    public String getDetailedSystemDiagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== System Diagnostics ===\n");
        sb.append("System Info: ").append(getSystemDiagnostics()).append("\n");
        sb.append("Process Info: ").append(getProcessInfo()).append("\n");
        sb.append("Memory Usage: ").append(getMemoryUsage()).append(" bytes\n");
        sb.append("System Time: ").append(getSystemTime()).append("\n");
        return sb.toString();
    }
}