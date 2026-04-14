package com.example;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/**
 * Demonstration of binary payload processing pipeline:
 * 1. Generate structured payload with header
 * 2. Calculate checksum
 * 3. Obfuscate payload
 * 4. Compress payload
 * 5. Reverse the process (decompress → deobfuscate → validate)
 */
public class PayloadDemo {
    
    private static final int MAGIC_HEADER = 0xCAFEBABE;
    private static final byte XOR_KEY = (byte) 0xA5;
    
    public static void main(String[] args) {
        BinaryPayloadProcessor processor = new BinaryPayloadProcessor();
        Random random = new Random(42); // Deterministic for demo
        
        System.out.println("=== Binary Payload Processing Demo ===\n");
        
        // Step 1: Create structured payload with header
        String message = "Secure payload transmission with JNI processing";
        byte[] payloadBody = message.getBytes(StandardCharsets.UTF_8);
        
        // Build payload with header: [MAGIC:4][LENGTH:4][TIMESTAMP:8][BODY...]
        ByteBuffer fullPayload = ByteBuffer.allocate(16 + payloadBody.length);
        fullPayload.putInt(MAGIC_HEADER);           // Magic number
        fullPayload.putInt(payloadBody.length);     // Payload length
        fullPayload.putLong(System.currentTimeMillis()); // Timestamp
        fullPayload.put(payloadBody);
        
        byte[] originalPayload = fullPayload.array();
        
        System.out.println("Original payload size: " + originalPayload.length + " bytes");
        System.out.println("Original content: \"" + message + "\"\n");
        
        // Step 2: Calculate CRC32 checksum
        int crc = processor.calculateCRC32(originalPayload);
        System.out.printf("CRC32 checksum: 0x%08X\n\n", crc);
        
        // Step 3: Obfuscate payload
        byte[] obfuscated = processor.obfuscateXOR(originalPayload, XOR_KEY);
        System.out.println("Obfuscated payload size: " + obfuscated.length + " bytes");
        System.out.println("First 16 obfuscated bytes: " + bytesToHex(Arrays.copyOf(obfuscated, 16)) + "\n");
        
        // Step 4: Compress obfuscated payload
        byte[] compressed = processor.compressRLE(obfuscated);
        System.out.println("Compressed payload size: " + compressed.length + " bytes");
        System.out.printf("Compression ratio: %.2f%%\n\n", 
            (compressed.length * 100.0) / obfuscated.length);
        
        // Step 5: Reverse the process - decompress
        byte[] decompressed = processor.decompressRLE(compressed);
        System.out.println("Decompressed payload size: " + decompressed.length + " bytes");
        System.out.println("Decompression successful: " + Arrays.equals(obfuscated, decompressed) + "\n");
        
        // Step 6: Deobfuscate
        byte[] restored = processor.deobfuscateXOR(decompressed, XOR_KEY);
        System.out.println("Restored payload size: " + restored.length + " bytes");
        System.out.println("Restoration successful: " + Arrays.equals(originalPayload, restored) + "\n");
        
        // Step 7: Validate payload structure
        BinaryPayloadProcessor.ValidationResult result = processor.validatePayload(restored);
        System.out.println("Validation result: " + result);
        System.out.println("Payload valid: " + result.isValid);
        
        if (result.isValid && restored.length >= 16) {
            // Extract and display original message
            byte[] extractedBody = Arrays.copyOfRange(restored, 16, restored.length);
            String extractedMessage = new String(extractedBody, StandardCharsets.UTF_8);
            System.out.println("Extracted message: \"" + extractedMessage + "\"");
        }
        
        // Step 8: Edge case testing
        System.out.println("\n=== Edge Case Testing ===");
        testEdgeCases(processor);
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    private static void testEdgeCases(BinaryPayloadProcessor processor) {
        // Null payload
        try {
            System.out.println("Null payload CRC: " + processor.calculateCRC32(null));
        } catch (Exception e) {
            System.out.println("Null payload handled: " + e.getClass().getSimpleName());
        }
        
        // Empty payload
        byte[] empty = new byte[0];
        System.out.println("Empty payload CRC: 0x" + Integer.toHexString(processor.calculateCRC32(empty)));
        System.out.println("Empty payload obfuscation length: " + processor.obfuscateXOR(empty, (byte)0x55).length);
        
        // Maximum byte value payload
        byte[] maxValues = new byte[256];
        for (int i = 0; i < 256; i++) maxValues[i] = (byte)i;
        byte[] obfuscatedMax = processor.obfuscateXOR(maxValues, (byte)0xFF);
        System.out.println("Full byte range obfuscation test: " + 
            (Arrays.equals(maxValues, processor.deobfuscateXOR(obfuscatedMax, (byte)0xFF)) ? "PASS" : "FAIL"));
        
        // RLE compression edge cases
        byte[] repeated = new byte[100];
        Arrays.fill(repeated, (byte)0xAA);
        byte[] compressed = processor.compressRLE(repeated);
        System.out.println("RLE compression of 100 identical bytes: " + 
            compressed.length + " bytes (expected <= 200)");
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}