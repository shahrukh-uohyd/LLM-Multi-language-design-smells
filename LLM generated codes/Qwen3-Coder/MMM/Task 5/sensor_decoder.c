#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stdbool.h>
#include <math.h>
#include "com_example_SensorDecoder.h"
#include "com_example_SensorDecoder_SensorReading.h"
#include "com_example_SensorDecoder_CalibrationData.h"
#include "com_example_SensorDecoder_SensorMetadata.h"

// Helper functions
static inline uint16_t le_read_u16(const uint8_t *data) {
    return (uint16_t)data[0] | ((uint16_t)data[1] << 8);
}

static inline uint32_t le_read_u32(const uint8_t *data) {
    return (uint32_t)data[0] | ((uint32_t)data[1] << 8) | 
           ((uint32_t)data[2] << 16) | ((uint32_t)data[3] << 24);
}

static inline uint64_t le_read_u64(const uint8_t *data) {
    return (uint64_t)data[0] | ((uint64_t)data[1] << 8) | 
           ((uint64_t)data[2] << 16) | ((uint64_t)data[3] << 24) |
           ((uint64_t)data[4] << 32) | ((uint64_t)data[5] << 40) |
           ((uint64_t)data[6] << 48) | ((uint64_t)data[7] << 56);
}

static inline uint32_t be_read_u32(const uint8_t *data) {
    return ((uint32_t)data[0] << 24) | ((uint32_t)data[1] << 16) | 
           ((uint32_t)data[2] << 8) | (uint32_t)data[3];
}

static inline uint64_t be_read_u64(const uint8_t *data) {
    return ((uint64_t)data[0] << 56) | ((uint64_t)data[1] << 48) | 
           ((uint64_t)data[2] << 40) | ((uint64_t)data[3] << 32) |
           ((uint64_t)data[4] << 24) | ((uint64_t)data[5] << 16) |
           ((uint64_t)data[6] << 8) | (uint64_t)data[7];
}

static inline float be_read_float(const uint8_t *data) {
    union { uint32_t i; float f; } u;
    u.i = be_read_u32(data);
    return u.f;
}

static inline double le_read_double(const uint8_t *data) {
    union { uint64_t i; double f; } u;
    u.i = le_read_u64(data);
    return u.f;
}

// ===== I2C SENSOR DECODER =====

static jobjectArray decode_i2c_sensor(JNIEnv *env, const uint8_t *data, size_t len, 
                                     double offset_x, double offset_y, double offset_z,
                                     double scale_x, double scale_y, double scale_z,
                                     double temp_coeff) {
    
    if (len < 15) return NULL; // Minimum packet size
    
    // Calculate how many complete packets we have
    size_t packet_size = 15; // 8 timestamp + 2x3 data + 1 checksum
    size_t num_packets = len / packet_size;
    
    if (num_packets == 0) return NULL;
    
    // Create array of SensorReading objects
    jclass readingClass = (*env)->FindClass(env, "com/example/SensorDecoder$SensorReading");
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    if (!readingClass || !stringClass) return NULL;
    
    jobjectArray result = (*env)->NewObjectArray(env, (jsize)num_packets, readingClass, NULL);
    if (!result) return NULL;
    
    jmethodID readingConstructor = (*env)->GetMethodID(env, readingClass, "<init>", 
        "(DJDDDILjava/lang/String;)V");
    if (!readingConstructor) return NULL;
    
    jstring sensorId = (*env)->NewStringUTF(env, "I2C_SENSOR");
    if (!sensorId) return NULL;
    
    const uint8_t *ptr = data;
    for (size_t i = 0; i < num_packets; i++) {
        // Validate checksum
        uint8_t checksum = 0;
        for (int j = 0; j < 13; j++) { // Sum first 13 bytes
            checksum += ptr[j];
        }
        if (checksum != ptr[13]) {
            // Skip corrupted packet, continue processing
            ptr += packet_size;
            continue;
        }
        
        uint64_t timestamp = le_read_u64(ptr); ptr += 8;
        int16_t raw_x = (int16_t)le_read_u16(ptr); ptr += 2;
        int16_t raw_y = (int16_t)le_read_u16(ptr); ptr += 2;
        int16_t raw_z = (int16_t)le_read_u16(ptr); ptr += 2;
        uint8_t status = ptr[0]; ptr += 1;
        
        // Apply calibration
        double calibrated_x = (raw_x * scale_x) + offset_x;
        double calibrated_y = (raw_y * scale_y) + offset_y;
        double calibrated_z = (raw_z * scale_z) + offset_z;
        
        // Calculate temperature compensation (simplified model)
        double temperature = 25.0 + (calibrated_x + calibrated_y + calibrated_z) * temp_coeff;
        
        jobject reading = (*env)->NewObject(env, readingClass, readingConstructor,
            (jlong)timestamp, calibrated_x, calibrated_y, calibrated_z, temperature, (jint)status, sensorId);
        
        if (reading) {
            (*env)->SetObjectArrayElement(env, result, (jsize)i, reading);
        }
    }
    
    return result;
}

