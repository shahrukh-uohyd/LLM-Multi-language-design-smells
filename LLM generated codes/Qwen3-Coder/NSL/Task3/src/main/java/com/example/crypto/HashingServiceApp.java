// File: src/main/java/com/example/crypto/HashingServiceApp.java
package com.example.crypto;

import java.util.Map;
import java.util.Random;

/**
 * Main application demonstrating the JNI-based cryptographic hashing service
 */
public class HashingServiceApp {
    private HashingServer server;

    public HashingServiceApp() {
        this.server = new HashingServer();
    }

    /**
     * Demonstrates various hashing operations
     */
    public void demonstrateHashingOperations() {
        System.out.println("=== Cryptographic Hashing Service Demo ===\n");
        
        // Show service status
        showServiceStatus();
        
        // Sample data for hashing
        String testData = "Hello, World! This is a test message for cryptographic hashing.";
        String testKey = "secret-key-for-hmac";
        
        System.out.println("Input data: " + testData);
        System.out.println("Input key: " + testKey);
        System.out.println();
        
        // Test different hash algorithms
        String[] algorithms = {"SHA-256", "SHA-3", "MD5", "SHA-1", "BLAKE2B"};
        
        for (String algorithm : algorithms) {
            try {
                String hash = server.computeHash(algorithm, testData);
                System.out.printf("%-10s: %s\n", algorithm, hash);
                
                // Verify the hash
                boolean isValid = server.verifyHash(algorithm, testData, hash);
                System.out.printf("Verification: %s\n\n", isValid ? "PASS" : "FAIL");
            } catch (Exception e) {
                System.out.printf("%-10s: ERROR - %s\n\n", algorithm, e.getMessage());
            }
        }
        
        // Test HMAC operations
        System.out.println("=== HMAC Operations ===");
        String[] hmacAlgorithms = {"HMAC-SHA256", "SHA-256"}; // Second one should auto-convert to HMAC
        
        for (String algorithm : hmacAlgorithms) {
            try {
                String hmac = server.computeHmac(algorithm, testData, testKey);
                System.out.printf("%-15s: %s\n", algorithm, hmac);
            } catch (Exception e) {
                System.out.printf("%-15s: ERROR - %s\n", algorithm, e.getMessage());
            }
        }
        
        System.out.println();
    }

    /**
     * Demonstrates performance comparison between native and Java implementations
     */
    public void demonstratePerformance() {
        System.out.println("=== Performance Comparison ===");
        
        if (!server.isNativeLibraryAvailable()) {
            System.out.println("Native library not available - skipping performance tests");
            return;
        }
        
        // Generate large test data
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        String largeTestData = sb.toString();
        
        String algorithm = "SHA-256";
        int iterations = 1000;
        
        System.out.println("Testing performance with " + iterations + " iterations on " + 
                          largeTestData.length() + " bytes of data:");
        
        // Time native implementation
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            server.computeHash(algorithm, largeTestData);
        }
        long endTime = System.currentTimeMillis();
        
        long nativeTime = endTime - startTime;
        System.out.printf("Native implementation: %d ms (%.2f ms per operation)\n", 
                         nativeTime, (double) nativeTime / iterations);
        
        System.out.println("Performance test completed.\n");
    }

    /**
     * Shows service status information
     */
    public void showServiceStatus() {
        System.out.println("=== Service Status ===");
        Map<String, Object> status = server.getServiceStatus();
        
        System.out.println("Service Enabled: " + status.get("service_enabled"));
        System.out.println("Native Library Loaded: " + status.get("native_library_loaded"));
        System.out.println("Native Version: " + status.get("native_version"));
        System.out.println("Supported Algorithms: " + status.get("supported_algorithms"));
        System.out.println();
    }

    /**
     * Tests edge cases and error handling
     */
    public void testEdgeCases() {
        System.out.println("=== Edge Case Testing ===");
        
        try {
            // Test with empty string
            String emptyHash = server.computeHash("SHA-256", "");
            System.out.println("Empty string SHA-256: " + emptyHash);
            
            // Test with null data (should throw exception)
            try {
                server.computeHash("SHA-256", (String) null);
                System.out.println("ERROR: Should have thrown exception for null data");
            } catch (IllegalArgumentException e) {
                System.out.println("Correctly handled null data: " + e.getMessage());
            }
            
            // Test with unsupported algorithm (should throw exception)
            try {
                server.computeHash("INVALID-ALGO", "test");
                System.out.println("ERROR: Should have thrown exception for invalid algorithm");
            } catch (IllegalArgumentException e) {
                System.out.println("Correctly handled invalid algorithm: " + e.getMessage());
            }
            
            // Test verification with incorrect hash
            boolean result = server.verifyHash("SHA-256", "test", "invalidhash");
            System.out.println("Verification with invalid hash: " + result);
            
        } catch (Exception e) {
            System.out.println("Unexpected error in edge case testing: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Runs a comprehensive test suite
     */
    public void runComprehensiveTest() {
        System.out.println("=== Comprehensive Test Suite ===\n");
        
        demonstrateHashingOperations();
        demonstratePerformance();
        testEdgeCases();
        
        System.out.println("=== Test Suite Completed ===");
    }

    public static void main(String[] args) {
        System.out.println("JNI-Based Cryptographic Hashing Service");
        System.out.println("=========================================\n");
        
        HashingServiceApp app = new HashingServiceApp();
        
        // Run the comprehensive test suite
        app.runComprehensiveTest();
        
        System.out.println("Application completed successfully.");
    }
}