#include <jni.h>
#include <iostream>

// --- Mock Native Encryption Engine ---
// This represents your actual C++ encryption logic (e.g., OpenSSL, AES-NI, etc.)
class NativeEncryptionEngine {
public:
    static void Initialize(const unsigned char* key, int keyLength) {
        std::cout << "[C++] Native Encryption Engine Initializing..." << std::endl;
        std::cout << "[C++] Received key of length: " << keyLength << " bytes." << std::endl;
        
        // Example: securely load the key into hardware/secure memory here
        // ...
        
        std::cout << "[C++] Native Engine is ready for data." << std::endl;
    }
};

// --- JNI Implementation ---
extern "C" 
JNIEXPORT void JNICALL Java_CryptoManager_initializeEncryptionEngine(JNIEnv *env, jobject thiz, jobject securityObject) {
    
    // 1. Safety check: ensure the passed object is not null
    if (securityObject == nullptr) {
        std::cerr << "[C++] Error: Provided Security Object is null." << std::endl;
        return;
    }

    // 2. Get the class of the passed Java 'securityObject'
    jclass secObjClass = env->GetObjectClass(securityObject);

    // 3. Find the field ID of the internal key.
    // We assume the key is stored as a byte[] in Java. 
    // The JNI signature for byte[] is "[B".
    jfieldID keyFieldId = env->GetFieldID(secObjClass, "encryptionKey", "[B");
    if (keyFieldId == nullptr) {
        // JNI automatically throws NoSuchFieldError in Java if not found
        std::cerr << "[C++] Error: 'encryptionKey' field not found in Security Object." << std::endl;
        env->DeleteLocalRef(secObjClass);
        return;
    }

    // 4. Retrieve the actual jbyteArray object from the securityObject
    jbyteArray jKeyArray = (jbyteArray) env->GetObjectField(securityObject, keyFieldId);
    if (jKeyArray == nullptr) {
        std::cerr << "[C++] Error: 'encryptionKey' is null." << std::endl;
        env->DeleteLocalRef(secObjClass);
        return;
    }

    // 5. Get the length of the byte array
    jsize keyLength = env->GetArrayLength(jKeyArray);

    // 6. Extract the raw bytes from the jbyteArray
    // This gives us a pointer to the array elements (jbyte is signed char)
    jbyte* keyBytes = env->GetByteArrayElements(jKeyArray, nullptr);
    if (keyBytes == nullptr) {
        // OutOfMemoryError already thrown by JVM
        env->DeleteLocalRef(jKeyArray);
        env->DeleteLocalRef(secObjClass);
        return;
    }

    // 7. Pass the extracted bytes to the native C++ engine
    // We cast jbyte* to unsigned char* as most C++ crypto libraries expect unsigned bytes.
    NativeEncryptionEngine::Initialize(reinterpret_cast<const unsigned char*>(keyBytes), keyLength);

    // 8. Secure Cleanup
    // Release the memory/reference to the byte array.
    // JNI_ABORT tells the JVM that we did NOT modify the array, 
    // so it doesn't need to waste time copying data back to the original Java array.
    // This is also important for security so key material isn't lingering in memory copies.
    env->ReleaseByteArrayElements(jKeyArray, keyBytes, JNI_ABORT);

    // Delete local references to avoid memory leaks in the JNI local reference table
    env->DeleteLocalRef(jKeyArray);
    env->DeleteLocalRef(secObjClass);
}