// ===== SPI SENSOR DECODER =====

static jobjectArray decode_spi_sensor(JNIEnv *env, const uint8_t *data, size_t len, const char *sensor_type) {
    
    if (len < 14) return NULL; // Minimum packet size
    
    // Calculate number of complete packets
    size_t packet_size = 14; // 1 sensor_id + 8 timestamp + 4 value + 1 status
    size_t num_packets = len / packet_size;
    
    if (num_packets == 0) return NULL;
    
    jclass readingClass = (*env)->FindClass(env, "com/example/SensorDecoder$SensorReading");
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    if (!readingClass || !stringClass) return NULL;
    
    jobjectArray result = (*env)->NewObjectArray(env, (jsize)num_packets, readingClass, NULL);
    if (!result) return NULL;
    
    jmethodID readingConstructor = (*env)->GetMethodID(env, readingClass, "<init>", 
        "(DJDDDILjava/lang/String;)V");
    if (!readingConstructor) return NULL;
    
    jstring sensorId = (*env)->NewStringUTF(env, sensor_type);
    if (!sensorId) return NULL;
    
    const uint8_t *ptr = data;
    for (size_t i = 0; i < num_packets; i++) {
        uint8_t sensor_id = ptr[0]; ptr += 1;
        uint64_t timestamp = be_read_u64(ptr); ptr += 8;
        float raw_value = be_read_float(ptr); ptr += 4;
        uint8_t status = ptr[0]; ptr += 1;
        
        // Convert raw value based on sensor type
        double x, y, z, temp;
        if (strcmp(sensor_type, "pressure") == 0) {
            x = raw_value;  // Pressure in hPa
            y = 0.0;
            z = 0.0;
            temp = 20.0 + (raw_value - 1013.25) * 0.01; // Estimate temperature from pressure
        } else if (strcmp(sensor_type, "temperature") == 0) {
            x = raw_value;  // Temperature in °C
            y = 0.0;
            z = 0.0;
            temp = raw_value;
        } else {
            x = y = z = 0.0;
            temp = 25.0;
        }
        
        jobject reading = (*env)->NewObject(env, readingClass, readingConstructor,
            (jlong)timestamp, x, y, z, temp, (jint)status, sensorId);
        
        if (reading) {
            (*env)->SetObjectArrayElement(env, result, (jsize)i, reading);
        }
    }
    
    return result;
}

// ===== UART SENSOR DECODER =====

