/**
 * JNI implementation for DataProcessor.java
 *
 * Pipeline:
 *   Java_DataProcessor_parse()      – tokenises "key=value;..." pairs
 *   Java_DataProcessor_normalize()  – converts every value to an integer
 *                                     and clamps it to [0, 100]
 *   Java_DataProcessor_compute()    – returns the sum as a decimal string
 *
 * The internal wire format between the three stages is a plain
 * newline-delimited list of decimal integers, e.g. "10\n20\n30\n".
 * Swap this for protobuf / flatbuffers / msgpack as needed.
 */

#include <jni.h>

#include <algorithm>   // std::clamp
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

// ------------------------------------------------------------------ //
//  Internal helpers (not visible outside this translation unit)       //
// ------------------------------------------------------------------ //

namespace {

// Convert a jbyteArray to std::string without a NUL-terminator assumption.
std::string jbyteArrayToString(JNIEnv* env, jbyteArray arr) {
    jsize        len  = env->GetArrayLength(arr);
    jbyte*       buf  = env->GetByteArrayElements(arr, nullptr);
    std::string  str(reinterpret_cast<char*>(buf), static_cast<size_t>(len));
    env->ReleaseByteArrayElements(arr, buf, JNI_ABORT);
    return str;
}

// Convert std::string to a newly allocated jbyteArray.
jbyteArray stringToJbyteArray(JNIEnv* env, const std::string& str) {
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(str.size()));
    if (arr) {
        env->SetByteArrayRegion(arr, 0, static_cast<jsize>(str.size()),
                                reinterpret_cast<const jbyte*>(str.data()));
    }
    return arr;
}

// Raise a Java IllegalArgumentException and return a sentinel value.
template<typename T>
T throwIAE(JNIEnv* env, const std::string& msg, T sentinel) {
    jclass cls = env->FindClass("java/lang/IllegalArgumentException");
    if (cls) env->ThrowNew(cls, msg.c_str());
    return sentinel;
}

} // anonymous namespace

// ------------------------------------------------------------------ //
//  Stage 1 – parse                                                    //
// ------------------------------------------------------------------ //
/**
 * Parses "key=value;key=value;..." into a newline-separated list of
 * raw value strings, preserving order.
 *
 * Example input : "key1=10;key2=20;key3=30"
 * Example output: "10\n20\n30\n"
 */
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_DataProcessor_parse(JNIEnv* env, jobject /*self*/, jstring rawInput) {

    const char* chars = env->GetStringUTFChars(rawInput, nullptr);
    if (!chars) return throwIAE<jbyteArray>(env, "GetStringUTFChars failed", nullptr);

    std::string input(chars);
    env->ReleaseStringUTFChars(rawInput, chars);

    // Tokenise by ';', then split each token on '='
    std::ostringstream out;
    std::istringstream stream(input);
    std::string        token;

    while (std::getline(stream, token, ';')) {
        if (token.empty()) continue;

        auto eq = token.find('=');
        if (eq == std::string::npos) {
            return throwIAE<jbyteArray>(
                env, "Malformed token (missing '='): " + token, nullptr);
        }
        // Emit the value part
        out << token.substr(eq + 1) << '\n';
    }

    return stringToJbyteArray(env, out.str());
}

// ------------------------------------------------------------------ //
//  Stage 2 – normalize                                                //
// ------------------------------------------------------------------ //
/**
 * Converts each raw value string to an integer and clamps it to [0, 100].
 *
 * Example input : "10\n200\n-5\n"
 * Example output: "10\n100\n0\n"
 */
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_DataProcessor_normalize(JNIEnv* env, jobject /*self*/, jbyteArray parsed) {

    std::string       data = jbyteArrayToString(env, parsed);
    std::istringstream stream(data);
    std::ostringstream out;
    std::string        line;

    while (std::getline(stream, line)) {
        if (line.empty()) continue;

        try {
            int value = std::stoi(line);
            value = std::clamp(value, 0, 100);
            out << value << '\n';
        } catch (const std::exception& ex) {
            return throwIAE<jbyteArray>(
                env, std::string("Cannot normalize value '") + line + "': " + ex.what(),
                nullptr);
        }
    }

    return stringToJbyteArray(env, out.str());
}

// ------------------------------------------------------------------ //
//  Stage 3 – compute                                                  //
// ------------------------------------------------------------------ //
/**
 * Sums all normalized integer values and returns the result as a
 * decimal string.
 *
 * Example input : "10\n100\n0\n"
 * Example output: "110"
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_DataProcessor_compute(JNIEnv* env, jobject /*self*/, jbyteArray normalized) {

    std::string        data = jbyteArrayToString(env, normalized);
    std::istringstream stream(data);
    std::string        line;
    long long          sum = 0;

    while (std::getline(stream, line)) {
        if (line.empty()) continue;

        try {
            sum += std::stoll(line);
        } catch (const std::exception& ex) {
            return throwIAE<jstring>(
                env, std::string("Cannot compute from value '") + line + "': " + ex.what(),
                nullptr);
        }
    }

    return env->NewStringUTF(std::to_string(sum).c_str());
}