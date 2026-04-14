/*
 * math_engine.c
 *
 * Native implementations for all JNI methods declared in MathEngine.java.
 * Every algorithm is implemented from first principles to demonstrate native
 * computation — no math.h shortcuts except for the sqrt seed.
 *
 * Compile (Linux example):
 *   gcc -O2 -shared -fPIC -o native_libs/libmath_engine.so \
 *       -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
 *       native/math_engine.c
 */

#include "math_engine.h"
#include <stdlib.h>   /* abs, malloc, free */
#include <string.h>   /* memset           */

/* ═══════════════════════════════════════════════════════════════════════════
 * Internal helpers
 * ═══════════════════════════════════════════════════════════════════════════*/

#define PI  3.14159265358979323846
#define E   2.71828182845904523536

/* Reduce angle to [-π, π] for stable trig series */
static double reduce_angle(double x) {
    while (x >  PI) x -= 2.0 * PI;
    while (x < -PI) x += 2.0 * PI;
    return x;
}

/* ─── Power ────────────────────────────────────────────────────────────────
 * Uses exp(exponent * ln(base)) via hand-rolled ln and exp series.
 * For integer exponents uses fast binary exponentiation for exactness.
 */
static double me_ln(double x) {
    /* ln(x) via identity: ln((1+t)/(1-t)) = 2*(t + t³/3 + t⁵/5 + …)
     * where t = (x-1)/(x+1)                                              */
    if (x <= 0.0) return -1e300;
    /* Range reduction: bring x into [0.5, 2) */
    int k = 0;
    while (x >= 2.0) { x /= 2.0; k++; }
    while (x <  0.5) { x *= 2.0; k--; }
    double t = (x - 1.0) / (x + 1.0);
    double t2 = t * t, term = t, sum = t;
    for (int i = 1; i <= 60; i++) {
        term *= t2;
        sum  += term / (2 * i + 1);
    }
    return 2.0 * sum + k * 0.6931471805599453; /* k * ln(2) */
}

static double me_exp(double x) {
    /* exp(x) = Σ xⁿ/n! */
    /* Range reduction: exp(x) = exp(x - k*ln2) * 2^k */
    int k = (int)(x / 0.6931471805599453);
    x -= k * 0.6931471805599453;
    double sum = 1.0, term = 1.0;
    for (int i = 1; i <= 50; i++) {
        term *= x / i;
        sum  += term;
    }
    /* Multiply by 2^k */
    double result = sum;
    if (k >= 0) for (int i = 0; i < k;  i++) result *= 2.0;
    else        for (int i = 0; i < -k; i++) result /= 2.0;
    return result;
}

/* ─── Trigonometry (Taylor series) ─────────────────────────────────────── */

static double me_sin(double x) {
    x = reduce_angle(x);
    double sum = 0.0, term = x;
    for (int i = 1; i <= 30; i++) {
        sum  += term;
        term *= -x * x / ((2 * i) * (2 * i + 1));
    }
    return sum;
}

static double me_cos(double x) {
    x = reduce_angle(x);
    double sum = 0.0, term = 1.0;
    for (int i = 1; i <= 30; i++) {
        sum  += term;
        term *= -x * x / ((2 * i - 1) * (2 * i));
    }
    return sum;
}

