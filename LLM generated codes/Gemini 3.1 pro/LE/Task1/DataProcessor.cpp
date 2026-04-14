#include <jni.h>
#include <string>
#include <stdexcept>

// --- Mocking the Core C++ Business Logic ---
// In a real environment, this struct and functions would be imported from your core C++ libraries
struct InternalRepresentation {
    std::string data;
    bool isNormalized = false;
};

InternalRepresentation* core_parse(const std::string& input) {
    auto* internalData = new InternalRepresentation();
    internalData->data = input + " -> [Parsed]";
    return internalData;
}

void core_normalize(InternalRepresentation* rep) {
    if (rep) {
        rep->data += " -> [Normalized]";
        rep->isNormalized = true;
    }
}

std::string core_compute(InternalRepresentation* rep) {
    if (rep && rep->isNormalized) {
        return rep->data + " -> [Final Computed Result]";
    }
    return "Computation Failed: Data not properly initialized or normalized";
}
// ------------------------------------------

extern "C" {

// 1. JNI Wrapper for Parsing
JNIEXPORT jlong JNICALL
Java_com_example_processor_DataProcessor_parseInput(JNIEnv *env, jobject thiz, jstring input) {
    if (input == nullptr) {
        return 0; // Return a null handle if input is null
    }

    // Convert Java String to C++ std::string
    const char *inStr = env->GetStringUTFChars(input, nullptr);
    std::string cppInput(inStr);
    
    // Call core C++ logic
    InternalRepresentation* internalRep = core_parse(cppInput);
    
    // Release the JNI string resource
    env->ReleaseStringUTFChars(input, inStr);
    
    // Cast the pointer to jlong to be stored safely in Java
    return reinterpret_cast<jlong>(internalRep);
}

// 2. JNI Wrapper for Normalization
JNIEXPORT void JNICALL
Java_com_example_processor_DataProcessor_normalizeData(JNIEnv *env, jobject thiz, jlong handle) {
    // Cast the jlong back to the C++ struct pointer
    auto* internalRep = reinterpret_cast<InternalRepresentation*>(handle);
    
    if (internalRep != nullptr) {
        core_normalize(internalRep);
    }
}

// 3. JNI Wrapper for Computation
JNIEXPORT jstring JNICALL
Java_com_example_processor_DataProcessor_computeResult(JNIEnv *env, jobject thiz, jlong handle) {
    auto* internalRep = reinterpret_cast<InternalRepresentation*>(handle);
    
    if (internalRep == nullptr) {
        return env->NewStringUTF("Error: Invalid handle");
    }

    // Compute the final result using core logic
    std::string result = core_compute(internalRep);
    
    // Convert C++ std::string back to Java String
    return env->NewStringUTF(result.c_str());
}

// 4. JNI Wrapper for Memory Management (Cleanup)
JNIEXPORT void JNICALL
Java_com_example_processor_DataProcessor_releaseData(JNIEnv *env, jobject thiz, jlong handle) {
    auto* internalRep = reinterpret_cast<InternalRepresentation*>(handle);
    
    // Safely delete the allocated C++ object to prevent memory leaks
    if (internalRep != nullptr) {
        delete internalRep;
    }
}

}