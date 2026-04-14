#include <jni.h>
#include <vector>
#include <stdexcept>
#include <cstdint>

// --- Mocking the Core C++ Transformation Logic ---
// In production, these would be linked from your existing C++ libraries.
struct PipelineContext {
    std::vector<uint8_t> buffer;
    bool isInterpreted = false;
    bool isTransformed = false;
};

PipelineContext* core_read_and_interpret(const std::vector<uint8_t>& raw_data) {
    auto* ctx = new PipelineContext();
    // Simulate interpreting the low-level format
    ctx->buffer = raw_data; 
    ctx->isInterpreted = true;
    return ctx;
}

void core_apply_rules(PipelineContext* ctx) {
    if (ctx && ctx->isInterpreted) {
        // Simulate applying transformation rules (e.g., bitwise NOT operation for demonstration)
        for (auto& byte : ctx->buffer) {
            byte = ~byte; 
        }
        ctx->isTransformed = true;
    }
}

std::vector<uint8_t> core_generate_output(PipelineContext* ctx) {
    if (ctx && ctx->isTransformed) {
        // Return the final transformed payload
        return ctx->buffer;
    }
    return std::vector<uint8_t>(); // Return empty on failure
}
// -------------------------------------------------

extern "C" {

// 1. JNI Wrapper for Reading and Interpreting
JNIEXPORT jlong JNICALL
Java_com_example_pipeline_DataTransformationPipeline_readAndInterpret(JNIEnv *env, jobject thiz, jbyteArray rawData) {
    if (rawData == nullptr) return 0;

    // Get the length of the Java byte array
    jsize length = env->GetArrayLength(rawData);
    
    // Copy the Java byte array into a C++ std::vector
    std::vector<uint8_t> cppBuffer(length);
    env->GetByteArrayRegion(rawData, 0, length, reinterpret_cast<jbyte*>(cppBuffer.data()));

    // Call the core C++ interpretation logic
    PipelineContext* ctx = core_read_and_interpret(cppBuffer);

    // Return the memory pointer to Java as a long handle
    return reinterpret_cast<jlong>(ctx);
}

// 2. JNI Wrapper for Applying Transformation Rules
JNIEXPORT void JNICALL
Java_com_example_pipeline_DataTransformationPipeline_applyTransformationRules(JNIEnv *env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<PipelineContext*>(handle);
    
    if (ctx != nullptr) {
        core_apply_rules(ctx);
    }
}

// 3. JNI Wrapper for Generating Output
JNIEXPORT jbyteArray JNICALL
Java_com_example_pipeline_DataTransformationPipeline_generateTransformedOutput(JNIEnv *env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<PipelineContext*>(handle);
    
    if (ctx == nullptr) return nullptr;

    // Get the transformed output from core logic
    std::vector<uint8_t> outputData = core_generate_output(ctx);

    // Create a new Java byte array
    jbyteArray javaOutput = env->NewByteArray(outputData.size());
    if (javaOutput == nullptr) {
        return nullptr; // Out of memory
    }

    // Copy the C++ std::vector data back into the Java byte array
    env->SetByteArrayRegion(javaOutput, 0, outputData.size(), reinterpret_cast<const jbyte*>(outputData.data()));

    return javaOutput;
}

// 4. JNI Wrapper for Memory Cleanup
JNIEXPORT void JNICALL
Java_com_example_pipeline_DataTransformationPipeline_cleanupPipeline(JNIEnv *env, jobject thiz, jlong handle) {
    auto* ctx = reinterpret_cast<PipelineContext*>(handle);
    
    // Safely delete the allocated C++ structure
    if (ctx != nullptr) {
        delete ctx;
    }
}

}