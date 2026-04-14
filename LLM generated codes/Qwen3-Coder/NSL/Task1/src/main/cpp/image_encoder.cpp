// File: src/main/cpp/image_encoder.cpp
#include <jni.h>
#include <string>
#include <cstring>
#include <vector>
#include <iostream>

// Simple image encoding simulation using basic compression
extern "C" {

JNIEXPORT jbyteArray JNICALL Java_com_example_image_ImageEncoder_encodeImageData
(JNIEnv *env, jobject obj, jbyteArray imageData, jstring format) {
    const char *formatStr = env->GetStringUTFChars(format, 0);
    
    // Get the input image data
    jsize len = env->GetArrayLength(imageData);
    jbyte *buf = env->GetByteArrayElements(imageData, 0);
    
    std::vector<jbyte> inputBuf(buf, buf + len);
    env->ReleaseByteArrayElements(imageData, buf, 0);
    
    // Simulate image encoding based on format
    std::vector<jbyte> encodedData;
    
    if (strcmp(formatStr, "JPEG") == 0) {
        // JPEG-like compression simulation
        for (size_t i = 0; i < inputBuf.size(); i += 2) {
            encodedData.push_back(inputBuf[i]);
        }
    } else if (strcmp(formatStr, "PNG") == 0) {
        // PNG-like compression simulation
        for (size_t i = 0; i < inputBuf.size(); i += 3) {
            encodedData.push_back(inputBuf[i]);
        }
    } else if (strcmp(formatStr, "WEBP") == 0) {
        // WEBP-like compression simulation
        for (size_t i = 0; i < inputBuf.size(); i += 4) {
            encodedData.push_back(inputBuf[i]);
        }
    } else {
        // Default: simple run-length encoding simulation
        for (size_t i = 0; i < inputBuf.size(); i += 5) {
            encodedData.push_back(inputBuf[i]);
        }
    }
    
    // Add header information
    std::vector<jbyte> result;
    result.push_back((jbyte)'E');  // E for Encoded
    result.push_back((jbyte)'N');
    result.push_back((jbyte)'C');
    result.insert(result.end(), encodedData.begin(), encodedData.end());
    
    // Release the format string
    env->ReleaseStringUTFChars(format, formatStr);
    
    // Create and return the resulting byte array
    jbyteArray resultArray = env->NewByteArray(result.size());
    env->SetByteArrayRegion(resultArray, 0, result.size(), &result[0]);
    
    return resultArray;
}

JNIEXPORT jbyteArray JNICALL Java_com_example_image_ImageEncoder_compressImage
(JNIEnv *env, jobject obj, jbyteArray imageData) {
    jsize len = env->GetArrayLength(imageData);
    jbyte *buf = env->GetByteArrayElements(imageData, 0);
    
    std::vector<jbyte> inputBuf(buf, buf + len);
    env->ReleaseByteArrayElements(imageData, buf, 0);
    
    // Simple compression: take every other byte
    std::vector<jbyte> compressed;
    for (size_t i = 0; i < inputBuf.size(); i += 2) {
        compressed.push_back(inputBuf[i]);
    }
    
    jbyteArray resultArray = env->NewByteArray(compressed.size());
    env->SetByteArrayRegion(resultArray, 0, compressed.size(), &compressed[0]);
    
    return resultArray;
}

JNIEXPORT jboolean JNICALL Java_com_example_image_ImageEncoder_isNativeLibraryLoaded
(JNIEnv *env, jobject obj) {
    // Always return true since we're in the native library
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_com_example_image_ImageEncoder_getNativeVersion
(JNIEnv *env, jobject obj) {
    return env->NewStringUTF("ImageEncoder v1.0.0 (Native)");
}

} // extern "C"