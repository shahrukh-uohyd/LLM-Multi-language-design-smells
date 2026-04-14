#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include "com_example_BinaryPayloadProcessor.h"

// CRC32 lookup table (IEEE polynomial 0xEDB88320)
static const uint32_t crc32_table[256] = {
    0x00000000, 0x77073096, 0xEE0E612C, 0x990951BA, 0x076DC419, 0x706AF48F,
    0xE963A535, 0x9E6495A3, 0x0EDB8832, 0x79DCB8A4, 0xE0D5E91E, 0x97D2D988,
    0x09B64C2B, 0x7EB17CBD, 0xE7B82D07, 0x90BF1D91, 0x1DB71064, 0x6AB020F2,
    0xF3B97148, 0x84BE41DE, 0x1ADAD47D, 0x6DDDE4EB, 0xF4D4B551, 0x83D385C7,
    0x136C9856, 0x646BA8C0, 0xFD62F97A, 0x8A65C9EC, 0x14015C4F, 0x63066CD9,
    0xFA0F3D63, 0x8D080DF5, 0x3B6E20C8, 0x4C69105E, 0xD56041E4, 0xA2677172,
    0x3C03E4D1, 0x4B04D447, 0xD20D85FD, 0xA50AB56B, 0x35B5A8FA, 0x42B2986C,
    0xDBBBC9D6, 0xACBCF940, 0x32D86CE3, 0x45DF5C75, 0xDCD60DCF, 0xABD13D59,
    0x26D930AC, 0x51DE003A, 0xC8D75180, 0xBFD06116, 0x21B4F4B5, 0x56B3C423,
    0xCFBA9599, 0xB8BDA50F, 0x2802B89E, 0x5F058808, 0xC60CD9B2, 0xB10BE924,
    0x2F6F7C87, 0x58684C11, 0xC1611DAB, 0xB6662D3D, 0x76DC4190, 0x01DB7106,
    0x98D220BC, 0xEFD5102A, 0x71B18589, 0x06B6B51F, 0x9FBFE4A5, 0xE8B8D433,
    0x7807C9A2, 0x0F00F934, 0x9609A88E, 0xE10E9818, 0x7F6A0DBB, 0x086D3D2D,
    0x91646C97, 0xE6635C01, 0x6B6B51F4, 0x1C6C6162, 0x856530D8, 0xF262004E,
    0x6C0695ED, 0x1B01A57B, 0x8208F4C1, 0xF50FC457, 0x65B0D9C6, 0x12B7E950,
    0x8BBEB8EA, 0xFCB9887C, 0x62DD1DDF, 0x15DA2D49, 0x8CD37CF3, 0xFBD44C65,
    0x4DB26158, 0x3AB551CE, 0xA3BC0074, 0xD4BB30E2, 0x4ADFA541, 0x3DD895D7,
    0xA4D1C46D, 0xD3D6F4FB, 0x4369E96A, 0x346ED9FC, 0xAD678846, 0xDA60B8D0,
    0x44042D73, 0x33031DE5, 0xAA0A4C5F, 0xDD0D7CC9, 0x5005713C, 0x270241AA,
    0xBE0B1010, 0xC90C2086, 0x5768B525, 0x206F85B3, 0xB966D409, 0xCE61E49F,
    0x5EDEF90E, 0x29D9C998, 0xB0D09822, 0xC7D7A8B4, 0x59B33D17, 0x2EB40D81,
    0xB7BD5C3B, 0xC0BA6CAD, 0xEDB88320, 0x9ABFB3B6, 0x03B6E20C, 0x74B1D29A,
    0xEAD54739, 0x9DD277AF, 0x04DB2615, 0x73DC1683, 0xE3630B12, 0x94643B84,
    0x0D6D6A3E, 0x7A6A5AA8, 0xE40ECF0B, 0x9309FF9D, 0x0A00AE27, 0x7D079EB1,
    0xF00F9344, 0x8708A3D2, 0x1E01F268, 0x6906C2FE, 0xF762575D, 0x806567CB,
    0x196C3671, 0x6E6B06E7, 0xFED41B76, 0x89D32BE0, 0x10DA7A5A, 0x67DD4ACC,
    0xF9B9DF6F, 0x8EBEEFF9, 0x17B7BE43, 0x60B08ED5, 0xD6D6A3E8, 0xA1D1937E,
    0x38D8C2C4, 0x4FDFF252, 0xD1BB67F1, 0xA6BC5767, 0x3FB506DD, 0x48B2364B,
    0xD80D2BDA, 0xAF0A1B4C, 0x36034AF6, 0x41047A60, 0xDF60EFC3, 0xA867DF55,
    0x316E8EEF, 0x4669BE79, 0xCB61B38C, 0xBC66831A, 0x256FD2A0, 0x5268E236,
    0xCC0C7795, 0xBB0B4703, 0x220216B9, 0x5505262F, 0xC5BA3BBE, 0xB2BD0B28,
    0x2BB45A92, 0x5CB36A04, 0xC2D7FFA7, 0xB5D0CF31, 0x2CD99E8B, 0x5BDEAE1D,
    0x9B64C2B0, 0xEC63F226, 0x756AA39C, 0x026D930A, 0x9C0906A9, 0xEB0E363F,
    0x72076785, 0x05005713, 0x95BF4A82, 0xE2B87A14, 0x7BB12BAE, 0x0CB61B38,
    0x92D28E9B, 0xE5D5BE0D, 0x7CDCEFB7, 0x0BDBDF21, 0x86D3D2D4, 0xF1D4E242,
    0x68DDB3F8, 0x1FDA836E, 0x81BE16CD, 0xF6B9265B, 0x6FB077E1, 0x18B74777,
    0x88085AE6, 0xFF0F6A70, 0x66063BCA, 0x11010B5C, 0x8F659EFF, 0xF862AE69,
    0x616BFFD3, 0x166CCF45, 0xA00AE278, 0xD70DD2EE, 0x4E048354, 0x3903B3C2,
    0xA7672661, 0xD06016F7, 0x4969474D, 0x3E6E77DB, 0xAED16A4A, 0xD9D65ADC,
    0x40DF0B66, 0x37D83BF0, 0xA9BCAE53, 0xDEBB9EC5, 0x47B2CF7F, 0x30B5FFE9,
    0xBDBDF21C, 0xCABAC28A, 0x53B39330, 0x24B4A3A6, 0xBAD03605, 0xCDD70693,
    0x54DE5729, 0x23D967BF, 0xB3667A2E, 0xC4614AB8, 0x5D681B02, 0x2A6F2B94,
    0xB40BBE37, 0xC30C8EA1, 0x5A05DF1B, 0x2D02EF8D
};

