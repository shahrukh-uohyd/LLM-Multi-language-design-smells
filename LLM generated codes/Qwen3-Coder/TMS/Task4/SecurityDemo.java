/**
 * Demo application showing usage of security native features
 */
public class SecurityDemo {
    
    public static void main(String[] args) {
        SecurityManager securityManager = new SecurityManager();
        
        try {
            // Example 1: Hash generation
            System.out.println("=== SHA-256 Hash Generation ===");
            String testData = "Hello, World!";
            byte[] hash = securityManager.generateHash(testData);
            String hexHash = new CryptoHashGenerator().hashToHex(hash);
            System.out.println("Original: " + testData);
            System.out.println("SHA-256: " + hexHash);
            
            // Example 2: Random number generation
            System.out.println("\n=== Secure Random Generation ===");
            byte[] randomBytes = securityManager.generateSecureRandom(32);
            System.out.println("Generated " + randomBytes.length + " random bytes");
            System.out.println("First 10 bytes: " + java.util.Arrays.toString(
                java.util.Arrays.copyOf(randomBytes, Math.min(10, randomBytes.length))));
            
            // Example 3: RSA key pair generation
            System.out.println("\n=== RSA Key Pair Generation ===");
            RSAKeyPair keyPair = securityManager.generateKeyPair(2048);
            System.out.println("Generated RSA key pair:");
            System.out.println("Public key length: " + keyPair.getPublicKey().length + " bytes");
            System.out.println("Private key length: " + keyPair.getPrivateKey().length + " bytes");
            
            // Example 4: Digital signature creation and verification
            System.out.println("\n=== RSA Digital Signature ===");
            byte[] dataToSign = "Important document".getBytes();
            byte[] signature = securityManager.signWithGeneratedKeys(dataToSign, keyPair);
            System.out.println("Created signature of " + signature.length + " bytes");
            
            boolean isValid = securityManager.verifySignature(dataToSign, signature, keyPair.getPublicKey());
            System.out.println("Signature verification: " + (isValid ? "VALID" : "INVALID"));
            
            // Example 5: Tampered data verification
            byte[] tamperedData = "Tampered document".getBytes();
            boolean isTamperedValid = securityManager.verifySignature(tamperedData, signature, keyPair.getPublicKey());
            System.out.println("Verification with tampered data: " + (isTamperedValid ? "VALID" : "INVALID"));
            
        } catch (Exception e) {
            System.err.println("Security operation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}