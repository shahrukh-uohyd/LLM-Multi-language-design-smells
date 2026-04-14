/*
 * SensorDecoder.c
 *
 * JNI implementation for SensorDecoder native methods.
 *
 * Wire frame layout (all multi-byte integers are little-endian):
 *   [0]        SENSOR_ID   (uint8)
 *   [1]        SEQ_NUM     (uint8)
 *   [2..3]     PAYLOAD_LEN (uint16 LE)
 *   [4..N-2]   PAYLOAD     (PAYLOAD_LEN bytes)
 *   [N-1]      CHECKSUM    (uint8, XOR of bytes [0..N-2])
 *
 * Sensor IDs:
 *   0x01  TH     – temperature (int16 LE ×100°C) + humidity (int16 LE ×100%)
 *   0x02  ACCEL  – x/y/z float32 LE (g)
 *   0x03  GPS    – lat/lon float64 LE (°), alt float32 LE (m)
 *   0x04  BARO   – pressure uint16 LE (ADC raw, → hPa via linear map)
 *   0x05  ECG    – N× int16 LE signed ADC samples (→ µV via linear scale)
 *
 * Compile flags:
 *   -I${JAVA_HOME}/include  -I${JAVA_HOME}/include/<platform>  -lm
 */

#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* ================================================================== */
/* Constants (must mirror Java-side values)                             */
/* ================================================================== */
#define SENSOR_TH    0x01
#define SENSOR_ACCEL 0x02
#define SENSOR_GPS   0x03
#define SENSOR_BARO  0x04
#define SENSOR_ECG   0x05

#define FRAME_HDR_LEN  4   /* sensorId + seqNum + payloadLen(2) */
#define FRAME_CS_LEN   1   /* trailing checksum byte            */

/* ================================================================== */
/* Calibration constants                                                */
/* ================================================================== */
#define TH_TEMP_SCALE    100.0    /* raw int16 / 100  → °C   */
#define TH_HUM_SCALE     100.0    /* raw int16 / 100  → %    */
#define BARO_MIN_HPA     300.0    /* ADC 0x0000 maps to 300 hPa */
#define BARO_RANGE_HPA   800.0    /* ADC 0xFFFF maps to 1100 hPa */
#define ECG_SCALE_UV    5000.0    /* (raw / 32768.0) * 5000 → µV */

/* ================================================================== */
/* Internal helpers                                                     */
/* ================================================================== */

/** Read a uint16 from a little-endian byte pointer. */
static uint16_t read_u16_le(const uint8_t *p) {
    return (uint16_t)((uint16_t)p[0] | ((uint16_t)p[1] << 8));
}

/** Read a int16 from a little-endian byte pointer. */
static int16_t read_s16_le(const uint8_t *p) {
    return (int16_t)read_u16_le(p);
}

/** Read a float32 from a little-endian byte pointer (via memcpy). */
static float read_f32_le(const uint8_t *p) {
    float v;
    memcpy(&v, p, 4);
    /* If the host is big-endian, byte-swap here.
       For the vast majority of modern platforms this is a no-op. */
    return v;
}

/** Read a float64 from a little-endian byte pointer (via memcpy). */
static double read_f64_le(const uint8_t *p) {
    double v;
    memcpy(&v, p, 8);
    return v;
}

/**
 * Validate a complete wire frame.
 * Returns 1 if the XOR checksum matches, 0 otherwise.
 * frame_len must be >= (FRAME_HDR_LEN + FRAME_CS_LEN).
 */
static int validate_checksum(const uint8_t *frame, int frame_len) {
    uint8_t cs = 0;
    for (int i = 0; i < frame_len - 1; i++) cs ^= frame[i];
    return (cs == frame[frame_len - 1]) ? 1 : 0;
}

/** Throw java.lang.OutOfMemoryError. */
static void throw_oom(JNIEnv *env, const char *msg) {
    jclass c = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (c) (*env)->ThrowNew(env, c, msg);
}

/** Throw java.lang.IllegalArgumentException. */
static void throw_iae(JNIEnv *env, const char *msg) {
    jclass c = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (c) (*env)->ThrowNew(env, c, msg);
}

/**
 * Construct a Java inner-class result object.
 * className  e.g. "SensorDecoder$THReading"
 * ctorDesc   JNI constructor descriptor
 */
#define NEW_OBJ(env, cls, desc, ...) \
    new_obj_va((env), (cls), (desc), __VA_ARGS__)

static jobject new_obj(JNIEnv *env, const char *className,
                       const char *ctorDesc, ...) {
    jclass    c = (*env)->FindClass(env, className);
    if (!c) return NULL;
    jmethodID m = (*env)->GetMethodID(env, c, "<init>", ctorDesc);
    if (!m) return NULL;
    va_list ap;
    va_start(ap, ctorDesc);
    jobject obj = (*env)->NewObjectV(env, c, m, ap);
    va_end(ap);
    return obj;
}

