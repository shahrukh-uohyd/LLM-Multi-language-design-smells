/*
 * NumericalAnalyzer.c
 *
 * JNI implementation for NumericalAnalyzer native methods.
 *
 * Methods:
 *   Java_NumericalAnalyzer_aggregateStats   – min/max/sum/mean/variance/stddev
 *   Java_NumericalAnalyzer_validateDataset  – NaN / Inf / out-of-range counts
 *   Java_NumericalAnalyzer_normalizeDataset – min-max normalisation to [0,1]
 *   Java_NumericalAnalyzer_histogramDataset – equal-width frequency bins
 *   Java_NumericalAnalyzer_dotProduct       – vector dot product
 *
 * Compile flags expected:
 *   -I${JAVA_HOME}/include  -I${JAVA_HOME}/include/<platform>
 *   -lm  (for sqrt, isnan, isinf)
 */

#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* ================================================================== */
/* Internal utility: throw OutOfMemoryError                            */
/* ================================================================== */
static void throwOOM(JNIEnv *env, const char *msg) {
    jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (cls) (*env)->ThrowNew(env, cls, msg);
}

/* ================================================================== */
/* Internal utility: throw IllegalArgumentException                    */
/* ================================================================== */
static void throwIAE(JNIEnv *env, const char *msg) {
    jclass cls = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (cls) (*env)->ThrowNew(env, cls, msg);
}

/* ================================================================== */
/* 1. aggregateStats                                                    */
/*                                                                      */
/* Returns double[6] = { min, max, sum, mean, variance, stdDev }.      */
/* All NaN when the array is empty.                                     */
/* NaN / Inf elements are skipped in every aggregation.                 */
/* ================================================================== */
JNIEXPORT jdoubleArray JNICALL
Java_NumericalAnalyzer_aggregateStats(JNIEnv   *env,
                                       jobject   obj,
                                       jdoubleArray jdata)
{
    const int RESULT_LEN = 6;
    jdoubleArray result = (*env)->NewDoubleArray(env, RESULT_LEN);
    if (!result) return NULL;

    /* Pre-fill with NaN */
    jdouble nanArr[6];
    for (int i = 0; i < RESULT_LEN; i++) nanArr[i] = (jdouble)NAN;
    (*env)->SetDoubleArrayRegion(env, result, 0, RESULT_LEN, nanArr);

    jsize len = (*env)->GetArrayLength(env, jdata);
    if (len == 0) return result;

    jdouble *data = (*env)->GetDoubleArrayElements(env, jdata, NULL);
    if (!data) return NULL;

    /* ---- Pass 1: min, max, sum, count of finite elements ---- */
    double dmin  =  1.0 / 0.0;   /* +Inf */
    double dmax  = -1.0 / 0.0;   /* -Inf */
    double dsum  = 0.0;
    long   count = 0;

    for (jsize i = 0; i < len; i++) {
        double v = (double)data[i];
        if (isnan(v) || isinf(v)) continue;
        if (v < dmin) dmin = v;
        if (v > dmax) dmax = v;
        dsum += v;
        count++;
    }

    if (count == 0) {
        (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);
        return result;   /* all NaN/Inf — keep NaN results */
    }

    double dmean = dsum / (double)count;

    /* ---- Pass 2: variance (Welford two-pass for numerical stability) ---- */
    double sq_diff_sum = 0.0;
    for (jsize i = 0; i < len; i++) {
        double v = (double)data[i];
        if (isnan(v) || isinf(v)) continue;
        double diff = v - dmean;
        sq_diff_sum += diff * diff;
    }
    double variance = sq_diff_sum / (double)count;   /* population variance */
    double stddev   = sqrt(variance);

    (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);

    jdouble out[6] = {
        (jdouble)dmin,
        (jdouble)dmax,
        (jdouble)dsum,
        (jdouble)dmean,
        (jdouble)variance,
        (jdouble)stddev
    };
    (*env)->SetDoubleArrayRegion(env, result, 0, RESULT_LEN, out);
    return result;
}

/* ================================================================== */
/* 2. validateDataset                                                   */
/*                                                                      */
/* Returns int[4] = { nanCount, infCount, outOfRangeCount, validCount } */
/* "valid" means finite AND within [lo, hi].                            */
/* ================================================================== */
JNIEXPORT jintArray JNICALL
Java_NumericalAnalyzer_validateDataset(JNIEnv      *env,
                                        jobject      obj,
                                        jdoubleArray jdata,
                                        jdouble      jlo,
                                        jdouble      jhi)
{
    jintArray result = (*env)->NewIntArray(env, 4);
    if (!result) return NULL;

    jsize len = (*env)->GetArrayLength(env, jdata);
    if (len == 0) return result;

    jdouble *data = (*env)->GetDoubleArrayElements(env, jdata, NULL);
    if (!data) return NULL;

    double lo = (double)jlo;
    double hi = (double)jhi;

    int nanCount = 0, infCount = 0, oorCount = 0, validCount = 0;

    for (jsize i = 0; i < len; i++) {
        double v = (double)data[i];
        if (isnan(v)) {
            nanCount++;
        } else if (isinf(v)) {
            infCount++;
        } else if (v < lo || v > hi) {
            oorCount++;
        } else {
            validCount++;
        }
    }

    (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);

    jint out[4] = {
        (jint)nanCount,
        (jint)infCount,
        (jint)oorCount,
        (jint)validCount
    };
    (*env)->SetIntArrayRegion(env, result, 0, 4, out);
    return result;
}

