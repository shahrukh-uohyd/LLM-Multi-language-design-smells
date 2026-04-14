package com.example;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

/**
 * Demonstration of sensor data decoding capabilities.
 */
public class SensorDemo {
    
    private static final Random random = new Random(12345); // Deterministic for demo
    
    public static void main(String[] args) {
        SensorDecoder decoder = new SensorDecoder();
        
        System.out.println("=== JNI Sensor Data Decoder Demo ===\n");
        
        // Test 1: I2C Accelerometer/Gyroscope decoding
        System.out.println("Test 1: I2C Sensor Decoding (Accelerometer/Gyroscope)");
        byte[] i2cData = generateI2CSensorStream(10);
        SensorDecoder.CalibrationData calib = new SensorDecoder.CalibrationData(
            0.1, 0.2, 0.05,  // offsets
            1.02, 0.98, 1.01, // scales
            0.001              // temp coefficient
        );
        SensorDecoder.SensorReading[] i2cReadings = decoder.decodeI2CSensor(i2cData, calib);
        System.out.println("Decoded " + i2cReadings.length + " I2C readings:");
        for (int i = 0; i < Math.min(3, i2cReadings.length); i++) {
            System.out.println("  " + i2cReadings[i]);
        }
        if (i2cReadings.length > 3) {
            System.out.println("  ... (" + (i2cReadings.length - 3) + " more readings)");
        }
        System.out.println();
        
        // Test 2: SPI Pressure/Temperature decoding
        System.out.println("Test 2: SPI Sensor Decoding (Pressure/Temperature)");
        byte[] spiData = generateSPISensorStream(8, "pressure");
        SensorDecoder.SensorReading[] spiReadings = decoder.decodeSPISensor(spiData, "pressure");
        System.out.println("Decoded " + spiReadings.length + " SPI readings:");
        for (int i = 0; i < spiReadings.length; i++) {
            System.out.println("  " + spiReadings[i]);
        }
        System.out.println();
        
        // Test 3: UART GPS decoding
        System.out.println("Test 3: UART Sensor Decoding (GPS-like)");
        byte[] uartData = generateUARTSensorStream(5);
        SensorDecoder.SensorReading[] uartReadings = decoder.decodeUARTSensor(uartData, 
            new byte[]{(byte)0xAA, (byte)0x55}, new byte[]{(byte)0x55, (byte)0xAA});
        System.out.println("Decoded " + uartReadings.length + " UART readings:");
        for (int i = 0; i < uartReadings.length; i++) {
            System.out.println("  " + uartReadings[i]);
        }
        System.out.println();
        
        // Test 4: Batch decoding
        System.out.println("Test 4: Batch Decoding (Multi-sensor fusion)");
        byte[][] batchStreams = {
            generateI2CSensorStream(3),
            generateSPISensorStream(3, "temperature"),
            generateUARTSensorStream(2)
        };
        String[] streamTypes = {"i2c", "spi", "uart"};
        SensorDecoder.SensorReading[][] batchResults = decoder.batchDecode(batchStreams, streamTypes);
        
        for (int i = 0; i < batchResults.length; i++) {
            System.out.println("Stream " + i + " (" + streamTypes[i] + "): " + 
                             batchResults[i].length + " readings");
        }
        System.out.println();
        
        // Test 5: Data validation
        System.out.println("Test 5: Data Validation");
        byte[] validData = generateI2CSensorStream(1);
        int checksum = calculateChecksum(validData);
        boolean isValid = decoder.validateSensorData(validData, checksum);
        System.out.println("Valid data validation: " + (isValid ? "PASS" : "FAIL"));
        
        // Tamper with data
        byte[] tamperedData = validData.clone();
        tamperedData[10] ^= (byte)0xFF; // Flip some bits
        boolean isTamperedValid = decoder.validateSensorData(tamperedData, checksum);
        System.out.println("Tampered data validation: " + (isTamperedValid ? "PASS (unexpected)" : "FAIL (expected)"));
        
        // Test 6: Metadata extraction
        System.out.println("\nTest 6: Metadata Extraction");
        byte[] dataWithHeader = addMetadataHeader(generateI2CSensorStream(1));
        SensorDecoder.SensorMetadata metadata = decoder.extractMetadata(dataWithHeader);
        System.out.println("Extracted metadata: " + metadata);
        
        System.out.println("\n=== Demo Complete ===");
    }
    
    // Helper methods to generate realistic sensor data
    private static byte[] generateI2CSensorStream(int count) {
        ByteBuffer buffer = ByteBuffer.allocate(count * 14); // 8+2+2+2+1 = 15 per reading, rounded down
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        for (int i = 0; i < count; i++) {
            long ts = System.currentTimeMillis() + i * 100;
            short rawX = (short)(random.nextGaussian() * 1000); // Raw sensor value
            short rawY = (short)(random.nextGaussian() * 1000);
            short rawZ = (short)(random.nextGaussian() * 1000);
            byte checksum = (byte)(rawX + rawY + rawZ); // Simple checksum
            
            buffer.putLong(ts);
            buffer.putShort(rawX);
            buffer.putShort(rawY);
            buffer.putShort(rawZ);
            buffer.put(checksum);
        }
        
        return buffer.array();
    }
    
    private static byte[] generateSPISensorStream(int count, String sensorType) {
        ByteBuffer buffer = ByteBuffer.allocate(count * 15); // 1+8+4+1 = 14 per reading, rounded up
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        for (int i = 0; i < count; i++) {
            byte sensorId = sensorType.equals("pressure") ? (byte)0x01 : (byte)0x02;
            long ts = System.currentTimeMillis() + i * 50;
            float rawValue = (float)(sensorType.equals("pressure") ? 
                1013.25 + random.nextGaussian() * 10 : // Pressure in hPa
                25.0 + random.nextGaussian() * 5);     // Temperature in °C
            byte status = (byte)0x00; // OK status
            
            buffer.put(sensorId);
            buffer.putLong(ts);
            buffer.putFloat(rawValue);
            buffer.put(status);
        }
        
        return buffer.array();
    }
    
    private static byte[] generateUARTSensorStream(int count) {
        ByteBuffer buffer = ByteBuffer.allocate(count * 20); // Variable length frames
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        for (int i = 0; i < count; i++) {
            // Frame: START + TYPE + LEN + DATA + CHKSUM + END
            buffer.put((byte)0xAA); // Start
            buffer.put((byte)0x01); // Type (GPS position)
            buffer.put((byte)0x0C); // Length (12 bytes: lat+lon+alt)
            
            // GPS data: latitude, longitude, altitude
            double lat = 37.7749 + random.nextGaussian() * 0.01;
            double lon = -122.4194 + random.nextGaussian() * 0.01;
            float alt = 100.0f + (float)(random.nextGaussian() * 10);
            
            buffer.putDouble(lat);
            buffer.putDouble(lon);
            buffer.putFloat(alt);
            
            // Simple checksum
            byte checksum = (byte)(lat + lon + alt);
            buffer.put(checksum);
            buffer.put((byte)0x55); // End
            buffer.put((byte)0xAA);
        }
        
        return buffer.array();
    }
    
    private static int calculateChecksum(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += b & 0xFF;
        }
        return sum & 0xFFFF;
    }
    
    private static byte[] addMetadataHeader(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(17 + data.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        
        buffer.putInt(0x42534443); // "BSDC" - Binary Sensor Data Container
        buffer.put((byte)0x01);    // Version
        buffer.putLong(System.currentTimeMillis()); // Timestamp
        buffer.putInt(0x12345678); // Source ID
        buffer.put(data);
        
        return buffer.array();
    }
}