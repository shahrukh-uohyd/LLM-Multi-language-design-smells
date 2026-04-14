/*
 * image_filter.c
 *
 * Complete native implementation of all ten image filters declared in
 * ImageFilterEngine.java.  Every filter operates on a flat RGBA byte array
 * and returns a freshly-allocated RGBA byte array of identical dimensions.
 *
 * Compile (Linux):
 *   gcc -O2 -shared -fPIC \
 *       -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
 *       -o ../native_libs/libimage_filter.so \
 *       image_filter.c -lm
 *
 * Compile (macOS):
 *   gcc -O2 -dynamiclib \
 *       -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
 *       -o ../native_libs/libimage_filter.dylib \
 *       image_filter.c -lm
 *
 * Compile (Windows, MinGW):
 *   gcc -O2 -shared \
 *       -I"%JAVA_HOME%/include" -I"%JAVA_HOME%/include/win32" \
 *       -o ../native_libs/image_filter.dll \
 *       image_filter.c
 */

#include "image_filter.h"
#include <stdlib.h>
#include <string.h>
#include <math.h>

/* ═══════════════════════════════════════════════════════════════════════════
 * Internal macros & helpers
 * ═══════════════════════════════════════════════════════════════════════════*/

#define CHANNELS 4          /* R, G, B, A bytes per pixel                  */
#define IDX(x,y,w)  (((y)*(w)+(x))*CHANNELS)  /* flat RGBA index          */

/* Clamp an integer to [0, 255] */
static inline int clamp(int v) {
    return v < 0 ? 0 : (v > 255 ? 255 : v);
}

/* Clamp a double to [0.0, 255.0] and cast to unsigned char */
static inline unsigned char clampd(double v) {
    return (unsigned char)(v < 0.0 ? 0.0 : (v > 255.0 ? 255.0 : v));
}

/*
 * alloc_result – allocates a new jbyteArray of size (w*h*4) and pins it.
 * The caller MUST call (*env)->ReleaseByteArrayElements(..., out_ptr, 0)
 * before returning the array.
 */
static jbyteArray alloc_result(JNIEnv *env, int w, int h, jbyte **out_ptr) {
    jbyteArray arr = (*env)->NewByteArray(env, w * h * CHANNELS);
    if (!arr) return NULL;
    *out_ptr = (*env)->GetByteArrayElements(env, arr, NULL);
    return arr;
}

