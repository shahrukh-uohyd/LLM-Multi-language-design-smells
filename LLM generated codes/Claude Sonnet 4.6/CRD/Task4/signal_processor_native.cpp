/**
 * signal_processor_native.cpp
 *
 * JNI implementation of com.app.signal.SignalProcessorNative.
 *
 * Algorithms (all self-contained — no external DSP library required):
 *   - FFT / IFFT  : Cooley–Tukey radix-2 DIT in-place
 *   - FIR filter  : direct-form convolution
 *   - IIR filter  : Direct Form I biquad (second-order section)
 *   - Resample    : linear interpolation
 *   - Statistics  : single-pass mean / variance / RMS / peak / ZCR
 *   - Convolution : direct linear convolution
 *   - Magnitude   : |re + j*im| per bin
 *
 * Compile (Linux):
 *   g++ -std=c++17 -shared -fPIC -O2 -Wall -Wextra              \
 *       -I"${JAVA_HOME}/include"                                  \
 *       -I"${JAVA_HOME}/include/linux"                            \
 *       signal_processor_native.cpp                               \
 *       -lm                                                       \
 *       -o libsignal_processor_native.so
 */

#include <jni.h>

#include <cmath>
#include <cstring>
#include <cstdlib>
#include <cstdio>
#include <memory>
#include <algorithm>
#include <stdexcept>
#include <string>

// ── JNI class / exception paths ───────────────────────────────────────────────
static constexpr const char* SIG_EXCEPTION_CLASS = "com/app/signal/SignalProcessingException";
static constexpr const char* IAE_CLASS           = "java/lang/IllegalArgumentException";
static constexpr const char* OOM_CLASS           = "java/lang/OutOfMemoryError";

// ── Operation ordinals — must match SignalProcessingException.Operation order ──
static constexpr jint OP_FFT        = 0;
static constexpr jint OP_IFFT       = 1;
static constexpr jint OP_FILTER     = 2;
static constexpr jint OP_RESAMPLE   = 3;
static constexpr jint OP_STATISTICS = 4;
static constexpr jint OP_CONVOLVE   = 5;
static constexpr jint OP_UNKNOWN    = 6;

// ── Statistics array indices (must match SignalProcessorNative Java constants) ─
static constexpr int STATS_MEAN     = 0;
static constexpr int STATS_VARIANCE = 1;
static constexpr int STATS_STDDEV   = 2;
static constexpr int STATS_RMS      = 3;
static constexpr int STATS_MIN      = 4;
static constexpr int STATS_MAX      = 5;
static constexpr int STATS_PEAK     = 6;
static constexpr int STATS_ZCR      = 7;
static constexpr int STATS_COUNT    = 8;

// ── M_PI guard ────────────────────────────────────────────────────────────────
#ifndef M_PI
static constexpr double M_PI = 3.14159265358979323846;
#endif

// ═════════════════════════════════════════════════════════════════════════════
// Exception helpers
// ═════════════════════════════════════════════════════════════════════════════

static void throw_iae(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass(IAE_CLASS);
    if (cls) { env->ThrowNew(cls, msg); env->DeleteLocalRef(cls); }
}

static void throw_oom(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass(OOM_CLASS);
    if (cls) { env->ThrowNew(cls, msg); env->DeleteLocalRef(cls); }
}

/**
 * Throws SignalProcessingException(String message, int errorCode, int operationOrdinal).
 * Mirrors the 3-argument constructor in SignalProcessingException.java.
 */
static void throw_signal_exception(JNIEnv*     env,
                                    const char* context,
                                    int         errorCode,
                                    jint        operationOrdinal) {
    jclass cls = env->FindClass(SIG_EXCEPTION_CLASS);
    if (!cls) return;

    jmethodID ctor = env->GetMethodID(cls, "<init>", "(Ljava/lang/String;II)V");
    if (!ctor) { env->DeleteLocalRef(cls); return; }

    jstring jmsg = env->NewStringUTF(context);
    if (!jmsg) { env->DeleteLocalRef(cls); return; }

    jobject ex = env->NewObject(cls, ctor, jmsg, (jint)errorCode, operationOrdinal);
    if (ex) { env->Throw(static_cast<jthrowable>(ex)); env->DeleteLocalRef(ex); }

    env->DeleteLocalRef(jmsg);
    env->DeleteLocalRef(cls);
}

