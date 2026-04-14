#ifndef IMAGE_FILTER_H
#define IMAGE_FILTER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * All native filter functions follow the same byte-array contract:
 *   - pixels : flat RGBA byte array (length = width * height * 4)
 *   - width  : frame width  in pixels
 *   - height : frame height in pixels
 *   - return : new RGBA byte array of the same dimensions
 *
 * The Java class is com.imagefilter.ImageFilterEngine.
 * JNI mangling: Java_com_imagefilter_ImageFilterEngine_native<FilterName>
 */

/* ── Point filters ─────────────────────────────────────────────────────── */

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeGrayscale(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height);

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeInvert(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height);

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeBrightness(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height, jint delta);

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeContrast(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height, jdouble factor);

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeSepia(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height);

/* ── Convolution filters ───────────────────────────────────────────────── */

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeGaussianBlur(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height,
    jint radius, jdouble sigma);

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeSharpen(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height, jdouble strength);

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeEdgeDetect(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height, jint threshold);

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeEmboss(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height);

/* ── Spatial filters ───────────────────────────────────────────────────── */

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativePixelate(
    JNIEnv *, jobject, jbyteArray pixels, jint width, jint height, jint blockSize);

#ifdef __cplusplus
}
#endif
#endif /* IMAGE_FILTER_H */