#include <jni.h>
#include <vector>

// Note: In a production environment, include your actual cryptography library headers here (e.g., <openssl/evp.h> or <sodium.h>).

extern "C" {

JNIEXPORT jbyteArray JNICALL Java_CryptoEngine_encryptPayload(JNIEnv *env, jobject thisObj, jbyteArray plainText, jbyteArray key) {
    // 1. Retrieve the lengths of the Java arrays
    jsize textLength = env->GetArrayLength(plainText);
    jsize keyLength = env->GetArrayLength(key);

    // 2. Obtain pointers to the elements (JVM may copy or pin the memory)
    jbyte *textElements = env->GetByteArrayElements(plainText, nullptr);
    jbyte *keyElements = env->GetByteArrayElements(key, nullptr);

    /* 
     * ========================================================
     * [Perform ACTUAL encryption logic here]
     * For demonstration, we implement a simple XOR cipher.
     * ========================================================
     */
    
    // 3. Create a new Java byte array for the result
    jbyteArray encryptedData = env->NewByteArray(textLength);

    if (encryptedData != nullptr && keyLength > 0) {
        // Use std::vector to safely manage the temporary C++ buffer
        std::vector<jbyte> buffer(textLength);
        
        // Mock Encryption (XOR)
        for (int i = 0; i < textLength; ++i) {
            buffer[i] = textElements[i] ^ keyElements[i % keyLength];
        }

        // 4. Copy the processed C++ buffer back into the JVM's result array
        env->SetByteArrayRegion(encryptedData, 0, textLength, buffer.data());
    }

    // 5. Release the input array elements
    // JNI_ABORT is used because we only read the data; we do not need to copy changes back to the original Java arrays.
    env->ReleaseByteArrayElements(plainText, textElements, JNI_ABORT);
    env->ReleaseByteArrayElements(key, keyElements, JNI_ABORT);

    // 6. Return the resulting array to Java
    return encryptedData;
}

JNIEXPORT jbyteArray JNICALL Java_CryptoEngine_decryptPayload(JNIEnv *env, jobject thisObj, jbyteArray cipherText, jbyteArray key) {
    /* 
     * ========================================================
     * [Perform ACTUAL decryption logic here]
     * Since XOR is a symmetric operation, calling the encryption
     * function again will reverse it. For algorithms like AES,
     * you would implement specific decryption API calls here.
     * ========================================================
     */
     
    // Reusing the encrypt function for the mock XOR cipher
    return Java_CryptoEngine_encryptPayload(env, thisObj, cipherText, key);
}

} // end extern "C"