// ═════════════════════════════════════════════════════════════════════════════
// RAII pinned double[] helper
// ═════════════════════════════════════════════════════════════════════════════

struct PinnedDoubleArray {
    JNIEnv*      env;
    jdoubleArray jarray;
    jdouble*     data   = nullptr;
    jsize        length = 0;

    PinnedDoubleArray(JNIEnv* e, jdoubleArray arr) : env(e), jarray(arr) {
        if (arr) {
            data   = env->GetDoubleArrayElements(arr, nullptr);
            length = env->GetArrayLength(arr);
        }
    }
    ~PinnedDoubleArray() {
        if (data) env->ReleaseDoubleArrayElements(jarray, data, JNI_ABORT);
    }
    bool ok()    const { return data   != nullptr; }
    bool empty() const { return length == 0; }

    PinnedDoubleArray(const PinnedDoubleArray&)            = delete;
    PinnedDoubleArray& operator=(const PinnedDoubleArray&) = delete;
};

// ═════════════════════════════════════════════════════════════════════════════
// Helper: build a Java double[] from a native buffer
// ═════════════════════════════════════════════════════════════════════════════

static jdoubleArray make_jdouble_array(JNIEnv* env, const double* data, jsize length) {
    jdoubleArray result = env->NewDoubleArray(length);
    if (!result) { throw_oom(env, "failed to allocate result double[]"); return nullptr; }
    env->SetDoubleArrayRegion(result, 0, length, data);
    return result;
}

// ═════════════════════════════════════════════════════════════════════════════
// Helper: is n a positive power of two?
// ═════════════════════════════════════════════════════════════════════════════

static bool is_power_of_two(int n) {
    return n > 0 && (n & (n - 1)) == 0;
}

// ═════════════════════════════════════════════════════════════════════════════
// Window functions — applied in-place on re[] before FFT
// ═════════════════════════════════════════════════════════════════════════════

static void apply_window(double* samples, int n, int windowType) {
    switch (windowType) {
    case 0: // RECTANGULAR — no-op
        break;
    case 1: // HANN
        for (int i = 0; i < n; ++i)
            samples[i] *= 0.5 * (1.0 - std::cos(2.0 * M_PI * i / (n - 1)));
        break;
    case 2: // HAMMING
        for (int i = 0; i < n; ++i)
            samples[i] *= 0.54 - 0.46 * std::cos(2.0 * M_PI * i / (n - 1));
        break;
    case 3: // BLACKMAN
        for (int i = 0; i < n; ++i)
            samples[i] *= 0.42
                - 0.50 * std::cos(2.0 * M_PI * i / (n - 1))
                + 0.08 * std::cos(4.0 * M_PI * i / (n - 1));
        break;
    }
}

// ══════════════════════════════════════���══════════════════════════════════════
// Core FFT — Cooley–Tukey radix-2 DIT, in-place on interleaved [re,im] pairs
//
// re[] and im[] each have length N (power of two).
// inverse = false → forward DFT; inverse = true → IDFT (un-normalised).
// Caller must divide by N for a normalised IDFT.
// ═════════════════════════════════════════════════════════════════════════════

