#include "TransformationPipeline.h"
#include <iostream>
#include <sstream>
#include <algorithm>
#include <cctype>
#include <regex>
#include <chrono>

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
        delete reinterpret_cast<InterpretedDataInternal*>(ptr);
        pthread_mutex_unlock(&mutex);
    }
    
    static void unregisterTransformedHandle(jlong handle) {
        pthread_mutex_lock(&mutex);
        void* ptr = reinterpret_cast<void*>(handle);
        delete reinterpret_cast<TransformedDataInternal*>(ptr);
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

// Transformation operation implementations
std::string applyUpperCaseTransformation(const std::string& value) {
    std::string result = value;
    std::transform(result.begin(), result.end(), result.begin(), ::toupper);
    return result;
}

std::string applyLowerCaseTransformation(const std::string& value) {
    std::string result = value;
    std::transform(result.begin(), result.end(), result.begin(), ::tolower);
    return result;
}

std::string applyNumberTransformation(const std::string& value, const std::string& operation, double param) {
    try {
        double numValue = std::stod(value);
        double result = 0.0;
        
        if (operation == "add") {
            result = numValue + param;
        } else if (operation == "multiply") {
            result = numValue * param;
        } else if (operation == "subtract") {
            result = numValue - param;
        } else if (operation == "divide") {
            result = numValue / param;
        }
        
        return std::to_string(result);
    } catch (...) {
        return value; // Return original if not a number
    }
}

std::string applyRegexTransformation(const std::string& value, const std::string& pattern, const std::string& replacement) {
    try {
        std::regex regexPattern(pattern);
        return std::regex_replace(value, regexPattern, replacement);
    } catch (...) {
        return value; // Return original if regex fails
    }
}

JNIEXPORT jlong JNICALL Java_TransformationPipeline_interpretLowLevelDataNative(JNIEnv *env, jobject obj, jbyteArray lowLevelData) {
    if (lowLevelData == nullptr) {
        return 0;
    }
    
    jsize length = env->GetArrayLength(lowLevelData);
    jbyte* bytes = env->GetByteArrayElements(lowLevelData, nullptr);
    
    if (bytes == nullptr) {
        return 0; // OutOfMemoryError thrown
    }
    
    try {
        // Interpret the low-level data
        std::string dataStr(reinterpret_cast<char*>(bytes), length);
        env->ReleaseByteArrayElements(lowLevelData, bytes, JNI_ABORT);
        
        auto* interpretedData = new InterpretedDataInternal();
        interpretedData->dataType = "custom_format";
        interpretedData->timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();
        
        // Parse the data - example implementation parses key=value pairs separated by semicolons
        std::vector<std::string> pairs = splitString(dataStr, ';');
        for (const auto& pair : pairs) {
            std::vector<std::string> keyValue = splitString(pair, '=');
            if (keyValue.size() == 2) {
                interpretedData->fields[trim(keyValue[0])] = trim(keyValue[1]);
            }
        }
        
        return HandleRegistry::registerHandle(interpretedData);
    } catch (const std::exception& e) {
        std::cerr << "Exception in interpretLowLevelDataNative: " << e.what() << std::endl;
        env->ReleaseByteArrayElements(lowLevelData, bytes, JNI_ABORT);
        return 0;
    }
}

JNIEXPORT jlong JNICALL Java_TransformationPipeline_applyTransformationRulesNative(JNIEnv *env, jobject obj, jlong interpretedDataHandle, jobjectArray rules) {
    if (interpretedDataHandle == 0 || rules == nullptr) {
        return 0;
    }
    
    auto* interpretedData = static_cast<InterpretedDataInternal*>(HandleRegistry::getHandle(interpretedDataHandle));
    if (interpretedData == nullptr) {
        return 0;
    }
    
    try {
        auto* transformedData = new TransformedDataInternal();
        
        jsize ruleCount = env->GetArrayLength(rules);
        
        // Get the TransformationRule class and method IDs
        jclass ruleClass = env->FindClass("TransformationRule");
        jmethodID getRuleIdMethod = env->GetMethodID(ruleClass, "getRuleId", "()Ljava/lang/String;");
        jmethodID getSourceFieldMethod = env->GetMethodID(ruleClass, "getSourceField", "()Ljava/lang/String;");
        jmethodID getTargetFieldMethod = env->GetMethodID(ruleClass, "getTargetField", "()Ljava/lang/String;");
        jmethodID getOperationMethod = env->GetMethodID(ruleClass, "getOperation", "()Ljava/lang/String;");
        jmethodID getParameterMethod = env->GetMethodID(ruleClass, "getParameter", "()Ljava/lang/Object;");
        
        // Copy original fields to transformed data
        for (const auto& fieldPair : interpretedData->fields) {
            transformedData->transformedFields[fieldPair.first] = fieldPair.second;
        }
        
        // Apply each transformation rule
        for (jsize i = 0; i < ruleCount; i++) {
            jobject ruleObj = env->GetObjectArrayElement(rules, i);
            if (ruleObj == nullptr) continue;
            
            // Extract rule properties
            jstring ruleIdJstr = (jstring)env->CallObjectMethod(ruleObj, getRuleIdMethod);
            jstring sourceFieldJstr = (jstring)env->CallObjectMethod(ruleObj, getSourceFieldMethod);
            jstring targetFieldJstr = (jstring)env->CallObjectMethod(ruleObj, getTargetFieldMethod);
            jstring operationJstr = (jstring)env->CallObjectMethod(ruleObj, getOperationMethod);
            jobject parameterObj = env->CallObjectMethod(ruleObj, getParameterMethod);
            
            const char* ruleId = env->GetStringUTFChars(ruleIdJstr, nullptr);
            const char* sourceField = env->GetStringUTFChars(sourceFieldJstr, nullptr);
            const char* targetField = env->GetStringUTFChars(targetFieldJstr, nullptr);
            const char* operation = env->GetStringUTFChars(operationJstr, nullptr);
            
            std::string ruleIdStr = ruleId ? ruleId : "";
            std::string sourceFieldStr = sourceField ? sourceField : "";
            std::string targetFieldStr = targetField ? targetField : "";
            std::string operationStr = operation ? operation : "";
            
            std::string sourceValue = "";
            auto it = interpretedData->fields.find(sourceFieldStr);
            if (it != interpretedData->fields.end()) {
                sourceValue = it->second;
            }
            
            std::string transformedValue = sourceValue;
            
            // Apply transformation based on operation type
            if (operationStr == "uppercase") {
                transformedValue = applyUpperCaseTransformation(sourceValue);
            } else if (operationStr == "lowercase") {
                transformedValue = applyLowerCaseTransformation(sourceValue);
            } else if (operationStr == "add" || operationStr == "multiply" || 
                      operationStr == "subtract" || operationStr == "divide") {
                if (parameterObj != nullptr) {
                    jdouble paramValue = env->CallDoubleMethod(parameterObj, 
                        env->GetMethodID(env->GetObjectClass(parameterObj), "doubleValue", "()D"));
                    transformedValue = applyNumberTransformation(sourceValue, operationStr, paramValue);
                }
            } else if (operationStr == "regex_replace") {
                if (parameterObj != nullptr) {
                    jstring patternJstr = (jstring)env->CallObjectMethod(parameterObj, 
                        env->GetMethodID(env->GetObjectClass(parameterObj), "toString", "()Ljava/lang/String;"));
                    const char* pattern = env->GetStringUTFChars(patternJstr, nullptr);
                    transformedValue = applyRegexTransformation(sourceValue, pattern, "");
                    env->ReleaseStringUTFChars(patternJstr, pattern);
                }
            }
            
            // Set the transformed value in the target field
            transformedData->transformedFields[targetFieldStr] = transformedValue;
            transformedData->appliedRules.push_back(ruleIdStr);
            
            // Clean up local references
            env->ReleaseStringUTFChars(ruleIdJstr, ruleId);
            env->ReleaseStringUTFChars(sourceFieldJstr, sourceField);
            env->ReleaseStringUTFChars(targetFieldJstr, targetField);
            env->ReleaseStringUTFChars(operationJstr, operation);
        }
        
        return HandleRegistry::registerHandle(transformedData);
    } catch (const std::exception& e) {
        std::cerr << "Exception in applyTransformationRulesNative: " << e.what() << std::endl;
        return 0;
    }
}

JNIEXPORT jbyteArray JNICALL Java_TransformationPipeline_generateTransformedOutputNative(JNIEnv *env, jobject obj, jlong transformedDataHandle) {
    if (transformedDataHandle == 0) {
        return nullptr;
    }
    
    auto* transformedData = static_cast<TransformedDataInternal*>(HandleRegistry::getHandle(transformedDataHandle));
    if (transformedData == nullptr) {
        return nullptr;
    }
    
    try {
        // Generate transformed output as a serialized string
        std::string output = "";
        bool first = true;
        
        for (const auto& fieldPair : transformedData->transformedFields) {
            if (!first) {
                output += ";";
            }
            output += fieldPair.first + "=" + fieldPair.second;
            first = false;
        }
        
        // Convert to byte array
        jsize length = output.length();
        jbyteArray result = env->NewByteArray(length);
        if (result == nullptr) {
            return nullptr; // OutOfMemoryError thrown
        }
        
        env->SetByteArrayRegion(result, 0, length, 
                               reinterpret_cast<const jbyte*>(output.c_str()));
        
        return result;
    } catch (const std::exception& e) {
        std::cerr << "Exception in generateTransformedOutputNative: " << e.what() << std::endl;
        return nullptr;
    }
}

JNIEXPORT void JNICALL Java_TransformationPipeline_cleanupHandleNative(JNIEnv *env, jobject obj, jlong handle) {
    if (handle == 0) {
        return;
    }
    
    void* ptr = HandleRegistry::getHandle(handle);
    if (ptr != nullptr) {
        delete static_cast<InterpretedDataInternal*>(ptr);
    }
}