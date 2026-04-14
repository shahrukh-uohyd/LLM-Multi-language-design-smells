/**
 * Demo application showing usage of all three native features
 */
public class SecurityFeatureDemo {
    
    public static void main(String[] args) {
        SystemSecurityManager securityManager = new SystemSecurityManager();
        
        try {
            // Example 1: File checksum verification
            System.out.println("=== File Checksum Verification ===");
            String filePath = "example.txt";
            String checksum = securityManager.checksumCalculator.getFileChecksum(filePath);
            System.out.println("File checksum: " + checksum);
            
            // Example 2: Memory clearing
            System.out.println("\n=== Memory Clearing ===");
            byte[] sensitiveData = "This is sensitive data".getBytes();
            System.out.println("Before clearing: " + new String(sensitiveData));
            securityManager.clearSensitiveData(sensitiveData);
            System.out.println("After clearing: " + new String(sensitiveData));
            
            // Example 3: Hardware ID retrieval
            System.out.println("\n=== Hardware ID Retrieval ===");
            String hardwareId = securityManager.getSystemHardwareId();
            System.out.println("Hardware ID: " + hardwareId);
            
            // Example 4: System integrity verification
            System.out.println("\n=== System Integrity Check ===");
            boolean isValid = securityManager.verifySystemIntegrity(hardwareId);
            System.out.println("System integrity: " + (isValid ? "VALID" : "INVALID"));
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}