static void fft_inplace(double* re, double* im, int n, bool inverse) {
    // Bit-reversal permutation
    for (int i = 1, j = 0; i < n; ++i) {
        int bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) { std::swap(re[i], re[j]); std::swap(im[i], im[j]); }
    }

    // Cooley–Tukey butterfly stages
    for (int len = 2; len <= n; len <<= 1) {
        double ang  = 2.0 * M_PI / len * (inverse ? 1.0 : -1.0);
        double wRe  = std::cos(ang);
        double wIm  = std::sin(ang);
        for (int i = 0; i < n; i += len) {
            double curRe = 1.0, curIm = 0.0;
            for (int j = 0; j < len / 2; ++j) {
                double uRe = re[i + j];
                double uIm = im[i + j];
                double vRe = re[i + j + len/2] * curRe - im[i + j + len/2] * curIm;
                double vIm = re[i + j + len/2] * curIm + im[i + j + len/2] * curRe;
                re[i + j]         =  uRe + vRe;
                im[i + j]         =  uIm + vIm;
                re[i + j + len/2] =  uRe - vRe;
                im[i + j + len/2] =  uIm - vIm;
                double nextRe = curRe * wRe - curIm * wIm;
                double nextIm = curRe * wIm + curIm * wRe;
                curRe = nextRe; curIm = nextIm;
            }
        }
    }
    // Normalise IDFT
    if (inverse) {
        for (int i = 0; i < n; ++i) { re[i] /= n; im[i] /= n; }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// computeFFT — Java_com_app_signal_SignalProcessorNative_computeFFT
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_app_signal_SignalProcessorNative_computeFFT(JNIEnv*      env,
                                                      jobject      /*thiz*/,
                                                      jdoubleArray jSamples,
                                                      jint         windowType) {
    if (!jSamples) { throw_iae(env, "samples must not be null"); return nullptr; }

    PinnedDoubleArray samples(env, jSamples);
    if (!samples.ok()) { throw_oom(env, "failed to pin samples"); return nullptr; }
    if (samples.empty()) { throw_iae(env, "samples must not be empty"); return nullptr; }

    int n = static_cast<int>(samples.length);
    if (!is_power_of_two(n)) {
        throw_iae(env, "samples length must be a power of two");
        return nullptr;
    }
    if (windowType < 0 || windowType > 3) {
        throw_iae(env, "windowType must be 0–3");
        return nullptr;
    }

    // Copy input to mutable buffers
    auto re = std::make_unique<double[]>(n);
    auto im = std::make_unique<double[]>(n);
    for (int i = 0; i < n; ++i) { re[i] = samples.data[i]; im[i] = 0.0; }

    // Apply window
    apply_window(re.get(), n, windowType);

    // Forward FFT
    fft_inplace(re.get(), im.get(), n, false);

    // Interleave [re0, im0, re1, im1, ...]
    int outLen = n * 2;
    auto out = std::make_unique<double[]>(outLen);
    for (int i = 0; i < n; ++i) {
        out[2 * i]     = re[i];
        out[2 * i + 1] = im[i];
    }

    return make_jdouble_array(env, out.get(), static_cast<jsize>(outLen));
}

// ═════════════════════════════════════════════════════════════════════════════
// computeIFFT — Java_com_app_signal_SignalProcessorNative_computeIFFT
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_app_signal_SignalProcessorNative_computeIFFT(JNIEnv*      env,
                                                       jobject      /*thiz*/,
                                                       jdoubleArray jComplexSpectrum) {
    if (!jComplexSpectrum) { throw_iae(env, "complexSpectrum must not be null"); return nullptr; }

    PinnedDoubleArray spec(env, jComplexSpectrum);
    if (!spec.ok())   { throw_oom(env, "failed to pin complexSpectrum"); return nullptr; }
    if (spec.empty()) { throw_iae(env, "complexSpectrum must not be empty"); return nullptr; }
    if (spec.length % 2 != 0) {
        throw_iae(env, "complexSpectrum length must be even (interleaved re/im pairs)");
        return nullptr;
    }

    int n = static_cast<int>(spec.length) / 2;
    if (!is_power_of_two(n)) {
        throw_iae(env, "complexSpectrum.length/2 must be a power of two");
        return nullptr;
    }

    auto re = std::make_unique<double[]>(n);
    auto im = std::make_unique<double[]>(n);
    for (int i = 0; i < n; ++i) {
        re[i] = spec.data[2 * i];
        im[i] = spec.data[2 * i + 1];
    }

    fft_inplace(re.get(), im.get(), n, true); // inverse=true, normalises by 1/N

    return make_jdouble_array(env, re.get(), static_cast<jsize>(n));
}

// ═════════════════════════════════════════════════════════════════════════════
// applyFIRFilter — Java_com_app_signal_SignalProcessorNative_applyFIRFilter
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_app_signal_SignalProcessorNative_applyFIRFilter(JNIEnv*      env,
                                                          jobject      /*thiz*/,
                                                          jdoubleArray jSamples,
                                                          jdoubleArray jCoeffs) {
    if (!jSamples) { throw_iae(env, "samples must not be null");      return nullptr; }
    if (!jCoeffs)  { throw_iae(env, "coefficients must not be null"); return nullptr; }

    PinnedDoubleArray samples(env, jSamples);
    PinnedDoubleArray coeffs(env, jCoeffs);
    if (!samples.ok() || !coeffs.ok()) {
        throw_oom(env, "failed to pin FIR arrays");
        return nullptr;
    }
    if (samples.empty()) { throw_iae(env, "samples must not be empty");      return nullptr; }
    if (coeffs.empty())  { throw_iae(env, "coefficients must not be empty"); return nullptr; }

    int n    = static_cast<int>(samples.length);
    int taps = static_cast<int>(coeffs.length);
    int half = taps / 2;

    auto out = std::make_unique<double[]>(n);
    for (int i = 0; i < n; ++i) {
        double acc = 0.0;
        for (int k = 0; k < taps; ++k) {
            int idx = i - k + half;
            double s = (idx >= 0 && idx < n) ? samples.data[idx] : 0.0;
            acc += coeffs.data[k] * s;
        }
        out[i] = acc;
    }

    return make_jdouble_array(env, out.get(), static_cast<jsize>(n));
}

// ═════════════════════════════════════════════════════════════════════════════
// applyIIRFilter — Java_com_app_signal_SignalProcessorNative_applyIIRFilter
//
// Direct Form I biquad: coeffs = [b0, b1, b2, a1, a2], a0 normalised to 1.
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_app_signal_SignalProcessorNative_applyIIRFilter(JNIEnv*      env,
                                                          jobject      /*thiz*/,
                                                          jdoubleArray jSamples,
                                                          jdoubleArray jBiquadCoeffs) {
    if (!jSamples)     { throw_iae(env, "samples must not be null");       return nullptr; }
    if (!jBiquadCoeffs){ throw_iae(env, "biquadCoeffs must not be null");  return nullptr; }

    PinnedDoubleArray samples(env, jSamples);
    PinnedDoubleArray bq(env, jBiquadCoeffs);
    if (!samples.ok() || !bq.ok()) {
        throw_oom(env, "failed to pin IIR arrays");
        return nullptr;
    }
    if (samples.empty()) { throw_iae(env, "samples must not be empty");              return nullptr; }
    if (bq.length != 5)  { throw_iae(env, "biquadCoeffs must have exactly 5 elements [b0,b1,b2,a1,a2]"); return nullptr; }

    double b0 = bq.data[0], b1 = bq.data[1], b2 = bq.data[2];
    double a1 = bq.data[3], a2 = bq.data[4];

    int    n   = static_cast<int>(samples.length);
    auto   out = std::make_unique<double[]>(n);

    double x1 = 0.0, x2 = 0.0; // input delay line
    double y1 = 0.0, y2 = 0.0; // output delay line

    for (int i = 0; i < n; ++i) {
        double x0 = samples.data[i];
        double y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        out[i] = y0;
        x2 = x1; x1 = x0;
        y2 = y1; y1 = y0;
    }

    return make_jdouble_array(env, out.get(), static_cast<jsize>(n));
}

// ═════════════════════════════════════════════════════════════════════════════
// resample — Java_com_app_signal_SignalProcessorNative_resample
//
// Linear interpolation resampling.
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_app_signal_SignalProcessorNative_resample(JNIEnv*      env,
                                                    jobject      /*thiz*/,
                                                    jdoubleArray jSamples,
                                                    jdouble      originalRate,
                                                    jdouble      targetRate) {
    if (!jSamples) { throw_iae(env, "samples must not be null"); return nullptr; }
    if (originalRate <= 0.0) { throw_iae(env, "originalRate must be > 0"); return nullptr; }
    if (targetRate   <= 0.0) { throw_iae(env, "targetRate must be > 0");   return nullptr; }

    PinnedDoubleArray samples(env, jSamples);
    if (!samples.ok())   { throw_oom(env, "failed to pin samples"); return nullptr; }
    if (samples.empty()) { throw_iae(env, "samples must not be empty"); return nullptr; }

    int    inLen  = static_cast<int>(samples.length);
    double ratio  = originalRate / targetRate;   // input samples per output sample
    int    outLen = static_cast<int>(std::ceil(inLen / ratio));
    if (outLen <= 0) outLen = 1;

    auto out = std::make_unique<double[]>(outLen);
    for (int i = 0; i < outLen; ++i) {
        double srcIdx = i * ratio;
        int    lo     = static_cast<int>(srcIdx);
        int    hi     = lo + 1;
        double frac   = srcIdx - lo;
        double sLo    = (lo < inLen) ? samples.data[lo] : 0.0;
        double sHi    = (hi < inLen) ? samples.data[hi] : sLo;
        out[i]        = sLo + frac * (sHi - sLo);
    }

    return make_jdouble_array(env, out.get(), static_cast<jsize>(outLen));
}

// ═════════════════════════════════════════════════════════════════════════════
// computeStatistics — Java_com_app_signal_SignalProcessorNative_computeStatistics
//
// Single-pass Welford variance + RMS, min, max, peak, ZCR.
// Returns double[8] = [mean, variance, stdDev, rms, min, max, peak, zcr]
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_app_signal_SignalProcessorNative_computeStatistics(JNIEnv*      env,
                                                             jobject      /*thiz*/,
                                                             jdoubleArray jSamples) {
    if (!jSamples) { throw_iae(env, "samples must not be null"); return nullptr; }

    PinnedDoubleArray samples(env, jSamples);
    if (!samples.ok())   { throw_oom(env, "failed to pin samples"); return nullptr; }
    if (samples.empty()) { throw_iae(env, "samples must not be empty"); return nullptr; }

    int    n       = static_cast<int>(samples.length);
    double mean    = 0.0;
    double m2      = 0.0;   // Welford M2 accumulator
    double sumSq   = 0.0;
    double minVal  =  std::numeric_limits<double>::max();
    double maxVal  = -std::numeric_limits<double>::max();
    int    zeroCrossings = 0;

    // Welford single-pass
    for (int i = 0; i < n; ++i) {
        double x    = samples.data[i];
        double delta = x - mean;
        mean   += delta / (i + 1);
        double delta2 = x - mean;
        m2     += delta * delta2;
        sumSq  += x * x;
        if (x < minVal) minVal = x;
        if (x > maxVal) maxVal = x;
        if (i > 0) {
            // Zero crossing: consecutive samples have opposite signs
            if ((samples.data[i - 1] >= 0.0 && x < 0.0) ||
                (samples.data[i - 1] <  0.0 && x >= 0.0)) {
                ++zeroCrossings;
            }
        }
    }

    double variance = (n > 1) ? m2 / (n - 1) : 0.0;
    double stdDev   = std::sqrt(variance);
    double rms      = std::sqrt(sumSq / n);
    double peak     = std::max(std::abs(minVal), std::abs(maxVal));
    double zcr      = (n > 1) ? static_cast<double>(zeroCrossings) / (n - 1) : 0.0;

    double stats[STATS_COUNT] = { mean, variance, stdDev, rms, minVal, maxVal, peak, zcr };
    return make_jdouble_array(env, stats, static_cast<jsize>(STATS_COUNT));
}

// ═════════════════════════════════════════════════════════════════════════════
// convolve — Java_com_app_signal_SignalProcessorNative_convolve
//
// Direct linear convolution. Output length = signal.length + kernel.length - 1.
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_app_signal_SignalProcessorNative_convolve(JNIEnv*      env,
                                                    jobject      /*thiz*/,
                                                    jdoubleArray jSignal,
                                                    jdoubleArray jKernel) {
    if (!jSignal) { throw_iae(env, "signal must not be null"); return nullptr; }
    if (!jKernel) { throw_iae(env, "kernel must not be null"); return nullptr; }

    PinnedDoubleArray sig(env, jSignal);
    PinnedDoubleArray ker(env, jKernel);
    if (!sig.ok() || !ker.ok()) { throw_oom(env, "failed to pin convolve arrays"); return nullptr; }
    if (sig.empty()) { throw_iae(env, "signal must not be empty"); return nullptr; }
    if (ker.empty()) { throw_iae(env, "kernel must not be empty"); return nullptr; }

    int sigLen = static_cast<int>(sig.length);
    int kerLen = static_cast<int>(ker.length);
    int outLen = sigLen + kerLen - 1;

    auto out = std::make_unique<double[]>(outLen);
    std::fill(out.get(), out.get() + outLen, 0.0);

    for (int i = 0; i < sigLen; ++i)
        for (int j = 0; j < kerLen; ++j)
            out[i + j] += sig.data[i] * ker.data[j];

    return make_jdouble_array(env, out.get(), static_cast<jsize>(outLen));
}

// ═════════════════════════════════════════════════════════════════════════════
// computeMagnitudeSpectrum
// Java_com_app_signal_SignalProcessorNative_computeMagnitudeSpectrum
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_app_signal_SignalProcessorNative_computeMagnitudeSpectrum(JNIEnv*      env,
                                                                    jobject      /*thiz*/,
                                                                    jdoubleArray jComplexSpectrum) {
    if (!jComplexSpectrum) { throw_iae(env, "complexSpectrum must not be null"); return nullptr; }

    PinnedDoubleArray spec(env, jComplexSpectrum);
    if (!spec.ok())   { throw_oom(env, "failed to pin complexSpectrum"); return nullptr; }
    if (spec.empty()) { throw_iae(env, "complexSpectrum must not be empty"); return nullptr; }
    if (spec.length % 2 != 0) {
        throw_iae(env, "complexSpectrum length must be even");
        return nullptr;
    }

    int    n   = static_cast<int>(spec.length) / 2;
    auto   mag = std::make_unique<double[]>(n);
    for (int i = 0; i < n; ++i) {
        double re = spec.data[2 * i];
        double im = spec.data[2 * i + 1];
        mag[i]    = std::sqrt(re * re + im * im);
    }

    return make_jdouble_array(env, mag.get(), static_cast<jsize>(n));
}

// ═════════════════════════════════════════════════════════════════════════════
// findDominantFrequency
// Java_com_app_signal_SignalProcessorNative_findDominantFrequency
// ═════════════════════════════════════════════════════════════════════════════

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_app_signal_SignalProcessorNative_findDominantFrequency(JNIEnv*      env,
                                                                 jobject      /*thiz*/,
                                                                 jdoubleArray jMagnitude,
                                                                 jdouble      sampleRate) {
    if (!jMagnitude) { throw_iae(env, "magnitudeSpectrum must not be null"); return -1.0; }
    if (sampleRate <= 0.0) { throw_iae(env, "sampleRate must be > 0"); return -1.0; }

    PinnedDoubleArray mag(env, jMagnitude);
    if (!mag.ok())   { throw_oom(env, "failed to pin magnitudeSpectrum"); return -1.0; }
    if (mag.empty()) { throw_iae(env, "magnitudeSpectrum must not be empty"); return -1.0; }

    int    n        = static_cast<int>(mag.length);
    int    peakBin  = 0;
    double peakVal  = mag.data[0];

    // Search only the positive-frequency half (bins 0 .. N/2)
    int halfN = n / 2;
    for (int i = 1; i <= halfN; ++i) {
        if (mag.data[i] > peakVal) {
            peakVal = mag.data[i];
            peakBin = i;
        }
    }

    // Frequency resolution = sampleRate / (2 * halfN) = sampleRate / N
    double freqResolution = sampleRate / (2.0 * halfN);
    return static_cast<jdouble>(peakBin * freqResolution);
}