static jobjectArray decode_uart_sensor(JNIEnv *env, const uint8_t *data, size_t len,
                                      const uint8_t *frame_start, size_t start_len,
                                      const uint8_t *frame_end, size_t end_len) {
    
    if (len < start_len + end_len + 3) return NULL; // Minimum frame size
    
    // Count valid frames first
    size_t frame_count = 0;
    const uint8_t *ptr = data;
    const uint8_t *end = data + len;
    
    while (ptr < end) {
        // Look for start marker
        const uint8_t *frame_start_pos = memchr(ptr, frame_start[0], end - ptr);
        if (!frame_start_pos) break;
        
        // Verify full start marker
        if (frame_start_pos + start_len > end || 
            memcmp(frame_start_pos, frame_start, start_len) != 0) {
            ptr = frame_start_pos + 1;
            continue;
        }
        
        frame_start_pos += start_len; // Move past start marker
        
        if (frame_start_pos >= end) break;
        
        uint8_t frame_type = frame_start_pos[0];
        frame_start_pos += 1;
        
        if (frame_start_pos >= end) break;
        
        uint8_t frame_len = frame_start_pos[0];
        frame_start_pos += 1;
        
        if (frame_start_pos + frame_len + 1 + end_len > end) break; // Not enough data for frame
        
        // Look for end marker
        if (memcmp(frame_start_pos + frame_len + 1, frame_end, end_len) != 0) {
            ptr = frame_start_pos;
            continue;
        }
        
        frame_count++;
        ptr = frame_start_pos + frame_len + 1 + end_len; // Move past frame
    }
    
    if (frame_count == 0) return NULL;
    
    // Create result array
    jclass readingClass = (*env)->FindClass(env, "com/example/SensorDecoder$SensorReading");
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    if (!readingClass || !stringClass) return NULL;
    
    jobjectArray result = (*env)->NewObjectArray(env, (jsize)frame_count, readingClass, NULL);
    if (!result) return NULL;
    
    jmethodID readingConstructor = (*env)->GetMethodID(env, readingClass, "<init>", 
        "(DJDDDILjava/lang/String;)V");
    if (!readingConstructor) return NULL;
    
    jstring sensorId = (*env)->NewStringUTF(env, "UART_SENSOR");
    if (!sensorId) return NULL;
    
    // Process frames again
    ptr = data;
    size_t result_idx = 0;
    
    while (ptr < end && result_idx < frame_count) {
        // Find start marker
        const uint8_t *frame_start_pos = memchr(ptr, frame_start[0], end - ptr);
        if (!frame_start_pos) break;
        
        if (frame_start_pos + start_len > end || 
            memcmp(frame_start_pos, frame_start, start_len) != 0) {
            ptr = frame_start_pos + 1;
            continue;
        }
        
        frame_start_pos += start_len;
        
        uint8_t frame_type = frame_start_pos[0];
        frame_start_pos += 1;
        
        uint8_t frame_len = frame_start_pos[0];
        frame_start_pos += 1;
        
        if (frame_start_pos + frame_len + 1 + end_len > end) break;
        
        // Verify end marker
        if (memcmp(frame_start_pos + frame_len + 1, frame_end, end_len) != 0) {
            ptr = frame_start_pos;
            continue;
        }
        
        // Calculate checksum
        uint8_t checksum = 0;
        for (size_t i = 0; i < frame_len; i++) {
            checksum += frame_start_pos[i];
        }
        if (checksum != frame_start_pos[frame_len]) {
            ptr = frame_start_pos + frame_len + 1 + end_len;
            continue; // Skip corrupted frame
        }
        
        // Decode based on frame type
        double x = 0.0, y = 0.0, z = 0.0, temp = 25.0;
        
        if (frame_type == 0x01 && frame_len >= 20) { // GPS position
            double lat = le_read_double(frame_start_pos);
            double lon = le_read_double(frame_start_pos + 8);
            float alt = (float)le_read_u32(frame_start_pos + 16) / 100.0f; // Convert from cm to m
            
            x = lat;
            y = lon;
            z = alt;
            temp = 20.0 + (lat + lon) * 0.001; // Estimate temperature
        } else if (frame_type == 0x02 && frame_len >= 12) { // Environmental data
            float temp_f = be_read_float(frame_start_pos);
            float humidity = be_read_float(frame_start_pos + 4);
            float pressure = be_read_float(frame_start_pos + 8);
            
            x = temp_f;
            y = humidity;
            z = pressure;
            temp = temp_f;
        }
        
        uint64_t timestamp = (uint64_t)time(NULL) * 1000; // Use system time if not in frame
        
        jobject reading = (*env)->NewObject(env, readingClass, readingConstructor,
            (jlong)timestamp, x, y, z, temp, (jint)0, sensorId);
        
        if (reading) {
            (*env)->SetObjectArrayElement(env, result, (jsize)result_idx, reading);
        }
        
        result_idx++;
        ptr = frame_start_pos + frame_len + 1 + end_len;
    }
    
    return result;
}

// ===== JNI METHOD IMPLEMENTATIONS =====

