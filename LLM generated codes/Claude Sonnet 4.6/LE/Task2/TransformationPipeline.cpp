/**
 * JNI implementation of TransformationPipeline.java
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │  Stage 1 – nativeRead                                        │
 * │    Parses the raw format tag and payload into an IR.         │
 * │    IR wire format: 1-byte version | 1-byte count | N×values  │
 * │                                                              │
 * │  Stage 2 – nativeTransform                                   │
 * │    Applies two predefined rules:                             │
 * │      R1 – multiply every value by 2 (scale rule)            │
 * │      R2 – XOR every value with 0xAA (obfuscation rule)      │
 * │                                                              │
 * │  Stage 3 – nativeGenerate                                    │
 * │    Builds a PipelineResult Java object:                      │
 * │      outputFormat = "TRANSFORMED"                            │
 * │      data         = transformed bytes                        │
 * │      metadata     = "rules=R1,R2;count=N"                   │
 * └─────────────────────────────────���────────────────────────────┘
 *
 * Swap the wire format, rules, and output encoding to match real requirements.
 */

#include <jni.h>

#include <cstdint>
#include <cstring>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

// ─────────────────────────────────────────────────────────────────────────── //
//  IR wire-format constants                                                   //
// ─────────────────────────────────────────────────────────────────────────── //
static constexpr uint8_t IR_VERSION = 0x01;

// ─────────────────────────────────────────────────────────────────────────── //
//  Internal helpers                                                            //
// ─────────────────────────────────────────────────────────────────────────── //
namespace {

// ── JNI type conversions ────────────────────────────────────────────────── //

std::vector<uint8_t> jbyteArrayToVec(JNIEnv* env, jbyteArray arr) {
    jsize len = env->GetArrayLength(arr);
    jbyte* raw = env->GetByteArrayElements(arr, nullptr);
    std::vector<uint8_t> vec(reinterpret_cast<uint8_t*>(raw),
                              reinterpret_cast<uint8_t*>(raw) + len);
    env->ReleaseByteArrayElements(arr, raw, JNI_ABORT);
    return vec;
}

jbyteArray vecToJbyteArray(JNIEnv* env, const std::vector<uint8_t>& vec) {
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(vec.size()));
    if (arr && !vec.empty()) {
        env->SetByteArrayRegion(arr, 0, static_cast<jsize>(vec.size()),
                                reinterpret_cast<const jbyte*>(vec.data()));
    }
    return arr;
}

std::string jstringToString(JNIEnv* env, jstring js) {
    const char* chars = env->GetStringUTFChars(js, nullptr);
    std::string str(chars ? chars : "");
    if (chars) env->ReleaseStringUTFChars(js, chars);
    return str;
}

// ── Exception helpers ────────────────────────────────────────────────────── //

/**
 * Throws a Java PipelineException with the given stage and message.
 * Returns a sentinel so callers can: return throwPipelineEx(..., nullptr);
 */
template<typename T>
T throwPipelineEx(JNIEnv* env,
                  const char* stageFieldName,  // "READ", "TRANSFORM", "GENERATE"
                  const std::string& message,
                  T sentinel)
{
    // Resolve PipelineException class
    jclass exClass = env->FindClass("PipelineException");
    if (!exClass) return sentinel; // FindClass already threw NoClassDefFoundError

    // Resolve PipelineException$Stage enum
    jclass stageClass = env->FindClass("PipelineException$Stage");
    if (!stageClass) return sentinel;

    // Get the static Stage enum field
    jfieldID stageFieldID = env->GetStaticFieldID(
        stageClass, stageFieldName, "LPipelineException$Stage;");
    if (!stageFieldID) return sentinel;

    jobject stageObj = env->GetStaticObjectField(stageClass, stageFieldID);
    if (!stageObj) return sentinel;

    // Find PipelineException(Stage, String) constructor
    jmethodID ctor = env->GetMethodID(
        exClass, "<init>", "(LPipelineException$Stage;Ljava/lang/String;)V");
    if (!ctor) return sentinel;

    jstring jmsg = env->NewStringUTF(message.c_str());
    jobject ex   = env->NewObject(exClass, ctor, stageObj, jmsg);
    if (ex) env->Throw(static_cast<jthrowable>(ex));

    return sentinel;
}

} // anonymous namespace


// ─────────────────────────────────────────────────────────────────────────── //
//  Stage 1 – nativeRead                                                       //
// ─────────────────────────────────────────────────────────────────────────── //
/**
 * Reads and interprets a low-level data format.
 *
 * Supported formats:
 *   "HEX"    – payload bytes treated as raw hex values
 *   "BASE64" – payload decoded (simple 1-byte increment demo, not real Base64)
 *   "BINARY" – payload accepted as-is
 *
 * IR layout:  [ IR_VERSION (1B) | count (1B) | value₀ | value₁ | … ]
 */
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_TransformationPipeline_nativeRead(JNIEnv* env,
                                        jobject  /*self*/,
                                        jstring  jformat,
                                        jbyteArray jpayload)
{
    std::string            format  = jstringToString(env, jformat);
    std::vector<uint8_t>   payload = jbyteArrayToVec(env, jpayload);

    if (payload.empty()) {
        return throwPipelineEx<jbyteArray>(
            env, "READ", "payload is empty", nullptr);
    }
    if (payload.size() > 255) {
        return throwPipelineEx<jbyteArray>(
            env, "READ",
            "payload exceeds maximum supported size of 255 bytes", nullptr);
    }

    std::vector<uint8_t> values;
    values.reserve(payload.size());

    if (format == "HEX") {
        // Treat each byte as a raw hex value (0x00–0xFF)
        for (uint8_t b : payload)
            values.push_back(b);

    } else if (format == "BASE64") {
        // Demonstration: increment each byte by 1 to simulate decoding
        for (uint8_t b : payload)
            values.push_back(static_cast<uint8_t>(b + 1));

    } else if (format == "BINARY") {
        // Pass through as-is
        values = payload;

    } else {
        return throwPipelineEx<jbyteArray>(
            env, "READ",
            "Unsupported format: '" + format + "'. Expected HEX, BASE64, or BINARY.",
            nullptr);
    }

    // Build IR: [ version | count | values… ]
    std::vector<uint8_t> ir;
    ir.reserve(2 + values.size());
    ir.push_back(IR_VERSION);
    ir.push_back(static_cast<uint8_t>(values.size()));
    ir.insert(ir.end(), values.begin(), values.end());

    return vecToJbyteArray(env, ir);
}


