#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>

// --- Mocking the Core C Business Logic ---
// In a real application, these would map to your existing C library headers.
typedef struct {
    char *parsed_data;
    int is_processed;
} ProcessingState;

ProcessingState* core_parse(const char* input) {
    ProcessingState *state = (ProcessingState *)malloc(sizeof(ProcessingState));
    if (!state) return NULL;
    
    // Simulate parsing by allocating memory and copying the string
    size_t len = strlen(input) + 20;
    state->parsed_data = (char *)malloc(len);
    if (!state->parsed_data) {
        free(state);
        return NULL;
    }
    snprintf(state->parsed_data, len, "Parsed[%s]", input);
    state->is_processed = 0;
    
    return state;
}

void core_process(ProcessingState *state) {
    if (state && state->parsed_data) {
        // Simulate processing elements
        size_t len = strlen(state->parsed_data) + 20;
        char *new_data = (char *)realloc(state->parsed_data, len);
        if (new_data) {
            state->parsed_data = new_data;
            strcat(state->parsed_data, " -> Processed");
            state->is_processed = 1;
        }
    }
}

char* core_generate(ProcessingState *state) {
    if (state && state->is_processed) {
        size_t len = strlen(state->parsed_data) + 20;
        char *output = (char *)malloc(len);
        if (output) {
            snprintf(output, len, "%s -> FinalOutput", state->parsed_data);
            return output;
        }
    }
    return NULL;
}

void core_free(ProcessingState *state) {
    if (state) {
        if (state->parsed_data) {
            free(state->parsed_data);
        }
        free(state);
    }
}
// -----------------------------------------

// 1. JNI Wrapper for Parsing
JNIEXPORT jlong JNICALL
Java_com_example_processing_TextProcessingOperation_parseInputText(JNIEnv *env, jobject thiz, jstring input) {
    if (input == NULL) return 0;

    // Convert Java jstring to C char array. Note the C syntax: (*env)->...
    const char *c_input = (*env)->GetStringUTFChars(env, input, NULL);
    if (c_input == NULL) return 0; // Out of memory

    // Call core C logic
    ProcessingState *state = core_parse(c_input);

    // Release the JNI string resource
    (*env)->ReleaseStringUTFChars(env, input, c_input);

    // Cast the C pointer to jlong using intptr_t for 32/64-bit safety
    return (jlong)(intptr_t)state;
}

// 2. JNI Wrapper for Processing Elements
JNIEXPORT void JNICALL
Java_com_example_processing_TextProcessingOperation_processElements(JNIEnv *env, jobject thiz, jlong handle) {
    // Cast the jlong back to the C struct pointer
    ProcessingState *state = (ProcessingState *)(intptr_t)handle;
    
    if (state != NULL) {
        core_process(state);
    }
}

// 3. JNI Wrapper for Generating Output
JNIEXPORT jstring JNICALL
Java_com_example_processing_TextProcessingOperation_generateOutput(JNIEnv *env, jobject thiz, jlong handle) {
    ProcessingState *state = (ProcessingState *)(intptr_t)handle;
    
    if (state == NULL) return NULL;

    // Get the dynamically allocated result string from the core C logic
    char *result_c_str = core_generate(state);
    if (result_c_str == NULL) {
        return (*env)->NewStringUTF(env, "Error: Generation failed");
    }

    // Convert C string back to Java String
    jstring java_result = (*env)->NewStringUTF(env, result_c_str);
    
    // Free the temporarily allocated C string from core_generate
    free(result_c_str);
    
    return java_result;
}

// 4. JNI Wrapper for Memory Management (Cleanup)
JNIEXPORT void JNICALL
Java_com_example_processing_TextProcessingOperation_freeNativeState(JNIEnv *env, jobject thiz, jlong handle) {
    ProcessingState *state = (ProcessingState *)(intptr_t)handle;
    
    // Safely free the allocated C struct and its contents
    if (state != NULL) {
        core_free(state);
    }
}