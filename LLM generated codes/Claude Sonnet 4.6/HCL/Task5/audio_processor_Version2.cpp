/**
 * Native implementation of the Java AudioProcessor JNI bindings.
 *
 * In a production build this file links against a real multimedia
 * decoding library (e.g. FFmpeg, libopus, libvorbis).  The stub
 * implementations below simulate correct behaviour so the Java
 * integration can be compiled, tested, and demonstrated without
 * requiring external codec libraries.
 */

#include "audio_processor.h"
#include <cmath>
#include <cstring>
#include <algorithm>
#include <stdexcept>
#include <string>

// ─── Internal per-instance state ─────────────────────────────────────────────
struct AudioContext {
    int      sampleRateHz;
    int      bitDepth;
    int      channels;
    bool     bigEndian;

    // diagnostics populated after each decode
    long     lastDurationMs;
    float    lastPeakAmplitude;
    float    lastRmsLevel;
    bool     lastClipped;

    AudioContext(int sr, int bd, int ch, bool be)
        : sampleRateHz(sr), bitDepth(bd), channels(ch), bigEndian(be),
          lastDurationMs(0), lastPeakAmplitude(0.0f),
          lastRmsLevel(0.0f), lastClipped(false) {}
};

// ─── Helper: compute PCM diagnostics ─────────────────────────────────────────
static void computeDiagnostics(AudioContext* ctx, const jbyte* pcm, int length) {
    if (length == 0) return;

    double sumSq  = 0.0;
    jbyte  peak   = 0;
    bool   clipped = false;

    for (int i = 0; i < length; ++i) {
        jbyte sample = pcm[i];
        jbyte absSample = static_cast<jbyte>(std::abs(static_cast<int>(sample)));
        if (absSample > peak)  peak = absSample;
        if (absSample == 127)  clipped = true;
        sumSq += static_cast<double>(sample) * sample;
    }

    ctx->lastPeakAmplitude = peak / 127.0f;
    ctx->lastRmsLevel      = static_cast<float>(std::sqrt(sumSq / length) / 127.0);
    ctx->lastClipped       = clipped;
    // Duration ≈ samples / (sampleRate × channels × (bitDepth / 8))
    int bytesPerSample     = ctx->bitDepth / 8;
    int bytesPerSecond     = ctx->sampleRateHz * ctx->channels * bytesPerSample;
    ctx->lastDurationMs    = (bytesPerSecond > 0)
                             ? (static_cast<long>(length) * 1000L / bytesPerSecond)
                             : 0L;
}

// ─── JNI implementations ─────────────────────────────────────────────────────

JNIEXPORT jlong JNICALL
Java_com_audio_AudioProcessor_nativeInitialize(
        JNIEnv*, jobject, jint sampleRateHz, jint bitDepth,
        jint channels, jboolean bigEndian) {

    try {
        auto* ctx = new AudioContext(
            static_cast<int>(sampleRateHz),
            static_cast<int>(bitDepth),
            static_cast<int>(channels),
            bigEndian == JNI_TRUE);
        return reinterpret_cast<jlong>(ctx);
    } catch (...) {
        return 0L;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_audio_AudioProcessor_nativeDecodeAudio(
        JNIEnv* env, jobject, jlong handle,
        jbyteArray encodedAudio, jint length) {

    auto* ctx = reinterpret_cast<AudioContext*>(handle);

    // ── Stub: echo back the input as "decoded PCM" for demonstration ──────
    jbyteArray result = env->NewByteArray(length);
    if (!result) return nullptr;

    jbyte* buf = env->GetByteArrayElements(encodedAudio, nullptr);
    env->SetByteArrayRegion(result, 0, length, buf);
    env->ReleaseByteArrayElements(encodedAudio, buf, JNI_ABORT);

    // Compute diagnostics from the (stub) PCM buffer
    jbyte* pcmBuf = env->GetByteArrayElements(result, nullptr);
    computeDiagnostics(ctx, pcmBuf, length);
    env->ReleaseByteArrayElements(result, pcmBuf, JNI_ABORT);

    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_audio_AudioProcessor_nativeApplyGain(
        JNIEnv* env, jobject, jlong handle,
        jbyteArray pcmData, jint length, jfloat gainDb) {

    // Linear gain factor from dB: factor = 10^(gainDb / 20)
    float factor = std::pow(10.0f, gainDb / 20.0f);

    jbyteArray result = env->NewByteArray(length);
    if (!result) return nullptr;

    jbyte* src = env->GetByteArrayElements(pcmData, nullptr);
    jbyte* dst = env->GetByteArrayElements(result,  nullptr);

    for (int i = 0; i < length; ++i) {
        float scaled = static_cast<float>(src[i]) * factor;
        // Clamp to [-127, 127]
        scaled = std::max(-127.0f, std::min(127.0f, scaled));
        dst[i] = static_cast<jbyte>(static_cast<int>(scaled));
    }

    env->ReleaseByteArrayElements(pcmData, src, JNI_ABORT);
    env->ReleaseByteArrayElements(result,  dst, 0);   // write back
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_audio_AudioProcessor_nativeResample(
        JNIEnv* env, jobject, jlong handle,
        jbyteArray pcmData, jint length, jint targetSampleRateHz) {

    auto* ctx   = reinterpret_cast<AudioContext*>(handle);
    float ratio = static_cast<float>(targetSampleRateHz) / ctx->sampleRateHz;
    int newLen  = static_cast<int>(length * ratio);

    jbyteArray result = env->NewByteArray(newLen);
    if (!result) return nullptr;

    jbyte* src = env->GetByteArrayElements(pcmData, nullptr);
    jbyte* dst = env->GetByteArrayElements(result,  nullptr);

    // ── Stub: nearest-neighbour resampling ────────────────────────────────
    for (int i = 0; i < newLen; ++i) {
        int srcIdx = static_cast<int>(i / ratio);
        srcIdx     = std::min(srcIdx, length - 1);
        dst[i]     = src[srcIdx];
    }

    env->ReleaseByteArrayElements(pcmData, src, JNI_ABORT);
    env->ReleaseByteArrayElements(result,  dst, 0);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_audio_AudioProcessor_nativeGetDurationMs(JNIEnv*, jobject, jlong handle) {
    return reinterpret_cast<AudioContext*>(handle)->lastDurationMs;
}

JNIEXPORT jfloat JNICALL
Java_com_audio_AudioProcessor_nativeGetPeakAmplitude(JNIEnv*, jobject, jlong handle) {
    return reinterpret_cast<AudioContext*>(handle)->lastPeakAmplitude;
}

JNIEXPORT jfloat JNICALL
Java_com_audio_AudioProcessor_nativeGetRmsLevel(JNIEnv*, jobject, jlong handle) {
    return reinterpret_cast<AudioContext*>(handle)->lastRmsLevel;
}

JNIEXPORT jboolean JNICALL
Java_com_audio_AudioProcessor_nativeIsClipped(JNIEnv*, jobject, jlong handle) {
    return reinterpret_cast<AudioContext*>(handle)->lastClipped ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_audio_AudioProcessor_nativeGetVersion(JNIEnv* env, jobject, jlong) {
    return env->NewStringUTF("audiodecoder-native/2.0.0");
}

JNIEXPORT void JNICALL
Java_com_audio_AudioProcessor_nativeShutdown(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<AudioContext*>(handle);
}