/**
 * Calculate CRC32 checksum using efficient table-driven algorithm
 */
static uint32_t calculate_crc32(const uint8_t* data, size_t length) {
    uint32_t crc = 0xFFFFFFFF;
    for (size_t i = 0; i < length; i++) {
        crc = (crc >> 8) ^ crc32_table[(crc ^ data[i]) & 0xFF];
    }
    return ~crc;
}

/**
 * XOR obfuscation - symmetric operation (same for obfuscate/deobfuscate)
 */
static void xor_obfuscate(uint8_t* data, size_t length, uint8_t key) {
    for (size_t i = 0; i < length; i++) {
        data[i] ^= key;
    }
}

/**
 * Run-Length Encoding compression
 * Format: For each run: [byte value][repeat count]
 * - Count of 1-255 repetitions stored as single byte
 * - Non-repeated bytes stored as [byte][0x01]
 */
static jbyteArray compress_rle(JNIEnv* env, const uint8_t* input, size_t length) {
    if (length == 0) {
        jbyteArray result = (*env)->NewByteArray(env, 0);
        return result;
    }
    
    // Worst case: no compression (each byte becomes 2 bytes)
    uint8_t* compressed = (uint8_t*)malloc(length * 2);
    if (!compressed) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"), 
                        "Native allocation failed");
        return NULL;
    }
    
    size_t out_pos = 0;
    size_t i = 0;
    
    while (i < length) {
        uint8_t current = input[i];
        size_t run_length = 1;
        
        // Count consecutive identical bytes (max 255)
        while (run_length < 255 && i + run_length < length && input[i + run_length] == current) {
            run_length++;
        }
        
        // Store value and count
        compressed[out_pos++] = current;
        compressed[out_pos++] = (uint8_t)run_length;
        
        i += run_length;
    }
    
    // Create Java byte array with exact compressed size
    jbyteArray result = (*env)->NewByteArray(env, (jsize)out_pos);
    if (result) {
        (*env)->SetByteArrayRegion(env, result, 0, (jsize)out_pos, (jbyte*)compressed);
    }
    
    free(compressed);
    return result;
}

/**
 * Run-Length Encoding decompression
 */
