#ifndef TEXT_PROCESSOR_H
#define TEXT_PROCESSOR_H

#include <jni.h>
#include <string>
#include <vector>
#include <map>

// Forward declaration
struct ParsedElementsInternal;
struct ProcessedElementsInternal;

// Internal data structures
struct ParsedElementsInternal {
    std::vector<std::string> elements;
    std::string delimiterUsed;
    size_t elementCount;
    
    ParsedElementsInternal() : elementCount(0) {}
};

struct ProcessedElementsInternal {
    std::vector<std::string> processedElements;
    std::vector<std::string> processingLog;
    std::string processingLogic;
    
    ProcessedElementsInternal() {}
};

extern "C" {
    // JNI function declarations
    JNIEXPORT jlong JNICALL Java_TextProcessor_parseInputTextNative(JNIEnv *env, jobject obj, jstring inputText);
    JNIEXPORT jlong JNICALL Java_TextProcessor_processStructuredElementsNative(JNIEnv *env, jobject obj, jlong parsedElementsHandle, jstring processingLogic);
    JNIEXPORT jstring JNICALL Java_TextProcessor_generateOutputFromProcessedElementsNative(JNIEnv *env, jobject obj, jlong processedElementsHandle);
    JNIEXPORT void JNICALL Java_TextProcessor_cleanupHandleNative(JNIEnv *env, jobject obj, jlong handle);
}

// Helper functions
std::vector<std::string> splitString(const std::string& str, char delimiter);
std::string trim(const std::string& str);
std::vector<std::string> applyProcessingLogic(const std::vector<std::string>& elements, const std::string& logic);

#endif // TEXT_PROCESSOR_H