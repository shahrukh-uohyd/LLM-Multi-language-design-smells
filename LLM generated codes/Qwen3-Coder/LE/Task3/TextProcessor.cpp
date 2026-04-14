#include "TextProcessor.h"
#include <iostream>
#include <sstream>
#include <algorithm>
#include <cctype>
#include <set>

// Global registry for managing native object handles
class HandleRegistry {
private:
    static std::vector<void*> handles;
    static pthread_mutex_t mutex;
    
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
        delete reinterpret_cast<ParsedElementsInternal*>(ptr);
        pthread_mutex_unlock(&mutex);
    }
    
    static void unregisterProcessedHandle(jlong handle) {
        pthread_mutex_lock(&mutex);
        void* ptr = reinterpret_cast<void*>(handle);
        delete reinterpret_cast<ProcessedElementsInternal*>(ptr);
        pthread_mutex_unlock(&mutex);
    }
};

std::vector<void*> HandleRegistry::handles;
pthread_mutex_t HandleRegistry::mutex = PTHREAD_MUTEX_INITIALIZER;

// Helper function implementations
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

std::vector<std::string> applyProcessingLogic(const std::vector<std::string>& elements, const std::string& logic) {
    std::vector<std::string> result = elements;
    
    if (logic == "uppercase") {
        for (auto& element : result) {
            std::transform(element.begin(), element.end(), element.begin(), ::toupper);
        }
    } else if (logic == "lowercase") {
        for (auto& element : result) {
            std::transform(element.begin(), element.end(), element.begin(), ::tolower);
        }
    } else if (logic == "remove_duplicates") {
        std::set<std::string> seen;
        std::vector<std::string> uniqueElements;
        for (const auto& element : result) {
            if (seen.insert(element).second) {
                uniqueElements.push_back(element);
            }
        }
        result = uniqueElements;
    } else if (logic == "reverse_order") {
        std::reverse(result.begin(), result.end());
    } else if (logic == "sort_alphabetically") {
        std::sort(result.begin(), result.end());
    } else if (logic == "length_filter") {
        std::vector<std::string> filtered;
        for (const auto& element : result) {
            if (element.length() >= 3) { // Filter elements with length >= 3
                filtered.push_back(element);
            }
        }
        result = filtered;
    } else if (logic == "numeric_only") {
        std::vector<std::string> numericElements;
        for (const auto& element : result) {
            bool isNumeric = true;
            for (char c : element) {
                if (!std::isdigit(c) && c != '.') {
                    isNumeric = false;
                    break;
                }
            }
            if (isNumeric) {
                numericElements.push_back(element);
            }
        }
        result = numericElements;
    }
    
    return result;
}

JNIEXPORT jlong JNICALL Java_TextProcessor_parseInputTextNative(JNIEnv *env, jobject obj, jstring inputText) {
    if (inputText == nullptr) {
        return 0;
    }
    
    const char* inputChars = env->GetStringUTFChars(inputText, nullptr);
    if (inputChars == nullptr) {
        return 0; // OutOfMemoryError thrown
    }
    
    std::string inputStr(inputChars);
    env->ReleaseStringUTFChars(inputText, inputChars);
    
    try {
        auto* parsedElements = new ParsedElementsInternal();
        parsedElements->delimiterUsed = ",";
        
        // Parse the input text - this example splits by comma
        std::vector<std::string> elements = splitString(inputStr, ',');
        
        for (const auto& element : elements) {
            std::string trimmedElement = trim(element);
            if (!trimmedElement.empty()) {
                parsedElements->elements.push_back(trimmedElement);
            }
        }
        
        parsedElements->elementCount = parsedElements->elements.size();
        
        return HandleRegistry::registerHandle(parsedElements);
    } catch (const std::exception& e) {
        std::cerr << "Exception in parseInputTextNative: " << e.what() << std::endl;
        return 0;
    }
}

JNIEXPORT jlong JNICALL Java_TextProcessor_processStructuredElementsNative(JNIEnv *env, jobject obj, jlong parsedElementsHandle, jstring processingLogic) {
    if (parsedElementsHandle == 0 || processingLogic == nullptr) {
        return 0;
    }
    
    auto* parsedElements = static_cast<ParsedElementsInternal*>(HandleRegistry::getHandle(parsedElementsHandle));
    if (parsedElements == nullptr) {
        return 0;
    }
    
    const char* logicChars = env->GetStringUTFChars(processingLogic, nullptr);
    if (logicChars == nullptr) {
        return 0; // OutOfMemoryError thrown
    }
    
    std::string logicStr(logicChars);
    env->ReleaseStringUTFChars(processingLogic, logicChars);
    
    try {
        auto* processedElements = new ProcessedElementsInternal();
        processedElements->processingLogic = logicStr;
        
        // Apply processing logic to the parsed elements
        processedElements->processedElements = applyProcessingLogic(parsedElements->elements, logicStr);
        
        // Log the processing operation
        processedElements->processingLog.push_back("Applied logic: " + logicStr);
        processedElements->processingLog.push_back("Input element count: " + std::to_string(parsedElements->elements.size()));
        processedElements->processingLog.push_back("Output element count: " + std::to_string(processedElements->processedElements.size()));
        
        return HandleRegistry::registerHandle(processedElements);
    } catch (const std::exception& e) {
        std::cerr << "Exception in processStructuredElementsNative: " << e.what() << std::endl;
        return 0;
    }
}

JNIEXPORT jstring JNICALL Java_TextProcessor_generateOutputFromProcessedElementsNative(JNIEnv *env, jobject obj, jlong processedElementsHandle) {
    if (processedElementsHandle == 0) {
        return nullptr;
    }
    
    auto* processedElements = static_cast<ProcessedElementsInternal*>(HandleRegistry::getHandle(processedElementsHandle));
    if (processedElements == nullptr) {
        return nullptr;
    }
    
    try {
        // Generate final output by joining processed elements with a pipe delimiter
        std::string output = "";
        for (size_t i = 0; i < processedElements->processedElements.size(); ++i) {
            if (i > 0) {
                output += "|";
            }
            output += processedElements->processedElements[i];
        }
        
        // Create and return a Java String object
        jstring javaResult = env->NewStringUTF(output.c_str());
        return javaResult;
    } catch (const std::exception& e) {
        std::cerr << "Exception in generateOutputFromProcessedElementsNative: " << e.what() << std::endl;
        return nullptr;
    }
}

JNIEXPORT void JNICALL Java_TextProcessor_cleanupHandleNative(JNIEnv *env, jobject obj, jlong handle) {
    if (handle == 0) {
        return;
    }
    
    void* ptr = HandleRegistry::getHandle(handle);
    if (ptr != nullptr) {
        delete static_cast<ParsedElementsInternal*>(ptr);
    }
}