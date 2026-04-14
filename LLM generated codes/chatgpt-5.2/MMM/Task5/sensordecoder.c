#include <jni.h>
#include <stdint.h>

JNIEXPORT jintArray JNICALL
Java_NativeSensorDecoder_decode(
        JNIEnv* env,
        jclass,
        jbyteArray stream) {

    if (stream == NULL) {
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, stream);

    // Each reading is 4 bytes
    if (length % 4 != 0) {
        return NULL; // malformed stream
    }

    jsize count = length / 4;

    jbyte* bytes =
        (*env)->GetByteArrayElements(env, stream, NULL);
    if (bytes == NULL) {
        return NULL;
    }

    jintArray result =
        (*env)->NewIntArray(env, count);
    if (result == NULL) {
        (*env)->ReleaseByteArrayElements(env, stream, bytes, JNI_ABORT);
        return NULL;
    }

    jint* values =
        (*env)->GetIntArrayElements(env, result, NULL);

    // Decode big-endian 4-byte integers
    for (jsize i = 0; i < count; i++) {
        int idx = i * 4;
        values[i] =
            ((bytes[idx]     & 0xFF) << 24) |
            ((bytes[idx + 1] & 0xFF) << 16) |
            ((bytes[idx + 2] & 0xFF) << 8)  |
            ( bytes[idx + 3] & 0xFF);
    }

    (*env)->ReleaseIntArrayElements(env, result, values, 0);
    (*env)->ReleaseByteArrayElements(env, stream, bytes, JNI_ABORT);

    return result;
}