// ─────────────────────────────────────────────────────────────────────────── //
//  Stage 2 – nativeTransform                                                  //
// ─────────────────────────────────────────────────────────────────────────── //
/**
 * Applies predefined transformation rules to the IR.
 *
 * Rules applied in order:
 *   R1 – Scale  : value = value * 2  (mod 256)
 *   R2 – Encode : value = value XOR 0xAA
 *
 * Returns a new IR in the same [ version | count | values… ] layout.
 */
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_TransformationPipeline_nativeTransform(JNIEnv* env,
                                             jobject  /*self*/,
                                             jbyteArray jir)
{
    std::vector<uint8_t> ir = jbyteArrayToVec(env, jir);

    // Validate IR structure
    if (ir.size() < 2) {
        return throwPipelineEx<jbyteArray>(
            env, "TRANSFORM", "Intermediate representation is too short", nullptr);
    }
    if (ir[0] != IR_VERSION) {
        return throwPipelineEx<jbyteArray>(
            env, "TRANSFORM",
            "Unknown IR version: " + std::to_string(ir[0]), nullptr);
    }

    uint8_t count = ir[1];
    if (ir.size() != static_cast<size_t>(2 + count)) {
        return throwPipelineEx<jbyteArray>(
            env, "TRANSFORM",
            "IR length mismatch: header says " + std::to_string(count)
            + " values but found " + std::to_string(ir.size() - 2), nullptr);
    }

    std::vector<uint8_t> transformed;
    transformed.reserve(2 + count);
    transformed.push_back(IR_VERSION);
    transformed.push_back(count);

    for (size_t i = 2; i < ir.size(); ++i) {
        uint8_t v = ir[i];
        v = static_cast<uint8_t>((v * 2) & 0xFF); // R1: scale
        v = static_cast<uint8_t>(v ^ 0xAA);        // R2: XOR encode
        transformed.push_back(v);
    }

    return vecToJbyteArray(env, transformed);
}


// ──────────���──────────────────────────────────────────────────────────────── //
//  Stage 3 – nativeGenerate                                                   //
// ─────────────────────────────────────────────────────────────────────────── //
/**
 * Generates a PipelineResult Java object from the transformed IR.
 *
 * The PipelineResult is constructed directly via JNI:
 *   outputFormat = "TRANSFORMED"
 *   data         = the transformed value bytes (IR header stripped)
 *   metadata     = "rules=R1,R2;count=N"
 */
extern "C"
JNIEXPORT jobject JNICALL
Java_TransformationPipeline_nativeGenerate(JNIEnv* env,
                                            jobject  /*self*/,
                                            jbyteArray jtransformed)
{
    std::vector<uint8_t> ir = jbyteArrayToVec(env, jtransformed);

    // Validate
    if (ir.size() < 2 || ir[0] != IR_VERSION) {
        return throwPipelineEx<jobject>(
            env, "GENERATE", "Transformed IR is invalid or has wrong version", nullptr);
    }

    uint8_t count = ir[1];
    if (ir.size() != static_cast<size_t>(2 + count)) {
        return throwPipelineEx<jobject>(
            env, "GENERATE", "Transformed IR length mismatch", nullptr);
    }

    // Extract the value bytes (strip 2-byte header)
    std::vector<uint8_t> outputData(ir.begin() + 2, ir.end());

    // Build metadata string
    std::ostringstream meta;
    meta << "rules=R1,R2;count=" << static_cast<int>(count);

    // Locate PipelineResult class and its (String, byte[], String) constructor
    jclass resultClass = env->FindClass("PipelineResult");
    if (!resultClass) {
        return throwPipelineEx<jobject>(
            env, "GENERATE", "Cannot find PipelineResult class", nullptr);
    }

    jmethodID ctor = env->GetMethodID(
        resultClass, "<init>", "(Ljava/lang/String;[BLjava/lang/String;)V");
    if (!ctor) {
        return throwPipelineEx<jobject>(
            env, "GENERATE", "Cannot find PipelineResult constructor", nullptr);
    }

    // Build Java arguments
    jstring    jOutputFormat = env->NewStringUTF("TRANSFORMED");
    jbyteArray jData         = vecToJbyteArray(env, outputData);
    jstring    jMetadata     = env->NewStringUTF(meta.str().c_str());

    // Construct and return the PipelineResult object
    jobject result = env->NewObject(resultClass, ctor, jOutputFormat, jData, jMetadata);

    if (!result) {
        return throwPipelineEx<jobject>(
            env, "GENERATE", "Failed to instantiate PipelineResult", nullptr);
    }

    return result;
}