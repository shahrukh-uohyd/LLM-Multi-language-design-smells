#ifndef DATAPROCESSOR_H
#define DATAPROCESSOR_H

#include <jni.h>
#include <string>
#include <vector>
#include <memory>

// Forward declarations
struct ParsedDataInternal;
struct NormalizedDataInternal;

// Internal data structures
struct ParsedDataInternal {
    std::vector<std::string> elements;
    size_t elementCount;
    
    ParsedDataInternal() : elementCount(0) {}
};

struct NormalizedDataInternal {
    std::vector<std::string> normalizedElements;
    std::string rulesApplied;
    
    NormalizedDataInternal() {}
};

extern "C" {
    // JNI function declarations
    JNIEXPORT jlong JNICALL Java_DataProcessor_parseInputNative(JNIEnv *env, jobject obj, jstring rawData);
    JNIEXPORT jlong JNICALL Java_DataProcessor_normalizeParsedDataNative(JNIEnv *env, jobject obj, jlong parsedDataHandle);
    JNIEXPORT jobject JNICALL Java_DataProcessor_computeResultNative(JNIEnv *env, jobject obj, jlong normalizedDataHandle);
    JNIEXPORT void JNICALL Java_DataProcessor_cleanupHandleNative(JNIEnv *env, jobject obj, jlong handle);
}

#endif // DATAPROCESSOR_H