static jbyteArray decompress_rle(JNIEnv* env, const uint8_t* input, size_t length) {
    if (length == 0) {
        jbyteArray result = (*env)->NewByteArray(env, 0);
        return result;
    }
    
    // Validate input length (must be even)
    if (length % 2 != 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Invalid RLE data: length must be even");
        return NULL;
    }
    
    // Calculate decompressed size
    size_t decompressed_size = 0;
    for (size_t i = 1; i < length; i += 2) {
        decompressed_size += input[i]; // Add count byte
    }
    
    uint8_t* decompressed = (uint8_t*)malloc(decompressed_size);
    if (!decompressed) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Native allocation failed");
        return NULL;
    }
    
    size_t out_pos = 0;
    for (size_t i = 0; i < length; i += 2) {
        uint8_t value = input[i];
        uint8_t count = input[i + 1];
        
        for (uint8_t j = 0; j < count; j++) {
            decompressed[out_pos++] = value;
        }
    }
    
    jbyteArray result = (*env)->NewByteArray(env, (jsize)decompressed_size);
    if (result) {
        (*env)->SetByteArrayRegion(env, result, 0, (jsize)decompressed_size, (jbyte*)decompressed);
    }
    
    free(decompressed);
    return result;
}

/**
 * Validate payload structure: [MAGIC:4][LENGTH:4][TIMESTAMP:8][PAYLOAD...]
 */
static jobject validate_payload(JNIEnv* env, const uint8_t* data, size_t length) {
    // Find ValidationResult class and constructor
    jclass resultClass = (*env)->FindClass(env, "com/example/BinaryPayloadProcessor$ValidationResult");
    if (!resultClass) return NULL;
    
    jmethodID constructor = (*env)->GetMethodID(env, resultClass, "<init>", 
        "(ZIJI)V");
    if (!constructor) return NULL;
    
    // Default invalid result
    jboolean isValid = JNI_FALSE;
    jint payloadLength = 0;
    jint headerVersion = 0;
    jlong timestamp = 0;
    
    // Check minimum header size (4 magic + 4 length + 8 timestamp = 16 bytes)
    if (length >= 16) {
        // Extract magic number (big-endian)
        uint32_t magic = ((uint32_t)data[0] << 24) | 
                         ((uint32_t)data[1] << 16) | 
                         ((uint32_t)data[2] << 8) | 
                         (uint32_t)data[3];
        
        // Extract payload length (big-endian)
        payloadLength = ((jint)data[4] << 24) | 
                        ((jint)data[5] << 16) | 
                        ((jint)data[6] << 8) | 
                        (jint)data[7];
        
        // Extract timestamp (big-endian, 64-bit)
        timestamp = ((jlong)data[8] << 56) | 
                    ((jlong)data[9] << 48) | 
                    ((jlong)data[10] << 40) | 
                    ((jlong)data[11] << 32) | 
                    ((jlong)data[12] << 24) | 
                    ((jlong)data[13] << 16) | 
                    ((jlong)data[14] << 8) | 
                    (jlong)data[15];
        
        // Validate magic number and length consistency
        if (magic == 0xCAFEBABE && payloadLength >= 0 && (size_t)(16 + payloadLength) == length) {
            isValid = JNI_TRUE;
            headerVersion = 1; // Simple versioning
        }
    }
    
    return (*env)->NewObject(env, resultClass, constructor, 
        isValid, payloadLength, headerVersion, timestamp);
}

// ===== JNI METHOD IMPLEMENTATIONS =====

JNIEXPORT jint JNICALL Java_com_example_BinaryPayloadProcessor_calculateCRC32
  (JNIEnv *env, jobject thisObj, jbyteArray payload) {
    
    if (payload == NULL) {
        return 0;
    }
    
    jsize length = (*env)->GetArrayLength(env, payload);
    if (length == 0) {
        return 0xFFFFFFFF; // CRC32 of empty data
    }
    
    // Use GetByteArrayRegion for read-only access (avoids pinning/copying overhead)
    uint8_t* buffer = (uint8_t*)malloc(length);
    if (!buffer) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Native buffer allocation failed");
        return 0;
    }
    
    (*env)->GetByteArrayRegion(env, payload, 0, length, (jbyte*)buffer);
    if ((*env)->ExceptionCheck(env)) {
        free(buffer);
        return 0;
    }
    
    uint32_t crc = calculate_crc32(buffer, (size_t)length);
    free(buffer);
    
    return (jint)crc;
}

