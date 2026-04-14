package com.example;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance sensor data decoder using JNI.
 * Supports multiple sensor protocols: I2C, SPI, UART, and custom binary formats.
 * Processes raw byte streams into calibrated sensor readings.
 */
public class SensorDecoder {
    
    // Load native library with comprehensive fallback strategy
    static {
        boolean loaded = false;
        String[] libNames = {
            "sensor_decoder",
            "libsensor_decoder.so",
            "sensor_decoder.dll",
            "libsensor_decoder.dylib"
        };
        
        for (String lib : libNames) {
            try {
                String cleanName = lib
                    .replace("lib", "")
                    .replace(".so", "")
                    .replace(".dylib", "")
                    .replace(".dll", "");
                System.loadLibrary(cleanName);
                loaded = true;
                break;
            } catch (UnsatisfiedLinkError e1) {
                try {
                    System.load("./lib/" + lib);
                    loaded = true;
                    break;
                } catch (UnsatisfiedLinkError e2) {
                    // Continue trying other options
                }
            }
        }
        
        if (!loaded) {
            throw new RuntimeException(
                "Failed to load native library 'sensor_decoder'. Ensure it's in java.library.path\n" +
                "Current path: " + System.getProperty("java.library.path")
            );
        }
    }

    // ===== NATIVE METHODS =====
    
    /**
     * Decode I2C sensor data stream (typically from accelerometer, gyroscope, magnetometer).
     * Protocol: [START][ADDR][DATA...][STOP]
     * Data format: [TIMESTAMP:8][RAW_X:2][RAW_Y:2][RAW_Z:2][CHECKSUM:1]
     *
     * @param rawData Raw I2C byte stream
     * @param calibration Calibration coefficients for each axis
     * @return Array of decoded sensor readings
     */
    public native SensorReading[] decodeI2CSensor(byte[] rawData, CalibrationData calibration);
    
    /**
     * Decode SPI sensor data stream (typically from pressure, temperature sensors).
     * Protocol: [SYNC_BYTE][CMD][DATA...][CRC]
     * Data format: [SENSOR_ID:1][TIMESTAMP:8][RAW_VALUE:4][STATUS:1]
     *
     * @param rawData Raw SPI byte stream
     * @param sensorType Type of sensor (temperature, pressure, etc.)
     * @return Array of decoded sensor readings
     */
    public native SensorReading[] decodeSPISensor(byte[] rawData, String sensorType);
    
    /**
     * Decode UART sensor data stream (typically from GPS, weather stations).
     * Protocol: Frame-based with start/end markers
     * Data format: [START:2][TYPE:1][LEN:1][DATA...][CHKSUM:1][END:2]
     *
     * @param rawData Raw UART byte stream
     * @param frameStart Start marker bytes
     * @param frameEnd End marker bytes
     * @return Array of decoded sensor readings
     */
    public native SensorReading[] decodeUARTSensor(byte[] rawData, byte[] frameStart, byte[] frameEnd);
    
    /**
     * Batch decode multiple sensor streams simultaneously.
     * Optimized for multi-sensor fusion applications.
     *
     * @param sensorStreams Array of sensor data streams
     * @param streamTypes Types corresponding to each stream
     * @return Array of arrays containing decoded readings per stream
     */
    public native SensorReading[][] batchDecode(byte[][] sensorStreams, String[] streamTypes);
    
    /**
     * Validate sensor data integrity using checksums and sequence numbers.
     *
     * @param rawData Raw sensor data
     * @param expectedChecksum Expected checksum value
     * @return true if data is valid, false otherwise
     */
    public native boolean validateSensorData(byte[] rawData, int expectedChecksum);
    
    /**
     * Extract metadata from sensor stream header.
     * Header format: [MAGIC:4][VERSION:1][TIMESTAMP:8][SOURCE_ID:4]
     *
     * @param rawData Raw sensor data with header
     * @return Metadata about the sensor stream
     */
    public native SensorMetadata extractMetadata(byte[] rawData);

    // ===== SUPPORTING CLASSES =====
    
    public static class SensorReading {
        public final long timestamp;      // Unix timestamp in milliseconds
        public final double x;            // X-axis value (or primary value)
        public final double y;            // Y-axis value (or secondary value)
        public final double z;            // Z-axis value (or tertiary value)
        public final double temperature;  // Temperature compensation value
        public final int status;          // Status flags
        public final String sensorId;     // Unique sensor identifier
        
        public SensorReading(long timestamp, double x, double y, double z, 
                           double temperature, int status, String sensorId) {
            this.timestamp = timestamp;
            this.x = x;
            this.y = y;
            this.z = z;
            this.temperature = temperature;
            this.status = status;
            this.sensorId = sensorId;
        }
        
        @Override
        public String toString() {
            return String.format(
                "SensorReading{ts=%d, x=%.2f, y=%.2f, z=%.2f, temp=%.1f°C, status=0x%02X, id='%s'}",
                timestamp, x, y, z, temperature, status, sensorId
            );
        }
    }
    
    public static class CalibrationData {
        public final double offsetX, offsetY, offsetZ;
        public final double scaleX, scaleY, scaleZ;
        public final double temperatureCoefficient;
        
        public CalibrationData(double offsetX, double offsetY, double offsetZ,
                              double scaleX, double scaleY, double scaleZ,
                              double temperatureCoefficient) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.temperatureCoefficient = temperatureCoefficient;
        }
    }
    
    public static class SensorMetadata {
        public final int magic;
        public final byte version;
        public final long timestamp;
        public final int sourceId;
        public final int dataSize;
        
        public SensorMetadata(int magic, byte version, long timestamp, int sourceId, int dataSize) {
            this.magic = magic;
            this.version = version;
            this.timestamp = timestamp;
            this.sourceId = sourceId;
            this.dataSize = dataSize;
        }
        
        @Override
        public String toString() {
            return String.format(
                "SensorMetadata{magic=0x%08X, version=%d, timestamp=%d, sourceId=%d, dataSize=%d}",
                magic, version, timestamp, sourceId, dataSize
            );
        }
    }
}