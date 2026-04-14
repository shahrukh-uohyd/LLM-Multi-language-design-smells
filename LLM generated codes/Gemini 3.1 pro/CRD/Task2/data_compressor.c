#include <jni.h>
#include <stdlib.h>
#include <string.h>

// Include your actual C compression library here (e.g., zlib, lz4)
// #include <zlib.h> 

JNIEXPORT jbyteArray JNICALL Java_DataCompressor_compress(JNIEnv *env, jobject thisObj, jbyteArray inputData) {
    // 1. Get the length of the input byte array
    jsize inputLength = (*env)->GetArrayLength(env, inputData);

    // 2. Obtain a pointer to the input elements in memory
    jbyte *elements = (*env)->GetByteArrayElements(env, inputData, NULL);

    /* 
     * ========================================================
     * [Perform ACTUAL compression logic here using elements]
     * For this example, we mock a compression by halving the array.
     * ========================================================
     */
    jsize compressedLength = inputLength / 2; // Mock size
    
    // 3. Allocate a new byte array in the JVM for the compressed result
    jbyteArray compressedData = (*env)->NewByteArray(env, compressedLength);

    if (compressedData != NULL) {
        // Copy the "compressed" C buffer back into the JVM byte array
        // (In a real scenario, you'd pass the actual compressed buffer instead of 'elements')
        (*env)->SetByteArrayRegion(env, compressedData, 0, compressedLength, elements);
    }

    // 4. Release the input array elements to avoid memory leaks (JNI_ABORT means we didn't modify it)
    (*env)->ReleaseByteArrayElements(env, inputData, elements, JNI_ABORT);

    // 5. Return the newly created compressed byte array to Java
    return compressedData;
}

JNIEXPORT jbyteArray JNICALL Java_DataCompressor_decompress(JNIEnv *env, jobject thisObj, jbyteArray compressedData) {
    jsize compressedLength = (*env)->GetArrayLength(env, compressedData);
    jbyte *elements = (*env)->GetByteArrayElements(env, compressedData, NULL);

    /* 
     * ========================================================
     * [Perform ACTUAL decompression logic here]
     * We mock decompression by doubling the array size.
     * ========================================================
     */
    jsize decompressedLength = compressedLength * 2; // Mock original size
    jbyteArray decompressedData = (*env)->NewByteArray(env, decompressedLength);

    if (decompressedData != NULL) {
        // Allocate temporary C buffer for the mock decompressed data
        jbyte* tempBuffer = (jbyte*) malloc(decompressedLength);
        memset(tempBuffer, 1, decompressedLength); // Fill with dummy data

        // Copy back to JVM array
        (*env)->SetByteArrayRegion(env, decompressedData, 0, decompressedLength, tempBuffer);
        free(tempBuffer);
    }

    (*env)->ReleaseByteArrayElements(env, compressedData, elements, JNI_ABORT);

    return decompressedData;
}