#include <jni.h>
#include <vector>

// Note: Ensure your actual cryptography library (like OpenSSL or Libsodium) is included here.

extern "C" {

JNIEXPORT jbyteArray JNICALL Java_CryptoProvider_encrypt(JNIEnv *env, jobject thisObj, jbyteArray plainText, jbyteArray key) {
    // 1. Get lengths and C++ pointers to the Java arrays
    jsize textLength = env->GetArrayLength(plainText);
    jbyte *textElements = env->GetByteArrayElements(plainText, nullptr);

    jsize keyLength = env->GetArrayLength(key);
    jbyte *keyElements = env->GetByteArrayElements(key, nullptr);

    /* 
     * ========================================================
     * [Perform ACTUAL encryption logic here]
     * For demonstration, we implement a simple XOR cipher.
     * ========================================================
     */
    
    // 2. Allocate a new byte array in the JVM for the encrypted result
    jbyteArray encryptedData = env->NewByteArray(textLength);

    if (encryptedData != nullptr) {
        // Using std::vector as a safe C++ buffer
        std::vector<jbyte> buffer(textLength);
        
        // Perform XOR "Encryption"
        for (int i = 0; i < textLength; ++i) {
            buffer[i] = textElements[i] ^ keyElements[i % keyLength];
        }

        // 3. Copy the C++ buffer back into the JVM byte array
        env->SetByteArrayRegion(encryptedData, 0, textLength, buffer.data());
    }

    // 4. Release the input array elements to avoid memory leaks
    // JNI_ABORT tells the JVM we didn't modify the original Java arrays
    env->ReleaseByteArrayElements(plainText, textElements, JNI_ABORT);
    env->ReleaseByteArrayElements(key, keyElements, JNI_ABORT);

    // 5. Return the newly created encrypted byte array to Java
    return encryptedData;
}

JNIEXPORT jbyteArray JNICALL Java_CryptoProvider_decrypt(JNIEnv *env, jobject thisObj, jbyteArray cipherText, jbyteArray key) {
    /* 
     * ========================================================
     * [Perform ACTUAL decryption logic here]
     * Because XOR is a symmetric cipher, calling encrypt again decrypts it.
     * In a real application (e.g., using AES), you would write specific
     * decryption logic here.
     * ========================================================
     */
     
    // Reusing the encrypt function for the mock XOR cipher
    return Java_CryptoProvider_encrypt(env, thisObj, cipherText, key);
}

} // end extern "C"