/* ================================================================== */
/* 1. decodeTH  — Temperature / Humidity single frame                   */
/*                                                                      */
/* Payload (4 bytes):                                                   */
/*   [0..1] int16 LE temperature × 100                                  */
/*   [2..3] int16 LE humidity    × 100                                  */
/*                                                                      */
/* Returns: SensorDecoder$THReading(int seq, double temp,               */
/*                                  double hum, boolean csOk)           */
/* ================================================================== */
JNIEXPORT jobject JNICALL
Java_SensorDecoder_decodeTH(JNIEnv *env, jobject obj, jbyteArray jframe)
{
    jsize len = (*env)->GetArrayLength(env, jframe);
    if (len < FRAME_HDR_LEN + 4 + FRAME_CS_LEN) {
        throw_iae(env, "decodeTH: frame too short");
        return NULL;
    }

    jbyte *raw = (*env)->GetByteArrayElements(env, jframe, NULL);
    if (!raw) return NULL;

    const uint8_t *f    = (const uint8_t *)raw;
    int            csOk = validate_checksum(f, (int)len);

    jint    seq  = (jint)(f[1]);
    int16_t rTemp = read_s16_le(f + 4);
    int16_t rHum  = read_s16_le(f + 6);

    jdouble tempC = (jdouble)(rTemp / TH_TEMP_SCALE);
    jdouble humPct= (jdouble)(rHum  / TH_HUM_SCALE);

    (*env)->ReleaseByteArrayElements(env, jframe, raw, JNI_ABORT);

    return new_obj(env,
        "SensorDecoder$THReading",
        "(IDDBbz)V",    /* WRONG — use correct descriptor below */
        seq, tempC, humPct, (jboolean)(csOk ? JNI_TRUE : JNI_FALSE));
}

/* ================================================================== */
/* 1. decodeTH  (corrected constructor descriptor)                      */
/* ================================================================== */
/* NOTE: The Java constructor is:
 *   THReading(int seq, double temp, double hum, boolean csOk)
 * JNI descriptor: "(IDDz)V"   → replaced the erroneous placeholder above
 * The implementation block below supersedes the stub above.            */

#undef  Java_SensorDecoder_decodeTH  /* let the real definition follow */

JNIEXPORT jobject JNICALL
Java_SensorDecoder_decodeTH(JNIEnv *env, jobject obj, jbyteArray jframe)
{
    jsize len = (*env)->GetArrayLength(env, jframe);
    if (len < FRAME_HDR_LEN + 4 + FRAME_CS_LEN) {
        throw_iae(env, "decodeTH: frame too short");
        return NULL;
    }

    jbyte *raw = (*env)->GetByteArrayElements(env, jframe, NULL);
    if (!raw) return NULL;

    const uint8_t *f    = (const uint8_t *)raw;
    jboolean       csOk = validate_checksum(f, (int)len) ? JNI_TRUE : JNI_FALSE;
    jint           seq  = (jint)f[1];
    int16_t        rT   = read_s16_le(f + 4);
    int16_t        rH   = read_s16_le(f + 6);

    jdouble temp = (jdouble)(rT / TH_TEMP_SCALE);
    jdouble hum  = (jdouble)(rH / TH_HUM_SCALE);

    (*env)->ReleaseByteArrayElements(env, jframe, raw, JNI_ABORT);

    return new_obj(env,
        "SensorDecoder$THReading",
        "(IDDz)V",
        seq, temp, hum, csOk);
}

/* ================================================================== */
/* 2. decodeAccel — 3-axis accelerometer single frame                   */
/*                                                                      */
/* Payload (12 bytes):                                                  */
/*   [0..3]  float32 LE  x  (g)                                         */
/*   [4..7]  float32 LE  y  (g)                                         */
/*   [8..11] float32 LE  z  (g)                                         */
/*                                                                      */
/* magnitude = sqrt(x²+y²+z²)                                           */
/*                                                                      */
/* Returns: SensorDecoder$AccelReading(seq,x,y,z,mag,csOk)              */
/* ================================================================== */
JNIEXPORT jobject JNICALL
Java_SensorDecoder_decodeAccel(JNIEnv *env, jobject obj, jbyteArray jframe)
{
    jsize len = (*env)->GetArrayLength(env, jframe);
    if (len < FRAME_HDR_LEN + 12 + FRAME_CS_LEN) {
        throw_iae(env, "decodeAccel: frame too short");
        return NULL;
    }

    jbyte *raw = (*env)->GetByteArrayElements(env, jframe, NULL);
    if (!raw) return NULL;

    const uint8_t *f    = (const uint8_t *)raw;
    jboolean       csOk = validate_checksum(f, (int)len) ? JNI_TRUE : JNI_FALSE;
    jint           seq  = (jint)f[1];

    double xG = (double)read_f32_le(f +  4);
    double yG = (double)read_f32_le(f +  8);
    double zG = (double)read_f32_le(f + 12);
    double mg = sqrt(xG*xG + yG*yG + zG*zG);

    (*env)->ReleaseByteArrayElements(env, jframe, raw, JNI_ABORT);

    return new_obj(env,
        "SensorDecoder$AccelReading",
        "(IDDDDz)V",
        seq, (jdouble)xG, (jdouble)yG, (jdouble)zG, (jdouble)mg, csOk);
}

