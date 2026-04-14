/*
 * text_transformer.cpp
 *
 * JNI native implementation of TextTransformer.toUpperCaseBatch().
 *
 * Iterates a Java String[] array, converts each element to uppercase
 * using one of two backends (selected at compile time):
 *
 *   - USE_ICU=1  : Unicode-correct uppercase via ICU u_strToUpper()
 *                  Handles full Unicode, locale-sensitive folding (ß→SS etc.)
 *
 *   - USE_ICU=0  : Portable ASCII-safe uppercase via std::toupper()
 *                  Fast and dependency-free; correct for ASCII / Latin-1.
 *                  Non-ASCII bytes are passed through unchanged.
 *
 * Build: see CMakeLists.txt
 */

#include <jni.h>

#include <string>
#include <vector>
#include <cctype>
#include <algorithm>
#include <cstring>
#include <stdexcept>

#if USE_ICU
#  include <unicode/ustring.h>   /* u_strToUpper, u_strFromUTF8, u_strToUTF8 */
#  include <unicode/uenum.h>
#  include <unicode/uloc.h>
#endif

/* ============================================================================
 * Helper: throw a named Java exception
 * ============================================================================ */
static void throw_java_exception(JNIEnv* env,
                                 const char* class_name,
                                 const char* message)
{
    jclass cls = env->FindClass(class_name);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
}

/* ============================================================================
 * Backend A — ICU-based Unicode uppercase (USE_ICU=1)
 *
 * Converts a UTF-8 std::string to uppercase using ICU's u_strToUpper,
 * which correctly handles:
 *   - Full Unicode case folding  (ß → SS in German locale)
 *   - Supplementary code points  (emoji, CJK, etc.)
 *   - Locale-sensitive rules     (Turkish dotted I, etc.)
 *
 * Returns the uppercased UTF-8 string, or throws std::runtime_error.
 * ============================================================================ */
#if USE_ICU

static std::string uppercase_icu(const std::string& utf8_input,
                                  const char*         locale = "")
{
    UErrorCode status = U_ZERO_ERROR;

    /* ── Step 1: UTF-8 → UTF-16 (ICU's native representation) ── */
    int32_t utf16_len = 0;
    u_strFromUTF8(nullptr, 0, &utf16_len,
                  utf8_input.c_str(),
                  static_cast<int32_t>(utf8_input.size()),
                  &status);

    if (status != U_BUFFER_OVERFLOW_ERROR && U_FAILURE(status)) {
        throw std::runtime_error(
            std::string("ICU u_strFromUTF8 preflight failed: ") + u_errorName(status));
    }
    status = U_ZERO_ERROR;

    std::vector<UChar> utf16_src(static_cast<size_t>(utf16_len) + 1);
    u_strFromUTF8(utf16_src.data(),
                  static_cast<int32_t>(utf16_src.size()),
                  nullptr,
                  utf8_input.c_str(),
                  static_cast<int32_t>(utf8_input.size()),
                  &status);

    if (U_FAILURE(status)) {
        throw std::runtime_error(
            std::string("ICU u_strFromUTF8 failed: ") + u_errorName(status));
    }

    /* ── Step 2: Uppercase in UTF-16 ── */
    int32_t upper_len = 0;
    u_strToUpper(nullptr, 0, utf16_src.data(), utf16_len, locale, &status);

    if (status != U_BUFFER_OVERFLOW_ERROR && U_FAILURE(status)) {
        throw std::runtime_error(
            std::string("ICU u_strToUpper preflight failed: ") + u_errorName(status));
    }
    status = U_ZERO_ERROR;

    /* ICU may expand (ß→SS increases length by 1 code unit) — use preflight size */
    std::vector<UChar> utf16_upper(static_cast<size_t>(upper_len) + 1);
    u_strToUpper(utf16_upper.data(),
                 static_cast<int32_t>(utf16_upper.size()),
                 utf16_src.data(),
                 utf16_len,
                 locale,
                 &status);

    if (U_FAILURE(status)) {
        throw std::runtime_error(
            std::string("ICU u_strToUpper failed: ") + u_errorName(status));
    }

    /* ── Step 3: UTF-16 → UTF-8 ── */
    int32_t utf8_len = 0;
    u_strToUTF8(nullptr, 0, &utf8_len,
                utf16_upper.data(), upper_len, &status);

    if (status != U_BUFFER_OVERFLOW_ERROR && U_FAILURE(status)) {
        throw std::runtime_error(
            std::string("ICU u_strToUTF8 preflight failed: ") + u_errorName(status));
    }
    status = U_ZERO_ERROR;

    std::string result(static_cast<size_t>(utf8_len), '\0');
    u_strToUTF8(result.data(),
                static_cast<int32_t>(result.size()) + 1,
                nullptr,
                utf16_upper.data(),
                upper_len,
                &status);

    if (U_FAILURE(status)) {
        throw std::runtime_error(
            std::string("ICU u_strToUTF8 failed: ") + u_errorName(status));
    }

    return result;
}

#endif // USE_ICU

/* ============================================================================
 * Backend B — Portable ASCII/Latin-1 uppercase (USE_ICU=0, default)
 *
 * Uses std::toupper() with the classic locale for reliable ASCII handling.
 * Non-ASCII bytes (>127) are passed through unchanged.
 * For full Unicode correctness, compile with USE_ICU=1 instead.
 * ============================================================================ */
