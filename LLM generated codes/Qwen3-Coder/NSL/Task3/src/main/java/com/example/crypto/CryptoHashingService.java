// File: src/main/java/com/example/crypto/CryptoHashingService.java
package com.example.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * JNI-based cryptographic hashing service that computes hashes using native code.
 * This class provides a secure service API for cryptographic hash operations.
 */
public class CryptoHashingService {
    static {
        try {
            // Load the native cryptographic library securely
            System.loadLibrary("cryptohash");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native library 'cryptohash' not found: " + e.getMessage());
            System.err.println("Falling back to Java implementation for demonstration purposes.");
        }
    }

    /**
     * Computes SHA-256 hash using native implementation
     * @param data input data to hash
     * @return hexadecimal representation of the hash
     */
    public native String computeSha256(byte[] data);

    /**
     * Computes SHA-3 hash using native implementation
     * @param data input data to hash
     * @return hexadecimal representation of the hash
     */
    public native String computeSha3(byte[] data);

    /**
     * Computes MD5 hash using native implementation
     * @param data input data to hash
     * @return hexadecimal representation of the hash
     */
    public native String computeMd5(byte[] data);

    /**
     * Computes SHA-1 hash using native implementation
     * @param data input data to hash
     * @return hexadecimal representation of the hash
     */
    public native String computeSha1(byte[] data);

    /**
     * Computes BLAKE2b hash using native implementation
     * @param data input data to hash
     * @return hexadecimal representation of the hash
     */
    public native String computeBlake2b(byte[] data);

    /**
     * Computes HMAC-SHA256 using native implementation
     * @param data input data to hash
     * @param key secret key for HMAC
     * @return hexadecimal representation of the HMAC
     */
    public native String computeHmacSha256(byte[] data, byte[] key);

    /**
     * Checks if the native library is loaded and functional
     * @return true if native library is available
     */
    public native boolean isNativeLibraryLoaded();

    /**
     * Gets version information about the native hashing library
     * @return version string
     */
    public native String getNativeVersion();

    /**
     * Computes hash using native implementation with error handling
     * @param algorithm the hashing algorithm to use
     * @param data input data to hash
     * @return hexadecimal representation of the hash or null if error occurs
     */
    public String computeHash(String algorithm, byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }

        // Validate algorithm
        algorithm = algorithm.toUpperCase().trim();
        
        if (!isNativeLibraryLoaded()) {
            // Fallback to Java implementation if native library not loaded
            return computeHashJava(algorithm, data);
        }

        try {
            switch (algorithm) {
                case "SHA-256":
                    return computeSha256(data);
                case "SHA-3":
                    return computeSha3(data);
                case "MD5":
                    return computeMd5(data);
                case "SHA-1":
                    return computeSha1(data);
                case "BLAKE2B":
                    return computeBlake2b(data);
                default:
                    throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
            }
        } catch (Exception e) {
            System.err.println("Error computing hash with native library: " + e.getMessage());
            // Try Java fallback
            return computeHashJava(algorithm, data);
        }
    }

    /**
     * Computes hash using Java implementation as fallback
     * @param algorithm the hashing algorithm to use
     * @param data input data to hash
     * @return hexadecimal representation of the hash
     */
    private String computeHashJava(String algorithm, byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm.replace("-", ""));
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm not supported in Java: " + algorithm, e);
        }
    }

    /**
     * Computes HMAC using native implementation with error handling
     * @param algorithm the HMAC algorithm to use
     * @param data input data to hash
     * @param key secret key for HMAC
     * @return hexadecimal representation of the HMAC or null if error occurs
     */
    public String computeHmac(String algorithm, byte[] data, byte[] key) {
        if (data == null || key == null) {
            throw new IllegalArgumentException("Input data and key cannot be null");
        }

        algorithm = algorithm.toUpperCase().trim();

        if (!isNativeLibraryLoaded()) {
            // Fallback to Java implementation if native library not loaded
            return computeHmacJava(algorithm, data, key);
        }

        try {
            if ("HMAC-SHA256".equals(algorithm)) {
                return computeHmacSha256(data, key);
            } else {
                throw new IllegalArgumentException("Unsupported HMAC algorithm: " + algorithm);
            }
        } catch (Exception e) {
            System.err.println("Error computing HMAC with native library: " + e.getMessage());
            // Try Java fallback
            return computeHmacJava(algorithm, data, key);
        }
    }

    /**
     * Computes HMAC using Java implementation as fallback
     * @param algorithm the HMAC algorithm to use
     * @param data input data to hash
     * @param key secret key for HMAC
     * @return hexadecimal representation of the HMAC
     */
    private String computeHmacJava(String algorithm, byte[] data, byte[] key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(algorithm);
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key, algorithm);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data);
            return HexFormat.of().formatHex(hmacBytes).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed in Java: " + algorithm, e);
        }
    }

    /**
     * Verifies if a given hash matches the expected hash of the input data
     * @param algorithm the hashing algorithm to use
     * @param data input data to hash
     * @param expectedHash the expected hash to compare against
     * @return true if the computed hash matches the expected hash
     */
    public boolean verifyHash(String algorithm, byte[] data, String expectedHash) {
        String computedHash = computeHash(algorithm, data);
        return computedHash != null && computedHash.equalsIgnoreCase(expectedHash);
    }

    /**
     * Benchmarks the performance of native vs Java hashing
     * @param algorithm the hashing algorithm to benchmark
     * @param data the data to hash
     * @param iterations number of iterations to run
     * @return average time per operation in milliseconds
     */
    public double benchmarkNativePerformance(String algorithm, byte[] data, int iterations) {
        if (!isNativeLibraryLoaded()) {
            throw new IllegalStateException("Native library not loaded for benchmarking");
        }

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            computeHash(algorithm, data);
        }
        long endTime = System.nanoTime();

        return (double)(endTime - startTime) / iterations / 1_000_000; // Convert to milliseconds
    }
}