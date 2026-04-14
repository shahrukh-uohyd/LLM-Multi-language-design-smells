#ifndef TRANSFORMATION_PIPELINE_H
#define TRANSFORMATION_PIPELINE_H

#include <jni.h>
#include <string>
#include <map>
#include <vector>
#include <memory>

// Forward declarations
struct InterpretedDataInternal;
struct TransformedDataInternal;

// Internal data structures
struct InterpretedDataInternal {
    std::map<std::string, std::string> fields;
    std::string dataType;
    long timestamp;
    
    InterpretedDataInternal() : timestamp(0) {}
};

struct TransformedDataInternal {
    std::map<std::string, std::string> transformedFields;
    std::vector<std::string> appliedRules;
    
    TransformedDataInternal() {}
};

extern "C" {
    // JNI function declarations
    JNIEXPORT jlong JNICALL Java_TransformationPipeline_interpretLowLevelDataNative(JNIEnv *env, jobject obj, jbyteArray lowLevelData);
    JNIEXPORT jlong JNICALL Java_TransformationPipeline_applyTransformationRulesNative(JNIEnv *env, jobject obj, jlong interpretedDataHandle, jobjectArray rules);
    JNIEXPORT jbyteArray JNICALL Java_TransformationPipeline_generateTransformedOutputNative(JNIEnv *env, jobject obj, jlong transformedDataHandle);
    JNIEXPORT void JNICALL Java_TransformationPipeline_cleanupHandleNative(JNIEnv *env, jobject obj, jlong handle);
    
    // Helper functions for transformation operations
    std::string applyUpperCaseTransformation(const std::string& value);
    std::string applyLowerCaseTransformation(const std::string& value);
    std::string applyNumberTransformation(const std::string& value, const std::string& operation, double param);
    std::string applyRegexTransformation(const std::string& value, const std::string& pattern, const std::string& replacement);
}

#endif // TRANSFORMATION_PIPELINE_H