/* ─── Square root (Newton-Raphson) ─────────────────────────────────────── */
static double me_sqrt(double x) {
    if (x == 0.0) return 0.0;
    double guess = x > 1.0 ? x / 2.0 : x; /* crude initial guess */
    for (int i = 0; i < 100; i++) {
        double next = 0.5 * (guess + x / guess);
        if (next == guess) break;
        guess = next;
    }
    return guess;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * JNI implementations
 * ═══════════════════════════════════════════════════════════════════════════*/

/* ── Arithmetic ─────────────────────────────────────────────────────────── */

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativePower(JNIEnv *env, jobject obj,
                                           jdouble base, jdouble exponent) {
    (void)env; (void)obj;
    if (base == 0.0) return exponent == 0.0 ? 1.0 : 0.0;
    if (base < 0.0 && exponent != (long)exponent) return 0.0 / 0.0; /* NaN */
    if (base > 0.0)
        return me_exp(exponent * me_ln(base));
    /* base < 0, integer exponent */
    long ie   = (long)exponent;
    double r  = 1.0, b = base < 0 ? -base : base;
    long   e  = ie  < 0 ? -ie : ie;
    while (e) {
        if (e & 1) r *= b;
        b *= b; e >>= 1;
    }
    if (ie < 0) r = 1.0 / r;
    return (ie % 2 != 0) ? -r : r;
}

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeSqrt(JNIEnv *env, jobject obj, jdouble value) {
    (void)env; (void)obj;
    return me_sqrt(value);
}

/* ── Trigonometry ───────────────────────────────────────────────────────── */

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeSin(JNIEnv *env, jobject obj, jdouble radians) {
    (void)env; (void)obj;
    return me_sin(radians);
}

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeCos(JNIEnv *env, jobject obj, jdouble radians) {
    (void)env; (void)obj;
    return me_cos(radians);
}

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeTan(JNIEnv *env, jobject obj, jdouble radians) {
    (void)env; (void)obj;
    double c = me_cos(radians);
    if (c == 0.0) return 1.0 / 0.0; /* +Inf */
    return me_sin(radians) / c;
}

/* ── Calculus ───────────────────────────────────────────────────────────── */

/* Central-difference derivative of sin(x):  (sin(x+h) - sin(x-h)) / (2h) */
JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeDerivative(JNIEnv *env, jobject obj,
                                                jdouble x, jdouble h) {
    (void)env; (void)obj;
    return (me_sin(x + h) - me_sin(x - h)) / (2.0 * h);
}

/* Composite Simpson's Rule for ∫ sin(x) dx from a to b with n sub-intervals */
JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeIntegral(JNIEnv *env, jobject obj,
                                              jdouble a, jdouble b, jint n) {
    (void)env; (void)obj;
    double h   = (b - a) / n;
    double sum = me_sin(a) + me_sin(b);
    for (int i = 1; i < n; i++) {
        double x = a + i * h;
        sum += (i % 2 == 0 ? 2.0 : 4.0) * me_sin(x);
    }
    return (h / 3.0) * sum;
}

/* ── Linear Algebra ─────────────────────────────────────────────────────── */

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeDotProduct(JNIEnv *env, jobject obj,
                                                jdoubleArray ja, jdoubleArray jb) {
    (void)obj;
    jsize  len = (*env)->GetArrayLength(env, ja);
    jdouble *a = (*env)->GetDoubleArrayElements(env, ja, NULL);
    jdouble *b = (*env)->GetDoubleArrayElements(env, jb, NULL);
    double sum = 0.0;
    for (jsize i = 0; i < len; i++) sum += a[i] * b[i];
    (*env)->ReleaseDoubleArrayElements(env, ja, a, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jb, b, JNI_ABORT);
    return sum;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_mathengine_MathEngine_nativeNormalizeVector(JNIEnv *env, jobject obj,
                                                     jdoubleArray jv) {
    (void)obj;
    jsize    len  = (*env)->GetArrayLength(env, jv);
    jdouble *v    = (*env)->GetDoubleArrayElements(env, jv, NULL);

    /* Compute magnitude */
    double mag = 0.0;
    for (jsize i = 0; i < len; i++) mag += v[i] * v[i];
    mag = me_sqrt(mag);

    /* Build result array */
    jdoubleArray result = (*env)->NewDoubleArray(env, len);
    jdouble *r = (*env)->GetDoubleArrayElements(env, result, NULL);

    if (mag > 0.0)
        for (jsize i = 0; i < len; i++) r[i] = v[i] / mag;
    else
        for (jsize i = 0; i < len; i++) r[i] = 0.0;

    (*env)->ReleaseDoubleArrayElements(env, jv,     v, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, result, r, 0);
    return result;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_mathengine_MathEngine_nativeMatrixMultiply(JNIEnv *env, jobject obj,
                                                    jdoubleArray ja, jdoubleArray jb,
                                                    jint size) {
    (void)obj;
    jdouble *a = (*env)->GetDoubleArrayElements(env, ja, NULL);
    jdouble *b = (*env)->GetDoubleArrayElements(env, jb, NULL);

    jdoubleArray jresult = (*env)->NewDoubleArray(env, size * size);
    jdouble     *c       = (*env)->GetDoubleArrayElements(env, jresult, NULL);

    memset(c, 0, sizeof(jdouble) * size * size);
    for (int i = 0; i < size; i++)
        for (int k = 0; k < size; k++) {
            double aik = a[i * size + k];
            for (int j = 0; j < size; j++)
                c[i * size + j] += aik * b[k * size + j];
        }

    (*env)->ReleaseDoubleArrayElements(env, ja,      a, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jb,      b, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jresult, c, 0);
    return jresult;
}

/* ── Statistics ─────────────────────────────────────────────────────────── */

static double compute_mean(jdouble *data, jsize len) {
    double sum = 0.0;
    for (jsize i = 0; i < len; i++) sum += data[i];
    return sum / len;
}

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeMean(JNIEnv *env, jobject obj, jdoubleArray jdata) {
    (void)obj;
    jsize    len  = (*env)->GetArrayLength(env, jdata);
    jdouble *data = (*env)->GetDoubleArrayElements(env, jdata, NULL);
    double   mean = compute_mean(data, len);
    (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);
    return mean;
}

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeVariance(JNIEnv *env, jobject obj, jdoubleArray jdata) {
    (void)obj;
    jsize    len  = (*env)->GetArrayLength(env, jdata);
    jdouble *data = (*env)->GetDoubleArrayElements(env, jdata, NULL);
    double   mean = compute_mean(data, len);
    double   var  = 0.0;
    for (jsize i = 0; i < len; i++) {
        double d = data[i] - mean;
        var += d * d;
    }
    (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);
    return var / len;
}

JNIEXPORT jdouble JNICALL
Java_com_mathengine_MathEngine_nativeStdDev(JNIEnv *env, jobject obj, jdoubleArray jdata) {
    double var = Java_com_mathengine_MathEngine_nativeVariance(env, obj, jdata);
    return me_sqrt(var);
}

/* ── Number Theory ──��───────────────────────────────────────────────────── */

JNIEXPORT jlong JNICALL
Java_com_mathengine_MathEngine_nativeGcd(JNIEnv *env, jobject obj, jlong a, jlong b) {
    (void)env; (void)obj;
    while (b) { jlong t = b; b = a % b; a = t; }
    return a;
}

/* Deterministic Miller-Rabin bases sufficient for all n < 3,317,044,064,679,887,385,961,981 */
static jlong mod_mul(jlong a, jlong b, jlong m) {
    /* Use __int128 if available to avoid overflow */
#ifdef __SIZEOF_INT128__
    return (jlong)((__int128)a * b % m);
#else
    jlong result = 0;
    a %= m;
    while (b > 0) {
        if (b & 1) result = (result + a) % m;
        a = (a * 2) % m;
        b >>= 1;
    }
    return result;
#endif
}

static jlong mod_pow(jlong base, jlong exp, jlong mod) {
    jlong result = 1;
    base %= mod;
    while (exp > 0) {
        if (exp & 1) result = mod_mul(result, base, mod);
        base = mod_mul(base, base, mod);
        exp >>= 1;
    }
    return result;
}

static int miller_rabin_witness(jlong n, jlong a, jlong d, int r) {
    jlong x = mod_pow(a, d, n);
    if (x == 1 || x == n - 1) return 0; /* probably prime */
    for (int i = 0; i < r - 1; i++) {
        x = mod_mul(x, x, n);
        if (x == n - 1) return 0;
    }
    return 1; /* composite */
}

JNIEXPORT jboolean JNICALL
Java_com_mathengine_MathEngine_nativeIsPrime(JNIEnv *env, jobject obj, jlong n) {
    (void)env; (void)obj;
    if (n < 2)  return JNI_FALSE;
    if (n == 2 || n == 3 || n == 5 || n == 7) return JNI_TRUE;
    if (n % 2 == 0 || n % 3 == 0 || n % 5 == 0) return JNI_FALSE;

    /* Write n-1 as d * 2^r */
    jlong d = n - 1; int r = 0;
    while ((d & 1) == 0) { d >>= 1; r++; }

    /* Deterministic witnesses for n < 3.3 × 10²⁴ */
    jlong witnesses[] = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};
    for (int i = 0; i < 12; i++) {
        if (witnesses[i] >= n) continue;
        if (miller_rabin_witness(n, witnesses[i], d, r)) return JNI_FALSE;
    }
    return JNI_TRUE;
}