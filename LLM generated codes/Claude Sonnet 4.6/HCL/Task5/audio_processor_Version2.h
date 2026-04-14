#ifndef AUDIO_PROCESSOR_H
#define AUDIO_PROCESSOR_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Initialise — returns opaque handle (0 on failure) */
JNIEXPORT jlong JNICALL
Java_com_audio_AudioProcessor_nativeInitialize(
        JNIEnv*, jobject, jint sampleRateHz, jint bitDepth,
        jint channels, jboolean bigEndian);

/* Decode compressed audio → raw PCM bytes */
JNIEXPORT jbyteArray JNICALL
Java_com_audio_AudioProcessor_nativeDecodeAudio(
        JNIEnv*, jobject, jlong handle, jbyteArray encodedAudio, jint length);

/* Apply dB gain to PCM data */
JNIEXPORT jbyteArray JNICALL
Java_com_audio_AudioProcessor_nativeApplyGain(
        JNIEnv*, jobject, jlong handle, jbyteArray pcmData, jint length, jfloat gainDb);

/* Resample PCM to a new sample rate */
JNIEXPORT jbyteArray JNICALL
Java_com_audio_AudioProcessor_nativeResample(
        JNIEnv*, jobject, jlong handle,
        jbyteArray pcmData, jint length, jint targetSampleRateHz);

/* Diagnostics from the most recent decode */
JNIEXPORT jlong    JNICALL Java_com_audio_AudioProcessor_nativeGetDurationMs   (JNIEnv*, jobject, jlong);
JNIEXPORT jfloat   JNICALL Java_com_audio_AudioProcessor_nativeGetPeakAmplitude(JNIEnv*, jobject, jlong);
JNIEXPORT jfloat   JNICALL Java_com_audio_AudioProcessor_nativeGetRmsLevel     (JNIEnv*, jobject, jlong);
JNIEXPORT jboolean JNICALL Java_com_audio_AudioProcessor_nativeIsClipped       (JNIEnv*, jobject, jlong);
JNIEXPORT jstring  JNICALL Java_com_audio_AudioProcessor_nativeGetVersion      (JNIEnv*, jobject, jlong);

/* Release native resources */
JNIEXPORT void JNICALL Java_com_audio_AudioProcessor_nativeShutdown(JNIEnv*, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif /* AUDIO_PROCESSOR_H */