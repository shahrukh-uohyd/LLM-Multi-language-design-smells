#include <jni.h>
#include <stdlib.h>
#include <stdint.h>

JNIEXPORT jbyteArray JNICALL
Java_NativeImageEncoder_encode(
        JNIEnv* env,
        jclass,
        jbyteArray pixelArray,
        jint width,
        jint height) {

    if (pixelArray == NULL || width <= 0 || height <= 0) {
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, pixelArray);

    // Access pixel data
    jbyte* pixels =
        (*env)->GetByteArrayElements(env, pixelArray, NULL);
    if (pixels == NULL) {
        return NULL;
    }

    // Worst-case RLE size: 2 bytes per pixel
    jbyte* encoded =
        (jbyte*)malloc(length * 2);
    if (encoded == NULL) {
        (*env)->ReleaseByteArrayElements(
            env, pixelArray, pixels, JNI_ABORT);
        return NULL;
    }

    int outIndex = 0;
    for (int i = 0; i < length; ) {
        jbyte value = pixels[i];
        int count = 1;

        while (i + count < length &&
               pixels[i + count] == value &&
               count < 255) {
            count++;
        }

        encoded[outIndex++] = (jbyte)count;
        encoded[outIndex++] = value;

        i += count;
    }

    // Create Java byte[] for result
    jbyteArray result =
        (*env)->NewByteArray(env, outIndex);
    if (result != NULL) {
        (*env)->SetByteArrayRegion(
            env, result, 0, outIndex, encoded);
    }

    // Cleanup
    free(encoded);
    (*env)->ReleaseByteArrayElements(
        env, pixelArray, pixels, JNI_ABORT);

    return result;
}