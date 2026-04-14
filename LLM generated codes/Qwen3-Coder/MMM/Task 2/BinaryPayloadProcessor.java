package com.example;

/**
 * Binary payload processor using JNI for high-performance operations:
 * - CRC32 checksum calculation
 * - XOR-based obfuscation/deobfuscation
 * - Run-length encoding (RLE) compression/decompression
 * - Payload validation with metadata extraction
 */
public class BinaryPayloadProcessor {
    
    // Load native library with fallback paths
    static {
        boolean loaded = false;
        String[] libNames = {
            "payload_processor",          // Standard name
            "libpayload_processor.so",    // Linux explicit
            "payload_processor.dll",      // Windows explicit
            "libpayload_processor.dylib"  // macOS explicit
        };
        
        for (String lib : libNames) {
            try {
                System.loadLibrary(lib.replace("lib", "").replace(".so", "").replace(".dylib", "").replace(".dll", ""));
                loaded = true;
                break;
            } catch (UnsatisfiedLinkError e) {
                try {
                    System.load("./lib/" + lib);
                    loaded = true;
                    break;
                } catch (Exception ignored) {}
            }
        }
        
        if (!loaded) {
            throw new RuntimeException("Failed to load native library. Ensure libpayload_processor is in java.library.path");
        }
    }

    // ===== NATIVE METHODS =====
    
    /**
     * Calculate CRC32 checksum of binary payload
     * @param payload Binary data to checksum
     * @return 32-bit CRC checksum
     */
    public native int calculateCRC32(byte[] payload);
    
    /**
     * Obfuscate payload using XOR with key
     * @param payload Original binary data
     * @param key Single-byte XOR key
     * @return Obfuscated byte array
     */
    public native byte[] obfuscateXOR(byte[] payload, byte key);
    
    /**
     * Deobfuscate payload using XOR with key
     * @param payload Obfuscated binary data
     * @param key Single-byte XOR key
     * @return Original byte array
     */
    public native byte[] deobfuscateXOR(byte[] payload, byte key);
    
    /**
     * Compress payload using Run-Length Encoding (RLE)
     * Format: [byte][count] pairs for repeated sequences
     * @param payload Uncompressed data
     * @return Compressed byte array
     */
    public native byte[] compressRLE(byte[] payload);
    
    /**
     * Decompress RLE-encoded payload
     * @param compressed RLE-compressed data
     * @return Original uncompressed byte array
     */
    public native byte[] decompressRLE(byte[] compressed);
    
    /**
     * Validate payload structure and extract metadata
     * Expected format: [MAGIC:4][LENGTH:4][PAYLOAD...]
     * @param payload Binary data with header
     * @return Validation result object
     */
    public native ValidationResult validatePayload(byte[] payload);

    // ===== JAVA FALLBACK IMPLEMENTATIONS (for resilience) =====
    
    public int calculateCRC32Java(byte[] payload) {
        if (payload == null) return 0;
        int crc = 0xFFFFFFFF;
        for (byte b : payload) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                crc = (crc >>> 1) ^ (0xEDB88320 & -(crc & 1));
            }
        }
        return ~crc;
    }
    
    public byte[] obfuscateXORJava(byte[] payload, byte key) {
        if (payload == null) return null;
        byte[] result = new byte[payload.length];
        for (int i = 0; i < payload.length; i++) {
            result[i] = (byte) (payload[i] ^ key);
        }
        return result;
    }

    // ===== VALIDATION RESULT CLASS =====
    
    public static class ValidationResult {
        public final boolean isValid;
        public final int payloadLength;
        public final int headerVersion;
        public final long timestamp;
        
        public ValidationResult(boolean isValid, int payloadLength, int headerVersion, long timestamp) {
            this.isValid = isValid;
            this.payloadLength = payloadLength;
            this.headerVersion = headerVersion;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%b, length=%d, version=%d, timestamp=%d}",
                isValid, payloadLength, headerVersion, timestamp);
        }
    }
}