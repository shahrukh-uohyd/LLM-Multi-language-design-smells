#include <jni.h>
#include <stdint.h>

JNIEXPORT jbyteArray JNICALL
Java_NativeBinaryProcessor_process(
        JNIEnv* env,
        jclass,
        jbyteArray input) {

    if (input == NULL) {
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, input);

    // Access input bytes
    jbyte* inBytes =
        (*env)->GetByteArrayElements(env, input, NULL);
    if (inBytes == NULL) {
        return NULL;
    }

    // Create output byte[]
    jbyteArray output =
        (*env)->NewByteArray(env, length);
    if (output == NULL) {
        (*env)->ReleaseByteArrayElements(env, input, inBytes, JNI_ABORT);
        return NULL;
    }

    jbyte* outBytes =
        (*env)->GetByteArrayElements(env, output, NULL);
    if (outBytes == NULL) {
        (*env)->ReleaseByteArrayElements(env, input, inBytes, JNI_ABORT);
        return NULL;
    }

    // Inspect: compute checksum
    uint32_t checksum = 0;

    // Transform: invert each byte
    for (jsize i = 0; i < length; i++) {
        checksum += (uint8_t)inBytes[i];
        outBytes[i] = (jbyte)(~inBytes[i]);
    }

    // (Optional) checksum could be logged or used internally
    // printf("Checksum: %u\n", checksum);

    // Commit output, discard input changes
    (*env)->ReleaseByteArrayElements(env, output, outBytes, 0);
    (*env)->ReleaseByteArrayElements(env, input, inBytes, JNI_ABORT);

    return output;
}
