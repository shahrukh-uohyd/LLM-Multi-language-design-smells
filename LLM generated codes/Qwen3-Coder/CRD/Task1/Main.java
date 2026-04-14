// Main.java - Example usage
public class Main {
    public static void main(String[] args) {
        SecurityHandler securityHandler = new SecurityHandler();
        
        // Create some sample data
        String sensitiveData = "This is highly confidential information!";
        
        // Generate a random key
        byte[] key = securityHandler.generateRandomKey(32); // 256-bit key
        System.out.println("Generated key length: " + key.length);
        
        // Store encrypted data
        boolean stored = securityHandler.storeSensitiveData("encrypted_file.dat", sensitiveData, "mySecretPassword123");
        System.out.println("Data stored successfully: " + stored);
        
        // Retrieve and decrypt data
        String retrievedData = securityHandler.retrieveSensitiveData("encrypted_file.dat", "mySecretPassword123");
        System.out.println("Retrieved data: " + retrievedData);
        
        // Generate hash
        byte[] hash = securityHandler.generateSecureHash(sensitiveData.getBytes());
        System.out.println("SHA-256 Hash: " + FileSystemManager.bytesToHex(hash));
        
        // Test file system operations too
        FileSystemManager fsManager = new FileSystemManager();
        boolean fileCreated = fsManager.createFile("test.txt");
        System.out.println("File created: " + fileCreated);
        
        boolean written = fsManager.writeFile("test.txt", "Hello, World!".getBytes());
        System.out.println("File written: " + written);
        
        byte[] readData = fsManager.readFile("test.txt");
        System.out.println("File contents: " + new String(readData));
    }
}