static std::string uppercase_ascii(const std::string& input)
{
    std::string result(input.size(), '\0');

    std::transform(
        input.cbegin(), input.cend(),
        result.begin(),
        [](unsigned char ch) -> char {
            /*
             * Cast to unsigned char before toupper is MANDATORY.
             * Passing a signed char with value > 127 to toupper
             * is undefined behaviour in the C and C++ standards.
             */
            return static_cast<char>(std::toupper(ch));
        });

    return result;
}

/* ============================================================================
 * Unified uppercase dispatcher
 * Selects the appropriate backend at compile time.
 * ============================================================================ */
static std::string to_uppercase(const std::string& input)
{
#if USE_ICU
    return uppercase_icu(input);   // Unicode-correct
#else
    return uppercase_ascii(input); // ASCII-safe, portable
#endif
}

/* ============================================================================
 * JNI_OnLoad
 * ============================================================================ */
extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* /*vm*/, void* /*reserved*/) {
    return JNI_VERSION_1_8;
}

/* ============================================================================
 * Main JNI entry point
 *
 * Java signature:
 *   public native String[] toUpperCaseBatch(String[] inputs);
 * ============================================================================ */
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_text_TextTransformer_toUpperCaseBatch(
        JNIEnv*      env,
        jobject      /*thiz*/,
        jobjectArray jInputs)
{
    /* ------------------------------------------------------------------
     * 1. Validate input
     * ------------------------------------------------------------------ */
    if (jInputs == nullptr) {
        throw_java_exception(env,
            "java/lang/IllegalArgumentException",
            "inputs array must not be null");
        return nullptr;
    }

    jsize length = env->GetArrayLength(jInputs);

    /* ------------------------------------------------------------------
     * 2. Resolve java.lang.String class for result array construction
     * ------------------------------------------------------------------ */
    jclass stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr) {
        /* NoClassDefFoundError thrown by JVM */
        return nullptr;
    }

    /* ------------------------------------------------------------------
     * 3. Allocate the result array upfront (same length as input)
     *    Initialised with null elements — only overwritten for
     *    non-null inputs; null inputs remain null in the output.
     * ------------------------------------------------------------------ */
    jobjectArray jResults = env->NewObjectArray(length, stringClass, nullptr);
    env->DeleteLocalRef(stringClass);

    if (jResults == nullptr) {
        /* OutOfMemoryError thrown by JVM */
        return nullptr;
    }

    /* ------------------------------------------------------------------
     * 4. Iterate and transform each element
     * ------------------------------------------------------------------ */
    for (jsize i = 0; i < length; ++i) {

        jobject jElement = env->GetObjectArrayElement(jInputs, i);

        /* Propagate pending JVM exception immediately */
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jResults);
            return nullptr;
        }

        /* Preserve null elements as null in the output */
        if (jElement == nullptr) {
            /*
             * The result array was already initialised with null elements,
             * so no explicit SetObjectArrayElement call is needed here.
             */
            continue;
        }

        jstring jInputStr = static_cast<jstring>(jElement);

        /* ---- Convert jstring → UTF-8 std::string ----------------------- */
        const char* utf8_chars = env->GetStringUTFChars(jInputStr, nullptr);
        if (utf8_chars == nullptr) {
            /*
             * GetStringUTFChars returns nullptr only when it cannot
             * allocate memory — OutOfMemoryError is already pending.
             */
            env->DeleteLocalRef(jInputStr);
            env->DeleteLocalRef(jResults);
            return nullptr;
        }

        /*
         * Capture length BEFORE ReleaseStringUTFChars invalidates the ptr.
         * Use GetStringUTFLength (byte count) not GetStringLength (char16 count).
         */
        jsize   byte_len  = env->GetStringUTFLength(jInputStr);
        std::string input_str(utf8_chars, static_cast<size_t>(byte_len));

        env->ReleaseStringUTFChars(jInputStr, utf8_chars);

        /* ---- Perform uppercase transformation --------------------------- */
        std::string upper_str;
        try {
            upper_str = to_uppercase(input_str);
        } catch (const std::exception& ex) {
            /*
             * Conversion error (only possible with ICU backend).
             * Marshal the C++ exception into a Java RuntimeException.
             */
            env->DeleteLocalRef(jInputStr);
            env->DeleteLocalRef(jResults);
            throw_java_exception(env, "java/lang/RuntimeException", ex.what());
            return nullptr;
        }

        /* ---- Convert result back to jstring ----------------------------- */
        jstring jResultStr = env->NewStringUTF(upper_str.c_str());
        if (jResultStr == nullptr) {
            /* OutOfMemoryError thrown by JVM */
            env->DeleteLocalRef(jInputStr);
            env->DeleteLocalRef(jResults);
            return nullptr;
        }

        /* ---- Write into the result array -------------------------------- */
        env->SetObjectArrayElement(jResults, i, jResultStr);

        if (env->ExceptionCheck()) {
            /* ArrayIndexOutOfBoundsException — should never happen */
            env->DeleteLocalRef(jResultStr);
            env->DeleteLocalRef(jInputStr);
            env->DeleteLocalRef(jResults);
            return nullptr;
        }

        /*
         * CRITICAL: Release local refs inside the loop.
         * JNI local ref table is typically limited to ~512 slots.
         * Large input arrays will overflow it without explicit cleanup.
         */
        env->DeleteLocalRef(jResultStr);
        env->DeleteLocalRef(jInputStr);
    }

    /* ------------------------------------------------------------------
     * 5. Return the fully populated result array
     * ------------------------------------------------------------------ */
    return jResults;
}