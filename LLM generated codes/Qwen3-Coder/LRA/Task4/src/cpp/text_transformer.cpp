// src/cpp/text_transformer.cpp
#include "../headers/text_transformer.h"
#include <jni.h>
#include <string>
#include <cctype>
#include <vector>

// Global reference for String class
static jclass g_stringClass = nullptr;

// Helper function to convert C++ string to uppercase
std::string toUppercase(const std::string& input) {
    std::string result = input;
    for (char& c : result) {
        c = std::toupper(static_cast<unsigned char>(c));
    }
    return result;
}

// Helper function to convert Java String to C++ string
std::string jstringToString(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return "";
    
    const char* cstr = env->GetStringUTFChars(jstr, nullptr);
    if (cstr == nullptr) return ""; // OutOfMemoryError thrown
    
    std::string result(cstr);
    env->ReleaseStringUTFChars(jstr, cstr);
    return result;
}

// Helper function to convert C++ string to Java String
jstring stringToJstring(JNIEnv* env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// Initialize global references
void initializeGlobalReferences(JNIEnv* env) {
    if (g_stringClass == nullptr) {
        jclass tempClass = env->FindClass("java/lang/String");
        if (tempClass != nullptr) {
            g_stringClass = (jclass)env->NewGlobalRef(tempClass);
            env->DeleteLocalRef(tempClass);
        }
    }
}

// Main function to transform array of strings to uppercase
JNIEXPORT jobjectArray JNICALL Java_TextTransformer_transformToUppercase
  (JNIEnv *env, jobject thiz, jobjectArray inputStrings) {
    
    jsize arrayLength = env->GetArrayLength(inputStrings);
    
    if (arrayLength == 0) {
        // Return empty array
        return env->NewObjectArray(0, g_stringClass, nullptr);
    }
    
    // Initialize global references
    initializeGlobalReferences(env);
    
    if (g_stringClass == nullptr) {
        return nullptr;
    }
    
    // Create output array
    jobjectArray resultArray = env->NewObjectArray(arrayLength, g_stringClass, nullptr);
    if (resultArray == nullptr) {
        // OutOfMemoryError thrown
        return nullptr;
    }
    
    // Process each string in the input array
    for (jsize i = 0; i < arrayLength; i++) {
        jstring inputString = (jstring)env->GetObjectArrayElement(inputStrings, i);
        
        if (inputString == nullptr) {
            // Handle null element - keep it as null in result
            env->SetObjectArrayElement(resultArray, i, nullptr);
        } else {
            // Convert Java string to C++ string
            std::string inputCppStr = jstringToString(env, inputString);
            
            // Transform to uppercase using native library function
            std::string uppercaseStr = toUppercase(inputCppStr);
            
            // Convert back to Java string
            jstring resultString = stringToJstring(env, uppercaseStr);
            
            if (resultString != nullptr) {
                // Set the transformed string in the result array
                env->SetObjectArrayElement(resultArray, i, resultString);
                
                // Clean up local reference
                env->DeleteLocalRef(resultString);
            } else {
                // If allocation failed, set null in result array
                env->SetObjectArrayElement(resultArray, i, nullptr);
            }
            
            // Clean up local reference
            env->DeleteLocalRef(inputString);
        }
        
        // Check for pending exceptions and handle them
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            // Continue processing remaining elements
        }
    }
    
    return resultArray;
}

// Alternative implementation with optimized memory usage for very large arrays
JNIEXPORT jobjectArray JNICALL Java_TextTransformer_transformToUppercaseOptimized
  (JNIEnv *env, jobject thiz, jobjectArray inputStrings) {
    
    jsize arrayLength = env->GetArrayLength(inputStrings);
    
    if (arrayLength == 0) {
        return env->NewObjectArray(0, g_stringClass, nullptr);
    }
    
    initializeGlobalReferences(env);
    
    if (g_stringClass == nullptr) {
        return nullptr;
    }
    
    // Pre-allocate result array
    jobjectArray resultArray = env->NewObjectArray(arrayLength, g_stringClass, nullptr);
    if (resultArray == nullptr) {
        return nullptr;
    }
    
    // Vector to hold temporary C++ strings (to reduce JNI calls)
    std::vector<std::string> tempResults;
    tempResults.reserve(arrayLength);
    
    // First pass: collect all strings and convert to uppercase
    for (jsize i = 0; i < arrayLength; i++) {
        jstring inputString = (jstring)env->GetObjectArrayElement(inputStrings, i);
        
        if (inputString == nullptr) {
            tempResults.emplace_back(""); // Placeholder for null
        } else {
            std::string inputCppStr = jstringToString(env, inputString);
            std::string uppercaseStr = toUppercase(inputCppStr);
            tempResults.push_back(std::move(uppercaseStr));
            
            env->DeleteLocalRef(inputString);
        }
    }
    
    // Second pass: convert back to Java strings and populate result array
    for (jsize i = 0; i < arrayLength; i++) {
        if (tempResults[i].empty() && env->GetObjectArrayElement(inputStrings, i) == nullptr) {
            // Original was null, keep as null
            env->SetObjectArrayElement(resultArray, i, nullptr);
        } else {
            jstring resultString = stringToJstring(env, tempResults[i]);
            if (resultString != nullptr) {
                env->SetObjectArrayElement(resultArray, i, resultString);
                env->DeleteLocalRef(resultString);
            } else {
                env->SetObjectArrayElement(resultArray, i, nullptr);
            }
        }
    }
    
    return resultArray;
}

// Cleanup function
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_OK) {
        if (g_stringClass != nullptr) {
            env->DeleteGlobalRef(g_stringClass);
            g_stringClass = nullptr;
        }
    }
}