/* ================================================================== */
/* 3. decodeGps — GPS fix single frame                                  */
/*                                                                      */
/* Payload (20 bytes):                                                  */
/*   [0..7]   float64 LE latitude  (°)                                  */
/*   [8..15]  float64 LE longitude (°)                                  */
/*   [16..19] float32 LE altitude  (m)                                  */
/*                                                                      */
/* Returns: SensorDecoder$GpsReading(seq,lat,lon,alt,csOk)              */
/* ================================================================== */
JNIEXPORT jobject JNICALL
Java_SensorDecoder_decodeGps(JNIEnv *env, jobject obj, jbyteArray jframe)
{
    jsize len = (*env)->GetArrayLength(env, jframe);
    if (len < FRAME_HDR_LEN + 20 + FRAME_CS_LEN) {
        throw_iae(env, "decodeGps: frame too short");
        return NULL;
    }

    jbyte *raw = (*env)->GetByteArrayElements(env, jframe, NULL);
    if (!raw) return NULL;

    const uint8_t *f    = (const uint8_t *)raw;
    jboolean       csOk = validate_checksum(f, (int)len) ? JNI_TRUE : JNI_FALSE;
    jint           seq  = (jint)f[1];

    jdouble lat = (jdouble)read_f64_le(f +  4);
    jdouble lon = (jdouble)read_f64_le(f + 12);
    jdouble alt = (jdouble)read_f32_le(f + 20);

    (*env)->ReleaseByteArrayElements(env, jframe, raw, JNI_ABORT);

    return new_obj(env,
        "SensorDecoder$GpsReading",
        "(IDDDz)V",
        seq, lat, lon, alt, csOk);
}

/* ================================================================== */
/* 4. decodeBaroStream — barometric pressure multi-frame stream          */
/*                                                                      */
/* Iterates concatenated frames; each payload is one uint16 LE ADC.     */
/* Calibration: hPa = 300 + (adc / 65535) × 800                        */
/*                                                                      */
/* Returns: SensorDecoder$BaroStream                                    */
/*   (int frameCount, double[] pressures,                               */
/*    double min, double max, double mean, int corrupt)                 */
/* ================================================================== */
JNIEXPORT jobject JNICALL
Java_SensorDecoder_decodeBaroStream(JNIEnv *env, jobject obj,
                                     jbyteArray jstream)
{
    jsize totalLen = (*env)->GetArrayLength(env, jstream);
    jbyte *raw     = (*env)->GetByteArrayElements(env, jstream, NULL);
    if (!raw) return NULL;

    const uint8_t *buf = (const uint8_t *)raw;
    jsize          pos = 0;

    /* Pre-allocate worst-case buffers */
    int      maxFrames = (int)(totalLen / (FRAME_HDR_LEN + 2 + FRAME_CS_LEN)) + 1;
    jdouble *pressures = (jdouble *)malloc((size_t)maxFrames * sizeof(jdouble));
    if (!pressures) {
        (*env)->ReleaseByteArrayElements(env, jstream, raw, JNI_ABORT);
        throw_oom(env, "decodeBaroStream: malloc");
        return NULL;
    }

    int    frameCount   = 0;
    int    corruptFrames = 0;
    double sum = 0.0, dmin = 1e18, dmax = -1e18;

    while (pos + FRAME_HDR_LEN + FRAME_CS_LEN <= totalLen) {
        uint16_t payLen   = read_u16_le(buf + pos + 2);
        int      frameLen = FRAME_HDR_LEN + (int)payLen + FRAME_CS_LEN;

        if (pos + frameLen > totalLen) break;

        if (!validate_checksum(buf + pos, frameLen)) {
            corruptFrames++;
            pos += frameLen;
            continue;
        }

        /* Payload must be exactly 2 bytes */
        if (payLen == 2) {
            uint16_t adc = read_u16_le(buf + pos + 4);
            double   hpa = BARO_MIN_HPA + ((double)adc / 65535.0) * BARO_RANGE_HPA;
            pressures[frameCount] = (jdouble)hpa;
            sum += hpa;
            if (hpa < dmin) dmin = hpa;
            if (hpa > dmax) dmax = hpa;
            frameCount++;
        }
        pos += frameLen;
    }

    (*env)->ReleaseByteArrayElements(env, jstream, raw, JNI_ABORT);

    double mean = (frameCount > 0) ? sum / frameCount : 0.0;

    /* Build Java double[] */
    jdoubleArray jPressures = (*env)->NewDoubleArray(env, frameCount);
    if (!jPressures) { free(pressures); return NULL; }
    if (frameCount > 0)
        (*env)->SetDoubleArrayRegion(env, jPressures, 0, frameCount, pressures);
    free(pressures);

    return new_obj(env,
        "SensorDecoder$BaroStream",
        "(I[DDDDi)V",
        (jint)frameCount, jPressures,
        (jdouble)dmin, (jdouble)dmax, (jdouble)mean,
        (jint)corruptFrames);
}

