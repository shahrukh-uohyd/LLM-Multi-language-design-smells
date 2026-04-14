// image_validator.cpp
#include "image_validator.h"
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <algorithm>

// Helper function to check if file exists
bool fileExists(const std::string& path) {
    std::ifstream file(path);
    return file.good();
}

// Helper function to read file header bytes
std::vector<unsigned char> readHeader(const std::string& path, size_t numBytes = 16) {
    std::vector<unsigned char> header(numBytes);
    std::ifstream file(path, std::ios::binary);
    if (file.is_open()) {
        file.read(reinterpret_cast<char*>(header.data()), numBytes);
        file.close();
    }
    return header;
}

// Check if image format is supported based on magic bytes
bool isSupportedFormat(const std::string& path) {
    auto header = readHeader(path, 12);
    
    // JPEG: starts with FF D8 FF
    if (header.size() >= 3 && header[0] == 0xFF && header[1] == 0xD8 && header[2] == 0xFF) {
        return true;
    }
    
    // PNG: starts with 89 50 4E 47 0D 0A 1A 0A
    if (header.size() >= 8 && header[0] == 0x89 && header[1] == 0x50 && 
        header[2] == 0x4E && header[3] == 0x47 && header[4] == 0x0D && 
        header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A) {
        return true;
    }
    
    // GIF: starts with 47 49 46 38 (GIF8)
    if (header.size() >= 4 && header[0] == 0x47 && header[1] == 0x49 && 
        header[2] == 0x46 && header[3] == 0x38) {
        return true;
    }
    
    // BMP: starts with 42 4D (BM)
    if (header.size() >= 2 && header[0] == 0x42 && header[1] == 0x4D) {
        return true;
    }
    
    // TIFF: starts with 49 49 2A 00 or 4D 4D 00 2A
    if (header.size() >= 4 && 
        ((header[0] == 0x49 && header[1] == 0x49 && header[2] == 0x2A && header[3] == 0x00) ||
         (header[0] == 0x4D && header[1] == 0x4D && header[2] == 0x00 && header[3] == 0x2A))) {
        return true;
    }
    
    return false;
}

// Validate image header integrity
bool validateImageHeader(const std::string& path) {
    if (!fileExists(path)) {
        return false;
    }
    
    auto header = readHeader(path, 12);
    if (header.size() < 4) {
        return false;
    }
    
    // Basic checks for common image formats
    // JPEG validation
    if (header[0] == 0xFF && header[1] == 0xD8) {
        // JPEG should have SOI marker at beginning
        return true;
    }
    
    // PNG validation
    if (header[0] == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
        // PNG signature validation
        return true;
    }
    
    // GIF validation
    if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) {
        // GIF signature validation
        return true;
    }
    
    // BMP validation
    if (header[0] == 0x42 && header[1] == 0x4D) {
        // BMP signature validation
        return true;
    }
    
    // TIFF validation
    if ((header[0] == 0x49 && header[1] == 0x49) || (header[0] == 0x4D && header[1] == 0x4D)) {
        // TIFF signature validation
        return true;
    }
    
    return false;
}

// Get specific error message for validation failure
std::string getValidationError(const std::string& path) {
    if (!fileExists(path)) {
        return "File does not exist";
    }
    
    auto header = readHeader(path, 12);
    if (header.size() < 4) {
        return "File too small to contain valid image header";
    }
    
    if (!isSupportedFormat(path)) {
        return "Unsupported image format detected";
    }
    
    if (!validateImageHeader(path)) {
        return "Invalid image header structure";
    }
    
    return "No specific error found";
}

// Helper function to create ValidationResult object
jobject createValidationResult(JNIEnv* env, bool isValid, const std::string& error, const std::string& imageFile) {
    // Find the ValidationResult class
    jclass resultClass = env->FindClass("ValidationResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    // Find the constructor
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(ZLjava/lang/String;Ljava/lang/String;)V");
    if (constructor == nullptr) {
        return nullptr;
    }
    
    // Create string objects
    jstring jError = env->NewStringUTF(error.c_str());
    jstring jImageFile = env->NewStringUTF(imageFile.c_str());
    
    // Create the ValidationResult object
    jobject result = env->NewObject(resultClass, constructor, isValid, jError, jImageFile);
    
    // Clean up local references
    env->DeleteLocalRef(jError);
    env->DeleteLocalRef(jImageFile);
    
    return result;
}

JNIEXPORT jboolean JNICALL Java_ImageValidationService_validateImageHeader
  (JNIEnv *env, jobject obj, jstring imagePath) {
    
    const char* path = env->GetStringUTFChars(imagePath, 0);
    bool result = validateImageHeader(std::string(path));
    env->ReleaseStringUTFChars(imagePath, path);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_ImageValidationService_isSupportedFormat
  (JNIEnv *env, jobject obj, jstring imagePath) {
    
    const char* path = env->GetStringUTFChars(imagePath, 0);
    bool result = isSupportedFormat(std::string(path));
    env->ReleaseStringUTFChars(imagePath, path);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_ImageValidationService_getValidationError
  (JNIEnv *env, jobject obj, jstring imagePath) {
    
    const char* path = env->GetStringUTFChars(imagePath, 0);
    std::string error = getValidationError(std::string(path));
    env->ReleaseStringUTFChars(imagePath, path);
    
    return env->NewStringUTF(error.c_str());
}

JNIEXPORT jobjectArray JNICALL Java_ImageValidationService_validateImageBatch
  (JNIEnv *env, jobject obj, jobjectArray imagePaths) {
    
    jsize length = env->GetArrayLength(imagePaths);
    if (length == 0) {
        // Return empty array
        jclass resultClass = env->FindClass("ValidationResult");
        return env->NewObjectArray(0, resultClass, nullptr);
    }
    
    // Create ValidationResult array
    jclass resultClass = env->FindClass("ValidationResult");
    jobjectArray resultArray = env->NewObjectArray(length, resultClass, nullptr);
    
    // Process each image
    for (jsize i = 0; i < length; i++) {
        jstring imagePath = (jstring)env->GetObjectArrayElement(imagePaths, i);
        const char* path = env->GetStringUTFChars(imagePath, 0);
        
        bool hasValidHeader = validateImageHeader(std::string(path));
        bool isSupported = isSupportedFormat(std::string(path));
        std::string error;
        bool isValid = true;
        
        if (!hasValidHeader) {
            error = "Invalid image header: " + getValidationError(std::string(path));
            isValid = false;
        } else if (!isSupported) {
            error = "Unsupported image format";
            isValid = false;
        }
        
        // Create ValidationResult object
        jobject validationResult = createValidationResult(
            env, isValid, error, std::string(path)
        );
        
        // Set in array
        env->SetObjectArrayElement(resultArray, i, validationResult);
        
        // Clean up
        env->ReleaseStringUTFChars(imagePath, path);
        env->DeleteLocalRef(imagePath);
        env->DeleteLocalRef(validationResult);
    }
    
    return resultArray;
}