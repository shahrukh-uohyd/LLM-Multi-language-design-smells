/**
 * image_validator_native.cpp
 *
 * JNI implementation for ImageValidatorJniBridge.
 * Validates image format (magic bytes) and header structure for each image
 * in a batch, then constructs and returns Java ValidationResult objects.
 *
 * Build (Android NDK CMakeLists.txt excerpt):
 *   add_library(image_validator_native SHARED image_validator_native.cpp)
 *   target_link_libraries(image_validator_native log)
 */

#include <jni.h>
#include <android/log.h>
#include <cstdint>
#include <cstring>
#include <string>

#include "image_validator_native.h"

#define LOG_TAG "ImageValidatorNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── Internal result struct ────────────────────────────────────────────────────

struct NativeValidationResult {
    bool        valid              = false;
    int         errorCode          = IMG_ERR_NONE;
    std::string errorMessage;
    int         detectedFormat     = IMG_FMT_UNKNOWN;
};

// ── Magic-byte detection ──────────────────────────────────────────────────────

/**
 * Identifies the image format by inspecting leading bytes (magic numbers).
 * Returns one of the IMG_FMT_* constants.
 */
static int detectFormat(const uint8_t* data, jsize len) {
    if (len < IMG_MAGIC_MIN_BYTES) return IMG_FMT_UNKNOWN;

    // JPEG: FF D8 FF
    if (data[0] == 0xFF && data[1] == 0xD8 && data[2] == 0xFF)
        return IMG_FMT_JPEG;

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (data[0] == 0x89 && data[1] == 0x50 && data[2] == 0x4E &&
        data[3] == 0x47 && data[4] == 0x0D && data[5] == 0x0A &&
        data[6] == 0x1A && data[7] == 0x0A)
        return IMG_FMT_PNG;

    // WebP: 52 49 46 46 ?? ?? ?? ?? 57 45 42 50  ("RIFF....WEBP")
    if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' &&
        data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P')
        return IMG_FMT_WEBP;

    // GIF: 47 49 46 38 ('GIF8')
    if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8')
        return IMG_FMT_GIF;

    // BMP: 42 4D ('BM')
    if (data[0] == 0x42 && data[1] == 0x4D)
        return IMG_FMT_BMP;

    return IMG_FMT_UNKNOWN;
}

// ── Per-format header validators ──────────────────────────────────────────────

static NativeValidationResult validateJpeg(const uint8_t* data, jsize len) {
    NativeValidationResult r;
    // JPEG must end with FF D9 (EOI marker)
    if (len < 4) {
        r.errorCode    = IMG_ERR_DATA_TOO_SHORT;
        r.errorMessage = "JPEG data too short";
        return r;
    }
    if (data[len - 2] != 0xFF || data[len - 1] != 0xD9) {
        r.errorCode    = IMG_ERR_INVALID_HEADER;
        r.errorMessage = "JPEG missing EOI marker (FF D9)";
        return r;
    }
    r.valid          = true;
    r.detectedFormat = IMG_FMT_JPEG;
    return r;
}

static NativeValidationResult validatePng(const uint8_t* data, jsize len) {
    NativeValidationResult r;
    // Minimum PNG: 8 (sig) + 25 (IHDR chunk) + 12 (IEND chunk) = 45 bytes
    if (len < 45) {
        r.errorCode    = IMG_ERR_DATA_TOO_SHORT;
        r.errorMessage = "PNG data too short to contain IHDR";
        return r;
    }
    // IHDR chunk type must appear at byte 12
    if (data[12] != 'I' || data[13] != 'H' || data[14] != 'D' || data[15] != 'R') {
        r.errorCode    = IMG_ERR_INVALID_HEADER;
        r.errorMessage = "PNG missing IHDR chunk at expected offset";
        return r;
    }
    // Width and height fields must be non-zero (bytes 16-23)
    uint32_t width  = (data[16] << 24) | (data[17] << 16) | (data[18] << 8) | data[19];
    uint32_t height = (data[20] << 24) | (data[21] << 16) | (data[22] << 8) | data[23];
    if (width == 0 || height == 0) {
        r.errorCode    = IMG_ERR_CORRUPTED_DATA;
        r.errorMessage = "PNG IHDR contains zero width or height";
        return r;
    }
    r.valid          = true;
    r.detectedFormat = IMG_FMT_PNG;
    return r;
}

