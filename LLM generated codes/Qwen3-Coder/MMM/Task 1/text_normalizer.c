#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <wchar.h>
#include <wctype.h>
#include "com_example_TextNormalizer.h"

/**
 * Unicode-aware whitespace check (covers common space characters)
 */
static int is_unicode_space(wchar_t c) {
    // Basic whitespace + Unicode space characters
    return (c == L' ' || c == L'\t' || c == L'\n' || c == L'\r' || 
            c == L'\f' || c == L'\v' || c == 0x00A0 || // Non-breaking space
            c == 0x2000 || c == 0x2001 || c == 0x2002 || // En space, Em space, etc.
            c == 0x2003 || c == 0x2004 || c == 0x2005 ||
            c == 0x2006 || c == 0x2007 || c == 0x2008 ||
            c == 0x2009 || c == 0x200A || c == 0x202F ||
            c == 0x205F || c == 0x3000);
}

/**
 * Trim leading/trailing whitespace from wide string
 * Returns new allocated string (caller must free)
 */
static wchar_t* trim_whitespace(const wchar_t* input, size_t len, size_t* out_len) {
    if (!input || len == 0) {
        *out_len = 0;
        return NULL;
    }

    // Find first non-whitespace
    size_t start = 0;
    while (start < len && is_unicode_space(input[start])) {
        start++;
    }

    // Find last non-whitespace
    size_t end = len;
    while (end > start && is_unicode_space(input[end - 1])) {
        end--;
    }

    size_t result_len = end - start;
    if (result_len == 0) {
        *out_len = 0;
        return NULL;
    }

    wchar_t* result = (wchar_t*)malloc((result_len + 1) * sizeof(wchar_t));
    if (!result) return NULL;

    wcsncpy(result, input + start, result_len);
    result[result_len] = L'\0';
    *out_len = result_len;
    return result;
}

/**
 * Collapse consecutive whitespace characters to single space
 * Returns new allocated string (caller must free)
 */
static wchar_t* collapse_whitespace(const wchar_t* input, size_t len, size_t* out_len) {
    if (!input || len == 0) {
        *out_len = 0;
        return NULL;
    }

    wchar_t* result = (wchar_t*)malloc((len + 1) * sizeof(wchar_t));
    if (!result) return NULL;

    size_t pos = 0;
    int in_whitespace = 0;

    for (size_t i = 0; i < len; i++) {
        if (is_unicode_space(input[i])) {
            if (!in_whitespace) {
                result[pos++] = L' ';
                in_whitespace = 1;
            }
            // Skip additional whitespace characters
        } else {
            result[pos++] = input[i];
            in_whitespace = 0;
        }
    }

    // Trim trailing space if we ended with whitespace
    if (pos > 0 && result[pos - 1] == L' ') {
        pos--;
    }

    result[pos] = L'\0';
    *out_len = pos;
    return result;
}

/**
 * Convert wide string to lowercase using Unicode case folding
 * Returns new allocated string (caller must free)
 */
static wchar_t* to_lowercase(const wchar_t* input, size_t len, size_t* out_len) {
    if (!input || len == 0) {
        *out_len = 0;
        return NULL;
    }

    wchar_t* result = (wchar_t*)malloc((len + 1) * sizeof(wchar_t));
    if (!result) return NULL;

    for (size_t i = 0; i < len; i++) {
        // Use towlower for Unicode-aware case conversion
        result[i] = towlower(input[i]);
    }
    result[len] = L'\0';
    *out_len = len;
    return result;
}

/*
 * Class:     com_example_TextNormalizer
 * Method:    normalize
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_example_TextNormalizer_normalize
  (JNIEnv *env, jobject thisObj, jstring input) {
    
    // Handle null input
    if (input == NULL) {
        return NULL;
    }

    // Get string length and characters as UTF-32 (wchar_t) for proper Unicode handling
    const jchar* utf16_chars = (*env)->GetStringChars(env, input, NULL);
    if (utf16_chars == NULL) {
        // Out of memory or exception pending
        return NULL;
    }
    
    jsize utf16_len = (*env)->GetStringLength(env, input);
    
    // Convert UTF-16 to wide characters (wchar_t)
    // Note: This assumes wchar_t is 4 bytes (Linux/macOS). For Windows (2-byte wchar_t),
    // a more complex conversion would be needed. For simplicity and portability,
    // we'll use a direct cast which works for BMP characters.
    size_t wcs_len = (size_t)utf16_len;
    wchar_t* wcs = (wchar_t*)malloc((wcs_len + 1) * sizeof(wchar_t));
    if (!wcs) {
        (*env)->ReleaseStringChars(env, input, utf16_chars);
        return NULL;
    }
    
    // Simple conversion (works for BMP characters)
    for (jsize i = 0; i < utf16_len; i++) {
        wcs[i] = (wchar_t)utf16_chars[i];
    }
    wcs[wcs_len] = L'\0';
    
    (*env)->ReleaseStringChars(env, input, utf16_chars);

    // Step 1: Trim leading/trailing whitespace
    size_t trimmed_len = 0;
    wchar_t* trimmed = trim_whitespace(wcs, wcs_len, &trimmed_len);
    free(wcs);
    wcs = NULL;
    
    if (trimmed_len == 0) {
        if (trimmed) free(trimmed);
        return (*env)->NewStringUTF(env, ""); // Return empty string
    }

    // Step 2: Collapse internal whitespace
    size_t collapsed_len = 0;
    wchar_t* collapsed = collapse_whitespace(trimmed, trimmed_len, &collapsed_len);
    free(trimmed);
    
    if (collapsed_len == 0) {
        if (collapsed) free(collapsed);
        return (*env)->NewStringUTF(env, "");
    }

    // Step 3: Convert to lowercase
    size_t lower_len = 0;
    wchar_t* lower = to_lowercase(collapsed, collapsed_len, &lower_len);
    free(collapsed);

    if (!lower || lower_len == 0) {
        if (lower) free(lower);
        return (*env)->NewStringUTF(env, "");
    }

    // Convert back to UTF-16 for Java
    jchar* result_utf16 = (jchar*)malloc((lower_len + 1) * sizeof(jchar));
    if (!result_utf16) {
        free(lower);
        return NULL;
    }
    
    for (size_t i = 0; i < lower_len; i++) {
        result_utf16[i] = (jchar)lower[i];
    }
    result_utf16[lower_len] = 0;
    
    free(lower);

    // Create Java string from UTF-16 characters
    jstring result = (*env)->NewString(env, result_utf16, (jsize)lower_len);
    free(result_utf16);
    
    return result;
}