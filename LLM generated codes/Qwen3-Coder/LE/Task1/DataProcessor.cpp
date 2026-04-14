#include "DataProcessor.h"
#include <iostream>
#include <sstream>
#include <algorithm>
#include <cctype>

// Global registry for managing native object handles
class HandleRegistry {
private:
    static std::vector<void*> handles;
    static pthread_mutex_t mutex; // For thread safety
    
public:
    static jlong registerHandle(void* ptr) {
        pthread_mutex_lock(&mutex);
        handles.push_back(ptr);
        jlong handle = reinterpret_cast<jlong>(ptr);
        pthread_mutex_unlock(&mutex);
        return handle;
    }
    
    static void* getHandle(jlong handle) {
        return reinterpret_cast<void*>(handle);
    }
    
    static void unregisterHandle(jlong handle) {
        pthread_mutex_lock(&mutex);
        void* ptr = reinterpret_cast<void*>(handle);
        auto it = std::find(handles.begin(), handles.end(), ptr);
        if (it != handles.end()) {
            handles.erase(it);
        }
        delete reinterpret_cast<ParsedDataInternal*>(ptr);
        pthread_mutex_unlock(&mutex);
    }
    
    static void unregisterNormalizedHandle(jlong handle) {
        pthread_mutex_lock(&mutex);
        void* ptr = reinterpret_cast<void*>(handle);
        delete reinterpret_cast<NormalizedDataInternal*>(ptr);
        pthread_mutex_unlock(&mutex);
    }
};

std::vector<void*> HandleRegistry::handles;
pthread_mutex_t HandleRegistry::mutex = PTHREAD_MUTEX_INITIALIZER;

// Helper functions
std::vector<std::string> splitString(const std::string& str, char delimiter) {
    std::vector<std::string> tokens;
    std::stringstream ss(str);
    std::string token;
    
    while (std::getline(ss, token, delimiter)) {
        tokens.push_back(token);
    }
    
    return tokens;
}

std::string trim(const std::string& str) {
    size_t start = 0;
    size_t end = str.length();
    
    while (start < end && std::isspace(str[start])) {
        ++start;
    }
    
    while (end > start && std::isspace(str[end - 1])) {
        --end;
    }
    
    return str.substr(start, end - start);
}

JNIEXPORT jlong JNICALL Java_DataProcessor_parseInputNative(JNIEnv *env, jobject obj, jstring rawData) {
    if (rawData == nullptr) {
        return 0;
    }
    
    const char* rawChars = env->GetStringUTFChars(rawData, nullptr);
    if (rawChars == nullptr) {
        return 0; // OutOfMemoryError thrown
    }
    
    std::string rawStr(rawChars);
    env->ReleaseStringUTFChars(rawData, rawChars);
    
    try {
        // Parse the raw input into internal representation
        // This example splits by comma and creates individual elements
        std::vector<std::string> elements = splitString(rawStr, ',');
        
        auto* parsedData = new ParsedDataInternal();
        parsedData->elementCount = elements.size();
        
        for (const auto& element : elements) {
            std::string trimmedElement = trim(element);
            if (!trimmedElement.empty()) {
                parsedData->elements.push_back(trimmedElement);
            }
        }
        
        return HandleRegistry::registerHandle(parsedData);
    } catch (const std::exception& e) {
        std::cerr << "Exception in parseInputNative: " << e.what() << std::endl;
        return 0;
    }
}

JNIEXPORT jlong JNICALL Java_DataProcessor_normalizeParsedDataNative(JNIEnv *env, jobject obj, jlong parsedDataHandle) {
    if (parsedDataHandle == 0) {
        return 0;
    }
    
    auto* parsedData = static_cast<ParsedDataInternal*>(HandleRegistry::getHandle(parsedDataHandle));
    if (parsedData == nullptr) {
        return 0;
    }
    
    try {
        auto* normalizedData = new NormalizedDataInternal();
        normalizedData->rulesApplied = "trim whitespace, convert to lowercase";
        
        // Apply normalization rules to each element
        for (const auto& element : parsedData->elements) {
            std::string normalizedElement = element;
            
            // Example normalization: trim whitespace and convert to lowercase
            normalizedElement = trim(normalizedElement);
            
            // Convert to lowercase
            std::transform(normalizedElement.begin(), normalizedElement.end(), 
                         normalizedElement.begin(), ::tolower);
            
            if (!normalizedElement.empty()) {
                normalizedData->normalizedElements.push_back(normalizedElement);
            }
        }
        
        return HandleRegistry::registerHandle(normalizedData);
    } catch (const std::exception& e) {
        std::cerr << "Exception in normalizeParsedDataNative: " << e.what() << std::endl;
        return 0;
    }
}

JNIEXPORT jobject JNICALL Java_DataProcessor_computeResultNative(JNIEnv *env, jobject obj, jlong normalizedDataHandle) {
    if (normalizedDataHandle == 0) {
        return nullptr;
    }
    
    auto* normalizedData = static_cast<NormalizedDataInternal*>(HandleRegistry::getHandle(normalizedDataHandle));
    if (normalizedData == nullptr) {
        return nullptr;
    }
    
    try {
        // Compute final result based on normalized data
        // This example concatenates all elements and returns the result
        std::string result = "";
        for (size_t i = 0; i < normalizedData->normalizedElements.size(); ++i) {
            if (i > 0) {
                result += "|"; // Use pipe as separator
            }
            result += normalizedData->normalizedElements[i];
        }
        
        // Create and return a Java String object
        jstring javaResult = env->NewStringUTF(result.c_str());
        return javaResult;
    } catch (const std::exception& e) {
        std::cerr << "Exception in computeResultNative: " << e.what() << std::endl;
        return nullptr;
    }
}

JNIEXPORT void JNICALL Java_DataProcessor_cleanupHandleNative(JNIEnv *env, jobject obj, jlong handle) {
    if (handle == 0) {
        return;
    }
    
    // Try to determine the type of handle and clean up appropriately
    // In a real implementation, you might use type information stored with the handle
    // For this example, we'll assume it's a ParsedDataInternal first, then NormalizedDataInternal
    
    void* ptr = HandleRegistry::getHandle(handle);
    if (ptr != nullptr) {
        // Delete the appropriate type based on some heuristic
        // In a real implementation, you'd track the type with the handle
        delete static_cast<ParsedDataInternal*>(ptr);
    }
}