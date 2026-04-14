#ifndef MATH_ENGINE_H
#define MATH_ENGINE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ── Arithmetic ──────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativePower
    (JNIEnv *, jobject, jdouble base, jdouble exponent);

JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeSqrt
    (JNIEnv *, jobject, jdouble value);

/* ── Trigonometry ────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeSin
    (JNIEnv *, jobject, jdouble radians);

JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeCos
    (JNIEnv *, jobject, jdouble radians);

JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeTan
    (JNIEnv *, jobject, jdouble radians);

/* ── Calculus ────────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeDerivative
    (JNIEnv *, jobject, jdouble x, jdouble h);

JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeIntegral
    (JNIEnv *, jobject, jdouble a, jdouble b, jint n);

/* ── Linear Algebra ──────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeDotProduct
    (JNIEnv *, jobject, jdoubleArray a, jdoubleArray b);

JNIEXPORT jdoubleArray JNICALL Java_com_mathengine_MathEngine_nativeNormalizeVector
    (JNIEnv *, jobject, jdoubleArray v);

JNIEXPORT jdoubleArray JNICALL Java_com_mathengine_MathEngine_nativeMatrixMultiply
    (JNIEnv *, jobject, jdoubleArray a, jdoubleArray b, jint size);

/* ── Statistics ──────────────────────────────────────────────────────────── */
JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeMean
    (JNIEnv *, jobject, jdoubleArray data);

JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeVariance
    (JNIEnv *, jobject, jdoubleArray data);

JNIEXPORT jdouble JNICALL Java_com_mathengine_MathEngine_nativeStdDev
    (JNIEnv *, jobject, jdoubleArray data);

/* ── Number Theory ───────────────────────────────────────────────────────── */
JNIEXPORT jlong    JNICALL Java_com_mathengine_MathEngine_nativeGcd
    (JNIEnv *, jobject, jlong a, jlong b);

JNIEXPORT jboolean JNICALL Java_com_mathengine_MathEngine_nativeIsPrime
    (JNIEnv *, jobject, jlong n);

#ifdef __cplusplus
}
#endif
#endif /* MATH_ENGINE_H */