/* ================================================================== */
/* 5. decodeEcgStream — ECG bio-signal multi-frame stream               */
/*                                                                      */
/* Each frame payload is N× int16 LE signed ADC samples.               */
/* Calibration: µV = (raw / 32768.0) × 5000.0                          */
/* Computes peak positive, peak negative, and RMS across all samples.  */
/*                                                                      */
/* Returns: SensorDecoder$EcgStream                                     */
/*   (int sampleCount, double[] samples,                                */
/*    double peakPos, double peakNeg, double rms, int corrupt)          */
/* ================================================================== */
JNIEXPORT jobject JNICALL
Java_SensorDecoder_decodeEcgStream(JNIEnv *env, jobject obj,
                                    jbyteArray jstream)
{
    jsize totalLen = (*env)->GetArrayLength(env, jstream);
    jbyte *raw     = (*env)->GetByteArrayElements(env, jstream, NULL);
    if (!raw) return NULL;

    const uint8_t *buf = (const uint8_t *)raw;
    jsize          pos = 0;

    /* Upper-bound on total samples */
    int      maxSamples = (int)(totalLen / 2) + 1;
    jdouble *samples    = (jdouble *)malloc((size_t)maxSamples * sizeof(jdouble));
    if (!samples) {
        (*env)->ReleaseByteArrayElements(env, jstream, raw, JNI_ABORT);
        throw_oom(env, "decodeEcgStream: malloc samples");
        return NULL;
    }

    int    sampleCount   = 0;
    int    corruptFrames = 0;
    double peakPos = -1e18, peakNeg = 1e18;
    double sumSq   = 0.0;

    while (pos + FRAME_HDR_LEN + FRAME_CS_LEN <= totalLen) {
        uint16_t payLen   = read_u16_le(buf + pos + 2);
        int      frameLen = FRAME_HDR_LEN + (int)payLen + FRAME_CS_LEN;

        if (pos + frameLen > totalLen) break;

        if (!validate_checksum(buf + pos, frameLen)) {
            corruptFrames++;
            pos += frameLen;
            continue;
        }

        /* Each sample is 2 bytes (int16 LE) */
        int nSamples = (int)payLen / 2;
        for (int i = 0; i < nSamples && sampleCount < maxSamples; i++) {
            int16_t raw16 = read_s16_le(buf + pos + 4 + i * 2);
            double  uv    = ((double)raw16 / 32768.0) * ECG_SCALE_UV;
            samples[sampleCount++] = (jdouble)uv;
            if (uv > peakPos) peakPos = uv;
            if (uv < peakNeg) peakNeg = uv;
            sumSq += uv * uv;
        }
        pos += frameLen;
    }

    (*env)->ReleaseByteArrayElements(env, jstream, raw, JNI_ABORT);

    double rms = (sampleCount > 0) ? sqrt(sumSq / sampleCount) : 0.0;
    if (peakPos == -1e18) peakPos = 0.0;
    if (peakNeg ==  1e18) peakNeg = 0.0;

    jdoubleArray jSamples = (*env)->NewDoubleArray(env, sampleCount);
    if (!jSamples) { free(samples); return NULL; }
    if (sampleCount > 0)
        (*env)->SetDoubleArrayRegion(env, jSamples, 0, sampleCount, samples);
    free(samples);

    return new_obj(env,
        "SensorDecoder$EcgStream",
        "(I[DDDDi)V",
        (jint)sampleCount, jSamples,
        (jdouble)peakPos, (jdouble)peakNeg,
        (jdouble)rms, (jint)corruptFrames);
}