/* Pin input and output arrays, return 0 on failure */
static int pin_input(JNIEnv *env, jbyteArray jpx, jbyte **px_ptr) {
    *px_ptr = (*env)->GetByteArrayElements(env, jpx, NULL);
    return (*px_ptr != NULL);
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 1. GRAYSCALE  — ITU-R BT.601 luminance conversion
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeGrayscale(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    int total = w * h;
    for (int i = 0; i < total; i++) {
        int base = i * CHANNELS;
        unsigned char r = (unsigned char)src[base];
        unsigned char g = (unsigned char)src[base + 1];
        unsigned char b = (unsigned char)src[base + 2];
        unsigned char a = (unsigned char)src[base + 3];

        /* BT.601: Y = 0.299·R + 0.587·G + 0.114·B */
        unsigned char y = (unsigned char)(0.299 * r + 0.587 * g + 0.114 * b);

        dst[base]     = (jbyte)y;
        dst[base + 1] = (jbyte)y;
        dst[base + 2] = (jbyte)y;
        dst[base + 3] = (jbyte)a;   /* preserve alpha */
    }

    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 2. INVERT  — bitwise complement per RGB channel
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeInvert(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    int total = w * h;
    for (int i = 0; i < total; i++) {
        int b = i * CHANNELS;
        dst[b]     = (jbyte)(255 - (unsigned char)src[b]);
        dst[b + 1] = (jbyte)(255 - (unsigned char)src[b + 1]);
        dst[b + 2] = (jbyte)(255 - (unsigned char)src[b + 2]);
        dst[b + 3] =  src[b + 3];   /* preserve alpha */
    }

    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 3. BRIGHTNESS  — additive shift with clamping
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeBrightness(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h, jint delta) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    int total = w * h;
    for (int i = 0; i < total; i++) {
        int b = i * CHANNELS;
        dst[b]     = (jbyte)clamp((int)(unsigned char)src[b]     + delta);
        dst[b + 1] = (jbyte)clamp((int)(unsigned char)src[b + 1] + delta);
        dst[b + 2] = (jbyte)clamp((int)(unsigned char)src[b + 2] + delta);
        dst[b + 3] = src[b + 3];
    }

    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 4. CONTRAST  — multiplicative factor around mid-grey (128)
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeContrast(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h, jdouble factor) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    int total = w * h;
    for (int i = 0; i < total; i++) {
        int b = i * CHANNELS;
        dst[b]     = (jbyte)clamp((int)(factor * ((int)(unsigned char)src[b]     - 128) + 128));
        dst[b + 1] = (jbyte)clamp((int)(factor * ((int)(unsigned char)src[b + 1] - 128) + 128));
        dst[b + 2] = (jbyte)clamp((int)(factor * ((int)(unsigned char)src[b + 2] - 128) + 128));
        dst[b + 3] = src[b + 3];
    }

    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 5. SEPIA  — classic photographic sepia-tone colour matrix
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeSepia(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    int total = w * h;
    for (int i = 0; i < total; i++) {
        int b = i * CHANNELS;
        double r = (unsigned char)src[b];
        double g = (unsigned char)src[b + 1];
        double bl= (unsigned char)src[b + 2];

        dst[b]     = clampd(r * 0.393 + g * 0.769 + bl * 0.189);
        dst[b + 1] = clampd(r * 0.349 + g * 0.686 + bl * 0.168);
        dst[b + 2] = clampd(r * 0.272 + g * 0.534 + bl * 0.131);
        dst[b + 3] = src[b + 3];
    }

    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 6. GAUSSIAN BLUR  — separable two-pass (horizontal then vertical)
 * ═══════════════════════════════════════════════════════════════════════════*/

/* Build a 1-D Gaussian kernel of half-width r with std-dev sigma.
 * Caller is responsible for free()-ing the returned array.            */
static double *build_gaussian_kernel(int r, double sigma, int *kernel_size) {
    int ks = 2 * r + 1;
    *kernel_size = ks;
    double *k = (double *)malloc(ks * sizeof(double));
    if (!k) return NULL;

    double sum = 0.0;
    double s2 = 2.0 * sigma * sigma;
    for (int i = -r; i <= r; i++) {
        k[i + r] = exp(-(double)(i * i) / s2);
        sum += k[i + r];
    }
    for (int i = 0; i < ks; i++) k[i] /= sum;  /* normalise */
    return k;
}

JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeGaussianBlur(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h, jint radius, jdouble sigma) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    /* Trivial case */
    if (radius == 0) {
        jbyte *dst;
        jbyteArray result = alloc_result(env, w, h, &dst);
        memcpy(dst, src, (size_t)(w * h * CHANNELS));
        (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
        (*env)->ReleaseByteArrayElements(env, result, dst, 0);
        return result;
    }

    int ks;
    double *kernel = build_gaussian_kernel(radius, sigma, &ks);
    if (!kernel) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    /* Intermediate buffer for horizontal pass */
    unsigned char *temp = (unsigned char *)malloc((size_t)(w * h * CHANNELS));
    if (!temp) {
        free(kernel);
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    /* ── Horizontal pass ── */
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            double acc[4] = {0, 0, 0, 0};
            for (int k = -radius; k <= radius; k++) {
                int sx = x + k;
                if (sx < 0) sx = 0;
                if (sx >= w) sx = w - 1;
                double kv = kernel[k + radius];
                int idx = IDX(sx, y, w);
                acc[0] += kv * (unsigned char)src[idx];
                acc[1] += kv * (unsigned char)src[idx + 1];
                acc[2] += kv * (unsigned char)src[idx + 2];
                acc[3] += kv * (unsigned char)src[idx + 3];
            }
            int idx = IDX(x, y, w);
            temp[idx]     = clampd(acc[0]);
            temp[idx + 1] = clampd(acc[1]);
            temp[idx + 2] = clampd(acc[2]);
            temp[idx + 3] = clampd(acc[3]);
        }
    }

    /* ── Vertical pass ── */
    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        free(kernel); free(temp);
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            double acc[4] = {0, 0, 0, 0};
            for (int k = -radius; k <= radius; k++) {
                int sy = y + k;
                if (sy < 0) sy = 0;
                if (sy >= h) sy = h - 1;
                double kv = kernel[k + radius];
                int idx = IDX(x, sy, w);
                acc[0] += kv * temp[idx];
                acc[1] += kv * temp[idx + 1];
                acc[2] += kv * temp[idx + 2];
                acc[3] += kv * temp[idx + 3];
            }
            int idx = IDX(x, y, w);
            dst[idx]     = (jbyte)clampd(acc[0]);
            dst[idx + 1] = (jbyte)clampd(acc[1]);
            dst[idx + 2] = (jbyte)clampd(acc[2]);
            dst[idx + 3] = (jbyte)clampd(acc[3]);
        }
    }

    free(kernel);
    free(temp);
    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 7. SHARPEN  — unsharp mask: out = original + strength*(original - blurred)
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeSharpen(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h, jdouble strength) {
    (void)obj;

    /* Reuse the Gaussian blur to produce the "blurred" layer.
     * sharpen call → blur with r=1, σ=1 internally.                  */
    jbyteArray blurred = Java_com_imagefilter_ImageFilterEngine_nativeGaussianBlur(
            env, obj, jpx, w, h, 1, 1.0);
    if (!blurred) return NULL;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *blr;
    if (!pin_input(env, blurred, &blr)) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx,     src, JNI_ABORT);
        (*env)->ReleaseByteArrayElements(env, blurred, blr, JNI_ABORT);
        return NULL;
    }

    int total = w * h;
    for (int i = 0; i < total; i++) {
        int b = i * CHANNELS;
        for (int c = 0; c < 3; c++) {
            int orig = (unsigned char)src[b + c];
            int blur = (unsigned char)blr[b + c];
            /* unsharp mask formula */
            dst[b + c] = (jbyte)clamp((int)(orig + strength * (orig - blur)));
        }
        dst[b + 3] = src[b + 3];    /* preserve alpha */
    }

    (*env)->ReleaseByteArrayElements(env, jpx,     src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, blurred, blr, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result,  dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 8. EDGE DETECTION  — Sobel operator
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeEdgeDetect(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h, jint threshold) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    /* Sobel kernels (3×3) */
    static const int Kx[3][3] = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
    static const int Ky[3][3] = {{-1,-2,-1}, { 0, 0, 0}, { 1, 2, 1}};

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            double gx = 0, gy = 0;
            for (int ky = -1; ky <= 1; ky++) {
                for (int kx = -1; kx <= 1; kx++) {
                    int sx = x + kx; if (sx < 0) sx = 0; if (sx >= w) sx = w - 1;
                    int sy = y + ky; if (sy < 0) sy = 0; if (sy >= h) sy = h - 1;
                    int idx = IDX(sx, sy, w);
                    /* Convert neighbour to luminance */
                    double lum = 0.299 * (unsigned char)src[idx]
                               + 0.587 * (unsigned char)src[idx + 1]
                               + 0.114 * (unsigned char)src[idx + 2];
                    gx += Kx[ky + 1][kx + 1] * lum;
                    gy += Ky[ky + 1][kx + 1] * lum;
                }
            }
            double mag = sqrt(gx * gx + gy * gy);
            unsigned char edge = (mag > threshold) ? 255 : 0;
            int out_idx = IDX(x, y, w);
            dst[out_idx]     = (jbyte)edge;
            dst[out_idx + 1] = (jbyte)edge;
            dst[out_idx + 2] = (jbyte)edge;
            dst[out_idx + 3] = src[out_idx + 3];
        }
    }

    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 9. EMBOSS  — fixed 3×3 emboss convolution kernel
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativeEmboss(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    /* Classic emboss kernel:  -2 -1  0
     *                         -1  1  1
     *                          0  1  2
     * Bias = 128 (neutral grey for flat regions)                        */
    static const int K[3][3] = {{-2,-1,0},{-1,1,1},{0,1,2}};

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            double acc[3] = {128, 128, 128};
            for (int ky = -1; ky <= 1; ky++) {
                for (int kx = -1; kx <= 1; kx++) {
                    int sx = x + kx; if (sx < 0) sx = 0; if (sx >= w) sx = w - 1;
                    int sy = y + ky; if (sy < 0) sy = 0; if (sy >= h) sy = h - 1;
                    int idx = IDX(sx, sy, w);
                    double kv = K[ky + 1][kx + 1];
                    acc[0] += kv * (unsigned char)src[idx];
                    acc[1] += kv * (unsigned char)src[idx + 1];
                    acc[2] += kv * (unsigned char)src[idx + 2];
                }
            }
            int out_idx = IDX(x, y, w);
            dst[out_idx]     = (jbyte)clampd(acc[0]);
            dst[out_idx + 1] = (jbyte)clampd(acc[1]);
            dst[out_idx + 2] = (jbyte)clampd(acc[2]);
            dst[out_idx + 3] = src[out_idx + 3];
        }
    }

    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * 10. PIXELATE  — block-average mosaic
 * ═══════════════════════════════════════════════════════════════════════════*/