static NativeValidationResult validateWebp(const uint8_t* data, jsize len) {
    NativeValidationResult r;
    // Minimum WebP RIFF header: 12 bytes
    if (len < 12) {
        r.errorCode    = IMG_ERR_DATA_TOO_SHORT;
        r.errorMessage = "WebP data too short";
        return r;
    }
    // RIFF chunk size (bytes 4-7, LE) + 8 must equal total file size
    uint32_t riffSize = static_cast<uint32_t>(data[4])
                      | (static_cast<uint32_t>(data[5]) << 8)
                      | (static_cast<uint32_t>(data[6]) << 16)
                      | (static_cast<uint32_t>(data[7]) << 24);
    if (static_cast<jsize>(riffSize + 8) != len) {
        r.errorCode    = IMG_ERR_INVALID_HEADER;
        r.errorMessage = "WebP RIFF chunk size mismatch";
        return r;
    }
    r.valid          = true;
    r.detectedFormat = IMG_FMT_WEBP;
    return r;
}

static NativeValidationResult validateGif(const uint8_t* data, jsize len) {
    NativeValidationResult r;
    // GIF signature: "GIF87a" or "GIF89a" (6 bytes), then logical screen descriptor
    if (len < 13) {
        r.errorCode    = IMG_ERR_DATA_TOO_SHORT;
        r.errorMessage = "GIF data too short";
        return r;
    }
    if (!(data[4] == '7' || data[4] == '9') || data[5] != 'a') {
        r.errorCode    = IMG_ERR_INVALID_HEADER;
        r.errorMessage = "GIF version field invalid (expected 87a or 89a)";
        return r;
    }
    r.valid          = true;
    r.detectedFormat = IMG_FMT_GIF;
    return r;
}

static NativeValidationResult validateBmp(const uint8_t* data, jsize len) {
    NativeValidationResult r;
    // BMP header: 14 bytes file header + 40 bytes DIB header (minimum)
    if (len < 54) {
        r.errorCode    = IMG_ERR_DATA_TOO_SHORT;
        r.errorMessage = "BMP data too short for full header";
        return r;
    }
    // File size field (bytes 2-5, LE) should match actual size
    uint32_t fileSize = static_cast<uint32_t>(data[2])
                      | (static_cast<uint32_t>(data[3]) << 8)
                      | (static_cast<uint32_t>(data[4]) << 16)
                      | (static_cast<uint32_t>(data[5]) << 24);
    if (static_cast<jsize>(fileSize) != len) {
        r.errorCode    = IMG_ERR_INVALID_HEADER;
        r.errorMessage = "BMP file-size field does not match actual data length";
        return r;
    }
    r.valid          = true;
    r.detectedFormat = IMG_FMT_BMP;
    return r;
}

// ── Master validator ──────────────────────────────────────────────────────────

/**
 * Validates format and header for a single image buffer.
 */
static NativeValidationResult validateImage(const uint8_t* data, jsize len) {
    if (len < IMG_MAGIC_MIN_BYTES) {
        return {false, IMG_ERR_DATA_TOO_SHORT, "Image data too short to detect format",
                IMG_FMT_UNKNOWN};
    }

    int fmt = detectFormat(data, len);

    switch (fmt) {
        case IMG_FMT_JPEG:  return validateJpeg(data, len);
        case IMG_FMT_PNG:   return validatePng (data, len);
        case IMG_FMT_WEBP:  return validateWebp(data, len);
        case IMG_FMT_GIF:   return validateGif (data, len);
        case IMG_FMT_BMP:   return validateBmp (data, len);
        default:
            return {false, IMG_ERR_UNSUPPORTED_FORMAT,
                    "Image format not recognised or not supported",
                    IMG_FMT_UNKNOWN};
    }
}

// ── JNI entry point ───────────────────────────────────────────────────────────

