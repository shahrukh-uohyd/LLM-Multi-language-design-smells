#include <jni.h>
#include <ctype.h>
#include <string.h>
#include <stdlib.h>

JNIEXPORT jstring JNICALL
Java_NativeTextNormalizer_normalize(
        JNIEnv* env,
        jclass,
        jstring input) {

    if (input == NULL) {
        return NULL;
    }

    // Convert Java String → UTF-8 C string
    const char* utf =
        (*env)->GetStringUTFChars(env, input, NULL);
    if (utf == NULL) {
        return NULL;
    }

    size_t len = strlen(utf);

    // Trim leading whitespace
    const char* start = utf;
    while (*start && isspace((unsigned char)*start)) {
        start++;
    }

    // Trim trailing whitespace
    const char* end = utf + len;
    while (end > start && isspace((unsigned char)*(end - 1))) {
        end--;
    }

    size_t newLen = (size_t)(end - start);

    // Allocate buffer
    char* buffer = (char*)malloc(newLen + 1);
    if (buffer == NULL) {
        (*env)->ReleaseStringUTFChars(env, input, utf);
        return NULL;
    }

    // Copy and convert to uppercase
    for (size_t i = 0; i < newLen; i++) {
        buffer[i] = (char)toupper((unsigned char)start[i]);
    }
    buffer[newLen] = '\0';

    // Create Java String result
    jstring result = (*env)->NewStringUTF(env, buffer);

    // Cleanup
    free(buffer);
    (*env)->ReleaseStringUTFChars(env, input, utf);

    return result;
}
