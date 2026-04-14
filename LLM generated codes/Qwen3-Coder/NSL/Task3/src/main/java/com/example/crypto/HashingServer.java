// File: src/main/java/com/example/crypto/HashingServer.java
package com.example.crypto;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service layer that provides a REST-like API for cryptographic hashing operations.
 * This class manages the cryptographic hashing service and handles concurrent requests.
 */
public class HashingServer {
    private static final Logger logger = Logger.getLogger(HashingServer.class.getName());
    
    private final CryptoHashingService cryptoService;
    private final Map<String, String> algorithmMap;
    private final boolean serviceEnabled;

    public HashingServer() {
        this.cryptoService = new CryptoHashingService();
        this.algorithmMap = initializeAlgorithmMap();
        
        // Check if native library is available
        this.serviceEnabled = cryptoService.isNativeLibraryLoaded();
        
        if (serviceEnabled) {
            logger.info("Cryptographic hashing service initialized with native library support");
            logger.info("Native library version: " + cryptoService.getNativeVersion());
        } else {
            logger.warning("Cryptographic hashing service running in Java fallback mode");
        }
    }

    /**
     * Initializes the mapping of algorithm names to their standardized forms
     */
    private Map<String, String> initializeAlgorithmMap() {
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put("SHA-256", "SHA-256");
        map.put("SHA256", "SHA-256");
        map.put("SHA2", "SHA-256");
        map.put("SHA-3", "SHA-3");
        map.put("SHA3", "SHA-3");
        map.put("MD5", "MD5");
        map.put("SHA-1", "SHA-1");
        map.put("SHA1", "SHA-1");
        map.put("BLAKE2B", "BLAKE2B");
        return map;
    }

    /**
     * Computes a hash for the provided data using the specified algorithm
     * @param algorithm the hashing algorithm to use
     * @param data the input data to hash
     * @return the computed hash in hexadecimal format
     */
    public String computeHash(String algorithm, String data) {
        validateInputs(algorithm, data);
        
        String normalizedAlgorithm = normalizeAlgorithm(algorithm);
        byte[] dataBytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        logger.log(Level.INFO, "Computing hash for algorithm: {0}, data length: {1}", 
                  new Object[]{normalizedAlgorithm, dataBytes.length});
        
        String result = cryptoService.computeHash(normalizedAlgorithm, dataBytes);
        
        logger.log(Level.INFO, "Hash computed successfully for algorithm: {0}", normalizedAlgorithm);
        return result;
    }

    /**
     * Computes a hash for the provided binary data using the specified algorithm
     * @param algorithm the hashing algorithm to use
     * @param data the input binary data to hash
     * @return the computed hash in hexadecimal format
     */
    public String computeHash(String algorithm, byte[] data) {
        validateInputs(algorithm, data);
        
        String normalizedAlgorithm = normalizeAlgorithm(algorithm);
        
        logger.log(Level.INFO, "Computing hash for algorithm: {0}, data length: {1}", 
                  new Object[]{normalizedAlgorithm, data.length});
        
        String result = cryptoService.computeHash(normalizedAlgorithm, data);
        
        logger.log(Level.INFO, "Hash computed successfully for algorithm: {0}", normalizedAlgorithm);
        return result;
    }

    /**
     * Computes HMAC for the provided data using the specified algorithm and key
     * @param algorithm the HMAC algorithm to use
     * @param data the input data to hash
     * @param key the secret key for HMAC
     * @return the computed HMAC in hexadecimal format
     */
    public String computeHmac(String algorithm, String data, String key) {
        validateInputs(algorithm, data, key);
        
        String normalizedAlgorithm = normalizeAlgorithmForHmac(algorithm);
        byte[] dataBytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        logger.log(Level.INFO, "Computing HMAC for algorithm: {0}, data length: {1}, key length: {2}", 
                  new Object[]{normalizedAlgorithm, dataBytes.length, keyBytes.length});
        
        String result = cryptoService.computeHmac(normalizedAlgorithm, dataBytes, keyBytes);
        
        logger.log(Level.INFO, "HMAC computed successfully for algorithm: {0}", normalizedAlgorithm);
        return result;
    }

    /**
     * Verifies if the provided hash matches the expected hash of the input data
     * @param algorithm the hashing algorithm to use
     * @param data the input data to hash
     * @param expectedHash the expected hash to compare against
     * @return true if the computed hash matches the expected hash
     */
    public boolean verifyHash(String algorithm, String data, String expectedHash) {
        validateInputs(algorithm, data, expectedHash);
        
        String normalizedAlgorithm = normalizeAlgorithm(algorithm);
        byte[] dataBytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        logger.log(Level.INFO, "Verifying hash for algorithm: {0}, data length: {1}", 
                  new Object[]{normalizedAlgorithm, dataBytes.length});
        
        boolean result = cryptoService.verifyHash(normalizedAlgorithm, dataBytes, expectedHash);
        
        logger.log(Level.INFO, "Hash verification completed for algorithm: {0}, result: {1}", 
                  new Object[]{normalizedAlgorithm, result});
        return result;
    }

    /**
     * Gets the status of the hashing service
     * @return a map containing service status information
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        
        status.put("service_enabled", serviceEnabled);
        status.put("native_library_loaded", cryptoService.isNativeLibraryLoaded());
        status.put("native_version", cryptoService.getNativeVersion());
        status.put("supported_algorithms", algorithmMap.keySet());
        
        return status;
    }

    /**
     * Normalizes the algorithm name to its standard form
     * @param algorithm the algorithm name to normalize
     * @return the normalized algorithm name
     */
    private String normalizeAlgorithm(String algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        String normalized = algorithmMap.get(algorithm.toUpperCase());
        if (normalized == null) {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        
        return normalized;
    }

    /**
     * Normalizes the HMAC algorithm name to its standard form
     * @param algorithm the algorithm name to normalize
     * @return the normalized HMAC algorithm name
     */
    private String normalizeAlgorithmForHmac(String algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm cannot be null");
        }
        
        String upperAlgorithm = algorithm.toUpperCase();
        if (upperAlgorithm.startsWith("HMAC-")) {
            return upperAlgorithm;
        } else {
            // If it's just the hash algorithm, prepend HMAC-
            String hashAlgorithm = normalizeAlgorithm(algorithm);
            return "HMAC-" + hashAlgorithm;
        }
    }

    /**
     * Validates input parameters
     * @param algorithm the algorithm to validate
     * @param data the data to validate
     */
    private void validateInputs(String algorithm, String data) {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            throw new IllegalArgumentException("Algorithm cannot be null or empty");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
    }

    /**
     * Validates input parameters including a key
     * @param algorithm the algorithm to validate
     * @param data the data to validate
     * @param key the key to validate
     */
    private void validateInputs(String algorithm, String data, String key) {
        validateInputs(algorithm, data);
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
    }

    /**
     * Validates input parameters including expected hash
     * @param algorithm the algorithm to validate
     * @param data the data to validate
     * @param expectedHash the expected hash to validate
     */
    private void validateInputs(String algorithm, String data, String expectedHash) {
        validateInputs(algorithm, data);
        if (expectedHash == null || expectedHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Expected hash cannot be null or empty");
        }
        
        // Validate hex format of expected hash
        try {
            java.util.HexFormat.of().parseHex(expectedHash);
        } catch (Exception e) {
            throw new IllegalArgumentException("Expected hash is not a valid hexadecimal string");
        }
    }

    /**
     * Gets whether the native library is available
     * @return true if native library is loaded and available
     */
    public boolean isNativeLibraryAvailable() {
        return serviceEnabled;
    }
}