extern "C" {

/**
 * com.example.imageservice.jni.ImageValidatorJniBridge.nativeValidateBatch
 *
 * Validates each image in the batch and returns a ValidationResult[] to Java.
 */
JNIEXPORT jobjectArray JNICALL
Java_com_example_imageservice_jni_ImageValidatorJniBridge_nativeValidateBatch(
        JNIEnv*      env,
        jobject      /* thiz */,
        jobjectArray imageDataArray,
        jobjectArray filenameArray,
        jint         count) {

    // ── 1. Look up the ValidationResult class and constructor ─────────────────
    jclass resultClass = env->FindClass(
        "com/example/imageservice/model/ValidationResult");
    if (!resultClass) {
        LOGE("nativeValidateBatch: cannot find ValidationResult class");
        return nullptr;
    }

    // Constructor signature: (String filename, boolean valid, int errorCode,
    //                         String errorMessage, int detectedFormatOrdinal)
    jmethodID resultCtor = env->GetMethodID(
        resultClass, "<init>", "(Ljava/lang/String;ZILjava/lang/String;I)V");
    if (!resultCtor) {
        LOGE("nativeValidateBatch: cannot find ValidationResult constructor");
        env->DeleteLocalRef(resultClass);
        return nullptr;
    }

    // ── 2. Allocate the output ValidationResult[] ─────────────────────────────
    jobjectArray resultArray = env->NewObjectArray(count, resultClass, nullptr);
    if (!resultArray) {
        LOGE("nativeValidateBatch: failed to allocate result array");
        env->DeleteLocalRef(resultClass);
        return nullptr;
    }

    // ── 3. Process each image ─────────────────────────────────────────────────
    for (jint i = 0; i < count; ++i) {

        // Retrieve filename string
        auto jFilename = static_cast<jstring>(
            env->GetObjectArrayElement(filenameArray, i));
        const char* filename = jFilename
            ? env->GetStringUTFChars(jFilename, nullptr)
            : "unknown";

        // Retrieve raw byte array for this image
        auto jImageData = static_cast<jbyteArray>(
            env->GetObjectArrayElement(imageDataArray, i));

        NativeValidationResult nativeResult;

        if (!jImageData) {
            LOGE("nativeValidateBatch: null image data at index %d", i);
            nativeResult = {false, IMG_ERR_DATA_TOO_SHORT,
                            "Image byte array is null", IMG_FMT_UNKNOWN};
        } else {
            jsize       len  = env->GetArrayLength(jImageData);
            jbyte*      raw  = env->GetByteArrayElements(jImageData, nullptr);

            if (!raw) {
                LOGE("nativeValidateBatch: failed to pin byte array at index %d", i);
                nativeResult = {false, IMG_ERR_CORRUPTED_DATA,
                                "Could not access image bytes", IMG_FMT_UNKNOWN};
            } else {
                // ── Core validation ───────────────────────────────────────────
                nativeResult = validateImage(
                    reinterpret_cast<const uint8_t*>(raw), len);

                env->ReleaseByteArrayElements(jImageData, raw, JNI_ABORT);
            }
            env->DeleteLocalRef(jImageData);
        }

        LOGI("Image[%d] '%s': valid=%s fmt=%d err=%d msg=%s",
             i, filename,
             nativeResult.valid ? "true" : "false",
             nativeResult.detectedFormat,
             nativeResult.errorCode,
             nativeResult.errorMessage.c_str());

        // ── 4. Construct Java ValidationResult for this image ─────────────────
        jstring jFilenameOut  = env->NewStringUTF(filename);
        jstring jErrorMessage = env->NewStringUTF(nativeResult.errorMessage.c_str());

        jobject jResult = env->NewObject(
            resultClass, resultCtor,
            jFilenameOut,
            static_cast<jboolean>(nativeResult.valid),
            static_cast<jint>(nativeResult.errorCode),
            jErrorMessage,
            static_cast<jint>(nativeResult.detectedFormat));

        env->SetObjectArrayElement(resultArray, i, jResult);

        // ── 5. Release local JNI references ───────────────────────────────────
        env->DeleteLocalRef(jResult);
        env->DeleteLocalRef(jErrorMessage);
        env->DeleteLocalRef(jFilenameOut);
        if (jFilename) {
            env->ReleaseStringUTFChars(jFilename, filename);
            env->DeleteLocalRef(jFilename);
        }
    }

    env->DeleteLocalRef(resultClass);
    return resultArray;
}

} // extern "C"