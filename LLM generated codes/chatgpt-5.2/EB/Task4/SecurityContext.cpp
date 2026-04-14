#include <jni.h>
#include <vector>
#include <cstring>

// Example native encryption engine initializer
void initEncryptionEngine(const unsigned char* key, size_t keyLen) {
    // Native encryption engine initialization logic here
    // (e.g., AES key schedule, hardware module init, etc.)
}

JNIEXPORT void JNICALL
Java_SecurityContext_initNativeEncryption(JNIEnv* env, jobject securityObj) {

    // Get the SecurityContext class
    jclass securityClass = env->GetObjectClass(securityObj);
    if (securityClass == nullptr) {
        return;
    }

    // Locate the private field: byte[] key
    jfieldID keyField = env->GetFieldID(
        securityClass,
        "key",
        "[B"
    );

    if (keyField == nullptr) {
        env->ExceptionClear();
        return;
    }

    // Retrieve the key byte array
    jbyteArray keyArray =
        (jbyteArray)env->GetObjectField(securityObj, keyField);

    if (keyArray == nullptr) {
        return;
    }

    // Access key bytes
    jsize keyLen = env->GetArrayLength(keyArray);
    jbyte* keyBytes = env->GetByteArrayElements(keyArray, nullptr);

    if (keyBytes == nullptr || keyLen == 0) {
        return;
    }

    // Copy key into native-safe buffer
    std::vector<unsigned char> nativeKey(
        reinterpret_cast<unsigned char*>(keyBytes),
        reinterpret_cast<unsigned char*>(keyBytes) + keyLen
    );

    // Initialize native encryption engine
    initEncryptionEngine(nativeKey.data(), nativeKey.size());

    // Release Java byte array (no modification)
    env->ReleaseByteArrayElements(
        keyArray,
        keyBytes,
        JNI_ABORT
    );
}