JNIEXPORT jbyteArray JNICALL Java_com_example_BinaryPayloadProcessor_obfuscateXOR
  (JNIEnv *env, jobject thisObj, jbyteArray payload, jbyte key) {
    
    if (payload == NULL) {
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, payload);
    if (length == 0) {
        return (*env)->NewByteArray(env, 0);
    }
    
    // Get direct pointer to array elements (critical section)
    jbyte* elements = (*env)->GetByteArrayElements(env, payload, NULL);
    if (!elements) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Failed to access array elements");
        return NULL;
    }
    
    // Create result array
    jbyteArray result = (*env)->NewByteArray(env, length);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, payload, elements, JNI_ABORT);
        return NULL;
    }
    
    // Copy and transform in one pass
    jbyte* resultElements = (*env)->GetByteArrayElements(env, result, NULL);
    if (!resultElements) {
        (*env)->ReleaseByteArrayElements(env, payload, elements, JNI_ABORT);
        return NULL;
    }
    
    for (jsize i = 0; i < length; i++) {
        resultElements[i] = elements[i] ^ key;
    }
    
    // Release arrays (JNI_COMMIT writes changes, but we used separate arrays)
    (*env)->ReleaseByteArrayElements(env, result, resultElements, 0);
    (*env)->ReleaseByteArrayElements(env, payload, elements, JNI_ABORT); // No changes to input
    
    return result;
}

// Deobfuscation uses same implementation as obfuscation (XOR is symmetric)
JNIEXPORT jbyteArray JNICALL Java_com_example_BinaryPayloadProcessor_deobfuscateXOR
  (JNIEnv *env, jobject thisObj, jbyteArray payload, jbyte key) {
    
    return Java_com_example_BinaryPayloadProcessor_obfuscateXOR(env, thisObj, payload, key);
}

JNIEXPORT jbyteArray JNICALL Java_com_example_BinaryPayloadProcessor_compressRLE
  (JNIEnv *env, jobject thisObj, jbyteArray payload) {
    
    if (payload == NULL) {
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, payload);
    
    // Use GetByteArrayRegion for safe read-only access
    uint8_t* buffer = (uint8_t*)malloc(length);
    if (!buffer && length > 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Buffer allocation failed");
        return NULL;
    }
    
    if (length > 0) {
        (*env)->GetByteArrayRegion(env, payload, 0, length, (jbyte*)buffer);
        if ((*env)->ExceptionCheck(env)) {
            free(buffer);
            return NULL;
        }
    }
    
    jbyteArray result = compress_rle(env, buffer, (size_t)length);
    free(buffer);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_example_BinaryPayloadProcessor_decompressRLE
  (JNIEnv *env, jobject thisObj, jbyteArray compressed) {
    
    if (compressed == NULL) {
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, compressed);
    
    uint8_t* buffer = (uint8_t*)malloc(length);
    if (!buffer && length > 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Buffer allocation failed");
        return NULL;
    }
    
    if (length > 0) {
        (*env)->GetByteArrayRegion(env, compressed, 0, length, (jbyte*)buffer);
        if ((*env)->ExceptionCheck(env)) {
            free(buffer);
            return NULL;
        }
    }
    
    jbyteArray result = decompress_rle(env, buffer, (size_t)length);
    free(buffer);
    return result;
}

JNIEXPORT jobject JNICALL Java_com_example_BinaryPayloadProcessor_validatePayload
  (JNIEnv *env, jobject thisObj, jbyteArray payload) {
    
    if (payload == NULL) {
        // Return invalid result for null input
        jclass resultClass = (*env)->FindClass(env, "com/example/BinaryPayloadProcessor$ValidationResult");
        jmethodID constructor = (*env)->GetMethodID(env, resultClass, "<init>", "(ZIJI)V");
        return (*env)->NewObject(env, resultClass, constructor, JNI_FALSE, 0, 0, 0);
    }
    
    jsize length = (*env)->GetArrayLength(env, payload);
    
    uint8_t* buffer = (uint8_t*)malloc(length);
    if (!buffer && length > 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Buffer allocation failed");
        return NULL;
    }
    
    if (length > 0) {
        (*env)->GetByteArrayRegion(env, payload, 0, length, (jbyte*)buffer);
        if ((*env)->ExceptionCheck(env)) {
            free(buffer);
            return NULL;
        }
    }
    
    jobject result = validate_payload(env, buffer, (size_t)length);
    free(buffer);
    return result;
}