/* ================================================================== */
/* 3. normalizeDataset                                                  */
/*                                                                      */
/* Min-max normalisation: out[i] = (v - min) / (max - min)             */
/* When min == max (constant dataset) every output is 0.0.             */
/* NaN / Inf values are propagated as-is.                               */
/* ================================================================== */
JNIEXPORT jdoubleArray JNICALL
Java_NumericalAnalyzer_normalizeDataset(JNIEnv      *env,
                                         jobject      obj,
                                         jdoubleArray jdata)
{
    jsize len = (*env)->GetArrayLength(env, jdata);

    jdoubleArray result = (*env)->NewDoubleArray(env, len);
    if (!result) return NULL;
    if (len == 0) return result;

    jdouble *data = (*env)->GetDoubleArrayElements(env, jdata, NULL);
    if (!data) return NULL;

    /* Find finite min/max */
    double dmin =  1.0 / 0.0;
    double dmax = -1.0 / 0.0;
    for (jsize i = 0; i < len; i++) {
        double v = (double)data[i];
        if (isnan(v) || isinf(v)) continue;
        if (v < dmin) dmin = v;
        if (v > dmax) dmax = v;
    }

    double range = dmax - dmin;

    jdouble *out = (jdouble *)malloc((size_t)len * sizeof(jdouble));
    if (!out) {
        (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);
        throwOOM(env, "normalizeDataset: malloc failed");
        return NULL;
    }

    for (jsize i = 0; i < len; i++) {
        double v = (double)data[i];
        if (isnan(v) || isinf(v)) {
            out[i] = (jdouble)v;                /* propagate as-is */
        } else if (range == 0.0) {
            out[i] = (jdouble)0.0;              /* degenerate: constant array */
        } else {
            out[i] = (jdouble)((v - dmin) / range);
        }
    }

    (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);
    (*env)->SetDoubleArrayRegion(env, result, 0, len, out);
    free(out);
    return result;
}

/* ================================================================== */
/* 4. histogramDataset                                                  */
/*                                                                      */
/* Equal-width bins spanning [min, max].                                */
/* NaN / Inf elements are skipped.                                      */
/* The last bin is closed on the right: [lo, hi].                       */
/* ================================================================== */
JNIEXPORT jintArray JNICALL
Java_NumericalAnalyzer_histogramDataset(JNIEnv      *env,
                                         jobject      obj,
                                         jdoubleArray jdata,
                                         jint         jbins)
{
    int bins = (int)jbins;
    if (bins < 1) {
        throwIAE(env, "histogramDataset: bins must be >= 1");
        return NULL;
    }

    jintArray result = (*env)->NewIntArray(env, bins);
    if (!result) return NULL;

    jsize len = (*env)->GetArrayLength(env, jdata);
    if (len == 0) return result;

    jdouble *data = (*env)->GetDoubleArrayElements(env, jdata, NULL);
    if (!data) return NULL;

    /* Find finite min/max */
    double dmin =  1.0 / 0.0;
    double dmax = -1.0 / 0.0;
    for (jsize i = 0; i < len; i++) {
        double v = (double)data[i];
        if (isnan(v) || isinf(v)) continue;
        if (v < dmin) dmin = v;
        if (v > dmax) dmax = v;
    }

    jint *counts = (jint *)calloc((size_t)bins, sizeof(jint));
    if (!counts) {
        (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);
        throwOOM(env, "histogramDataset: calloc failed");
        return NULL;
    }

    double range = dmax - dmin;

    for (jsize i = 0; i < len; i++) {
        double v = (double)data[i];
        if (isnan(v) || isinf(v)) continue;

        int bin;
        if (range == 0.0) {
            bin = 0;                                  /* all same → bin 0 */
        } else {
            bin = (int)(((v - dmin) / range) * bins);
            if (bin >= bins) bin = bins - 1;          /* clamp max value */
        }
        counts[bin]++;
    }

    (*env)->ReleaseDoubleArrayElements(env, jdata, data, JNI_ABORT);
    (*env)->SetIntArrayRegion(env, result, 0, bins, counts);
    free(counts);
    return result;
}

/* ================================================================== */
/* 5. dotProduct                                                        */
/*                                                                      */
/* Scalar dot product: Σ a[i] * b[i]                                   */
/* The arrays must have equal length; if not, a Java                    */
/* IllegalArgumentException is thrown.                                  */
/* ================================================================== */
JNIEXPORT jdouble JNICALL
Java_NumericalAnalyzer_dotProduct(JNIEnv      *env,
                                   jobject      obj,
                                   jdoubleArray ja,
                                   jdoubleArray jb)
{
    jsize lenA = (*env)->GetArrayLength(env, ja);
    jsize lenB = (*env)->GetArrayLength(env, jb);

    if (lenA != lenB) {
        throwIAE(env, "dotProduct: arrays must have equal length");
        return (jdouble)NAN;
    }
    if (lenA == 0) return (jdouble)0.0;

    jdouble *a = (*env)->GetDoubleArrayElements(env, ja, NULL);
    if (!a) return (jdouble)NAN;
    jdouble *b = (*env)->GetDoubleArrayElements(env, jb, NULL);
    if (!b) {
        (*env)->ReleaseDoubleArrayElements(env, ja, a, JNI_ABORT);
        return (jdouble)NAN;
    }

    double acc = 0.0;
    for (jsize i = 0; i < lenA; i++) {
        acc += (double)a[i] * (double)b[i];
    }

    (*env)->ReleaseDoubleArrayElements(env, ja, a, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jb, b, JNI_ABORT);
    return (jdouble)acc;
}