JNIEXPORT jobjectArray JNICALL Java_com_example_SensorDecoder_decodeI2CSensor
  (JNIEnv *env, jobject thisObj, jbyteArray rawData, jobject calibration) {
    
    if (!rawData || !calibration) return NULL;
    
    jsize len = (*env)->GetArrayLength(env, rawData);
    if (len == 0) return NULL;
    
    jbyte *data = (*env)->GetByteArrayElements(env, rawData, NULL);
    if (!data) return NULL;
    
    // Extract calibration parameters
    jclass calibClass = (*env)->GetObjectClass(env, calibration);
    jfieldID offsetX_fid = (*env)->GetFieldID(env, calibClass, "offsetX", "D");
    jfieldID offsetY_fid = (*env)->GetFieldID(env, calibClass, "offsetY", "D");
    jfieldID offsetZ_fid = (*env)->GetFieldID(env, calibClass, "offsetZ", "D");
    jfieldID scaleX_fid = (*env)->GetFieldID(env, calibClass, "scaleX", "D");
    jfieldID scaleY_fid = (*env)->GetFieldID(env, calibClass, "scaleY", "D");
    jfieldID scaleZ_fid = (*env)->GetFieldID(env, calibClass, "scaleZ", "D");
    jfieldID tempCoeff_fid = (*env)->GetFieldID(env, calibClass, "temperatureCoefficient", "D");
    
    if (!offsetX_fid || !offsetY_fid || !offsetZ_fid || 
        !scaleX_fid || !scaleY_fid || !scaleZ_fid || !tempCoeff_fid) {
        (*env)->ReleaseByteArrayElements(env, rawData, data, JNI_ABORT);
        return NULL;
    }
    
    double offsetX = (*env)->GetDoubleField(env, calibration, offsetX_fid);
    double offsetY = (*env)->GetDoubleField(env, calibration, offsetY_fid);
    double offsetZ = (*env)->GetDoubleField(env, calibration, offsetZ_fid);
    double scaleX = (*env)->GetDoubleField(env, calibration, scaleX_fid);
    double scaleY = (*env)->GetDoubleField(env, calibration, scaleY_fid);
    double scaleZ = (*env)->GetDoubleField(env, calibration, scaleZ_fid);
    double tempCoeff = (*env)->GetDoubleField(env, calibration, tempCoeff_fid);
    
    jobjectArray result = decode_i2c_sensor(env, (const uint8_t*)data, (size_t)len,
                                           offsetX, offsetY, offsetZ,
                                           scaleX, scaleY, scaleZ,
                                           tempCoeff);
    
    (*env)->ReleaseByteArrayElements(env, rawData, data, JNI_ABORT);
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_example_SensorDecoder_decodeSPISensor
  (JNIEnv *env, jobject thisObj, jbyteArray rawData, jstring sensorType) {
    
    if (!rawData || !sensorType) return NULL;
    
    jsize len = (*env)->GetArrayLength(env, rawData);
    if (len == 0) return NULL;
    
    jbyte *data = (*env)->GetByteArrayElements(env, rawData, NULL);
    if (!data) return NULL;
    
    const char *type_str = (*env)->GetStringUTFChars(env, sensorType, NULL);
    if (!type_str) {
        (*env)->ReleaseByteArrayElements(env, rawData, data, JNI_ABORT);
        return NULL;
    }
    
    jobjectArray result = decode_spi_sensor(env, (const uint8_t*)data, (size_t)len, type_str);
    
    (*env)->ReleaseStringUTFChars(env, sensorType, type_str);
    (*env)->ReleaseByteArrayElements(env, rawData, data, JNI_ABORT);
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_example_SensorDecoder_decodeUARTSensor
  (JNIEnv *env, jobject thisObj, jbyteArray rawData, jbyteArray frameStart, jbyteArray frameEnd) {
    
    if (!rawData || !frameStart || !frameEnd) return NULL;
    
    jsize data_len = (*env)->GetArrayLength(env, rawData);
    jsize start_len = (*env)->GetArrayLength(env, frameStart);
    jsize end_len = (*env)->GetArrayLength(env, frameEnd);
    
    if (data_len == 0 || start_len == 0 || end_len == 0) return NULL;
    
    jbyte *data = (*env)->GetByteArrayElements(env, rawData, NULL);
    jbyte *start_marker = (*env)->GetByteArrayElements(env, frameStart, NULL);
    jbyte *end_marker = (*env)->GetByteArrayElements(env, frameEnd, NULL);
    
    if (!data || !start_marker || !end_marker) {
        if (data) (*env)->ReleaseByteArrayElements(env, rawData, data, JNI_ABORT);
        if (start_marker) (*env)->ReleaseByteArrayElements(env, frameStart, start_marker, JNI_ABORT);
        if (end_marker) (*env)->ReleaseByteArrayElements(env, frameEnd, end_marker, JNI_ABORT);
        return NULL;
    }
    
    jobjectArray result = decode_uart_sensor(env, 
        (const uint8_t*)data, (size_t)data_len,
        (const uint8_t*)start_marker, (size_t)start_len,
        (const uint8_t*)end_marker, (size_t)end_len);
    
    (*env)->ReleaseByteArrayElements(env, rawData, data, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, frameStart, start_marker, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, frameEnd, end_marker, JNI_ABORT);
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_example_SensorDecoder_batchDecode
  (JNIEnv *env, jobject thisObj, jobjectArray sensorStreams, jobjectArray streamTypes) {
    
    if (!sensorStreams || !streamTypes) return NULL;
    
    jsize num_streams = (*env)->GetArrayLength(env, sensorStreams);
    if (num_streams != (*env)->GetArrayLength(env, streamTypes)) return NULL;
    
    // Create array of arrays to hold results
    jclass readingClass = (*env)->FindClass(env, "com/example/SensorDecoder$SensorReading");
    jobjectArray result = (*env)->NewObjectArray(env, num_streams, 
        (*env)->FindClass(env, "[Lcom/example/SensorDecoder$SensorReading;"), NULL);
    
    if (!result) return NULL;
    
    for (jsize i = 0; i < num_streams; i++) {
        jbyteArray stream = (jbyteArray)(*env)->GetObjectArrayElement(env, sensorStreams, i);
        jstring type = (jstring)(*env)->GetObjectArrayElement(env, streamTypes, i);
        
        if (!stream || !type) continue;
        
        jobjectArray decoded = NULL;
        
        const char *type_str = (*env)->GetStringUTFChars(env, type, NULL);
        if (type_str) {
            if (strcmp(type_str, "i2c") == 0) {
                // For batch decode, use default calibration
                SensorDecoder_CalibrationData calib = {0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.001};
                decoded = Java_com_example_SensorDecoder_decodeI2CSensor(env, thisObj, stream, 
                    (*env)->NewDirectByteBuffer(env, &calib, sizeof(calib)));
            } else if (strcmp(type_str, "spi") == 0) {
                decoded = Java_com_example_SensorDecoder_decodeSPISensor(env, thisObj, stream, type);
            } else if (strcmp(type_str, "uart") == 0) {
                decoded = Java_com_example_SensorDecoder_decodeUARTSensor(env, thisObj, stream,
                    (*env)->NewByteArray(env, 2), (*env)->NewByteArray(env, 2));
            }
            (*env)->ReleaseStringUTFChars(env, type, type_str);
        }
        
        if (decoded) {
            (*env)->SetObjectArrayElement(env, result, i, decoded);
        }
    }
    
    return result;
}

JNIEXPORT jboolean JNICALL Java_com_example_SensorDecoder_validateSensorData
  (JNIEnv *env, jobject thisObj, jbyteArray rawData, jint expectedChecksum) {
    
    if (!rawData) return JNI_FALSE;
    
    jsize len = (*env)->GetArrayLength(env, rawData);
    if (len == 0) return JNI_FALSE;
    
    jbyte *data = (*env)->GetByteArrayElements(env, rawData, NULL);
    if (!data) return JNI_FALSE;
    
    // Simple checksum validation
    uint32_t calculated_checksum = 0;
    for (jsize i = 0; i < len; i++) {
        calculated_checksum += ((uint8_t*)data)[i];
    }
    
    (*env)->ReleaseByteArrayElements(env, rawData, data, JNI_ABORT);
    return (calculated_checksum == (uint32_t)expectedChecksum) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_com_example_SensorDecoder_extractMetadata
  (JNIEnv *env, jobject thisObj, jbyteArray rawData) {
    
    if (!rawData) return NULL;
    
    jsize len = (*env)->GetArrayLength(env, rawData);
    if (len < 17) return NULL; // Minimum header size: 4+1+8+4
    
    jbyte *data = (*env)->GetByteArrayElements(env, rawData, NULL);
    if (!data) return NULL;
    
    uint32_t magic = be_read_u32((const uint8_t*)data);
    uint8_t version = ((const uint8_t*)data)[4];
    uint64_t timestamp = be_read_u64((const uint8_t*)data + 5);
    uint32_t source_id = be_read_u32((const uint8_t*)data + 13);
    uint32_t data_size = len - 17;
    
    (*env)->ReleaseByteArrayElements(env, rawData, data, JNI_ABORT);
    
    // Create and return SensorMetadata object
    jclass metaClass = (*env)->FindClass(env, "com/example/SensorDecoder$SensorMetadata");
    jmethodID constructor = (*env)->GetMethodID(env, metaClass, "<init>", "(BJIJI)V");
    
    if (!constructor) return NULL;
    
    return (*env)->NewObject(env, metaClass, constructor,
        (jint)magic, (jbyte)version, (jlong)timestamp, (jint)source_id, (jint)data_size);
}