JNIEXPORT jbyteArray JNICALL
Java_com_imagefilter_ImageFilterEngine_nativePixelate(
        JNIEnv *env, jobject obj,
        jbyteArray jpx, jint w, jint h, jint blockSize) {
    (void)obj;

    jbyte *src;
    if (!pin_input(env, jpx, &src)) return NULL;

    jbyte *dst;
    jbyteArray result = alloc_result(env, w, h, &dst);
    if (!result) {
        (*env)->ReleaseByteArrayElements(env, jpx, src, JNI_ABORT);
        return NULL;
    }

    /* Walk over the image in block-sized strides */
    for (int by = 0; by < h; by += blockSize) {
        for (int bx = 0; bx < w; bx += blockSize) {

            /* Compute block boundaries (clamped to image edges) */
            int bx_end = bx + blockSize; if (bx_end > w) bx_end = w;
            int by_end = by + blockSize; if (by_end > h) by_end = h;
            int count  = (bx_end - bx) * (by_end - by);

            /* Accumulate channel sums */
            double sum[4] = {0, 0, 0, 0};
            for (int y = by; y < by_end; y++) {
                for (int x = bx; x < bx_end; x++) {
                    int idx = IDX(x, y, w);
                    sum[0] += (unsigned char)src[idx];
                    sum[1] += (unsigned char)src[idx + 1];
                    sum[2] += (unsigned char)src[idx + 2];
                    sum[3] += (unsigned char)src[idx + 3];
                }
            }

            /* Average colour for this block */
            unsigned char avg[4];
            for (int c = 0; c < 4; c++)
                avg[c] = (unsigned char)(sum[c] / count);

            /* Fill every pixel in the block with the average colour */
            for (int y = by; y < by_end; y++) {
                for (int x = bx; x < bx_end; x++) {
                    int idx = IDX(x, y, w);
                    dst[idx]     = (jbyte)avg[0];
                    dst[idx + 1] = (jbyte)avg[1];
                    dst[idx + 2] = (jbyte)avg[2];
                    dst[idx + 3] = (jbyte)avg[3];
                }
            }
        }
    }

    (*env)->ReleaseByteArrayElements(env, jpx,    src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, dst, 0);
    return result;
}