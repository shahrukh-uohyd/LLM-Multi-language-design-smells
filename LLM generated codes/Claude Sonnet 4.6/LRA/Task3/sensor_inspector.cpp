/*
 * sensor_inspector.cpp
 *
 * JNI native implementation of SensorInspector.inspectReadings().
 *
 * Processes Java arrays of SensorReading and ThresholdConfig objects,
 * identifies threshold violations, and returns a jobjectArray of
 * SensorAlert objects back to Java.
 *
 * Compile: see CMakeLists.txt
 */

#include <jni.h>

#include <string>
#include <vector>
#include <unordered_map>
#include <stdexcept>
#include <cstdint>
#include <limits>

/* ============================================================================
 * Internal constants — MUST stay in sync with SensorAlert Java constants
 * ============================================================================ */
static constexpr jint VIOLATION_BELOW_MIN = 0;
static constexpr jint VIOLATION_ABOVE_MAX = 1;

/* ============================================================================
 * Internal data structures
 * ============================================================================ */

/** Mirror of Java ThresholdConfig — held in native memory during one call. */
struct NativeThreshold {
    double min_value;
    double max_value;
};

/** Describes a single detected threshold violation. */
struct NativeAlert {
    std::string sensor_id;
    double      measurement_value;
    std::string unit;
    int64_t     collection_time;
    double      threshold_violated;
    jint        violation_type;      // VIOLATION_BELOW_MIN | VIOLATION_ABOVE_MAX
};

/* ============================================================================
 * JNI ID cache — resolved once per call from the incoming object instances.
 * For high-frequency calls, promote to global refs via JNI_OnLoad.
 * ============================================================================ */
struct JniCache {
    /* SensorReading */
    jclass    cls_reading        = nullptr;
    jfieldID  fid_rd_sensorId   = nullptr;
    jfieldID  fid_rd_value      = nullptr;
    jfieldID  fid_rd_unit       = nullptr;
    jfieldID  fid_rd_time       = nullptr;

    /* ThresholdConfig */
    jclass    cls_threshold      = nullptr;
    jfieldID  fid_th_sensorId   = nullptr;
    jfieldID  fid_th_minValue   = nullptr;
    jfieldID  fid_th_maxValue   = nullptr;

    /* SensorAlert */
    jclass    cls_alert          = nullptr;
    jmethodID mid_alert_init     = nullptr;
    jfieldID  fid_al_sensorId   = nullptr;
    jfieldID  fid_al_value      = nullptr;
    jfieldID  fid_al_unit       = nullptr;
    jfieldID  fid_al_time       = nullptr;
    jfieldID  fid_al_threshold  = nullptr;
    jfieldID  fid_al_violation  = nullptr;
};

/* ============================================================================
 * Utility helpers
 * ============================================================================ */

/** Throw a named Java exception with a message. Returns false for chaining. */
static bool throw_java_exception(JNIEnv* env,
                                 const char* class_name,
                                 const char* message) {
    jclass cls = env->FindClass(class_name);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
    return false;
}

/**
 * Convert a jstring to a std::string.
 * Throws RuntimeException and returns "" on failure.
 */
static std::string jstring_to_std(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) return {};

    const char* utf = env->GetStringUTFChars(jstr, nullptr);
    if (utf == nullptr) {
        throw_java_exception(env, "java/lang/RuntimeException",
                             "GetStringUTFChars returned NULL");
        return {};
    }

    std::string result(utf);
    env->ReleaseStringUTFChars(jstr, utf);
    return result;
}

/**
 * Resolves all required JNI class, field, and method IDs into cache.
 * Returns true on success; false if any lookup failed (exception is pending).
 */
static bool resolve_jni_cache(JNIEnv* env, JniCache& c) {

    /* ---- SensorReading -------------------------------------------------- */
    c.cls_reading = env->FindClass("com/example/sensor/SensorReading");
    if (!c.cls_reading) return false;

    c.fid_rd_sensorId = env->GetFieldID(c.cls_reading, "sensorId", "Ljava/lang/String;");
    if (!c.fid_rd_sensorId) return false;

    c.fid_rd_value = env->GetFieldID(c.cls_reading, "measurementValue", "D");
    if (!c.fid_rd_value) return false;

    c.fid_rd_unit = env->GetFieldID(c.cls_reading, "unit", "Ljava/lang/String;");
    if (!c.fid_rd_unit) return false;

    c.fid_rd_time = env->GetFieldID(c.cls_reading, "collectionTime", "J");
    if (!c.fid_rd_time) return false;

    /* ---- ThresholdConfig ------------------------------------------------- */
    c.cls_threshold = env->FindClass("com/example/sensor/ThresholdConfig");
    if (!c.cls_threshold) return false;

    c.fid_th_sensorId = env->GetFieldID(c.cls_threshold, "sensorId", "Ljava/lang/String;");
    if (!c.fid_th_sensorId) return false;

    c.fid_th_minValue = env->GetFieldID(c.cls_threshold, "minValue", "D");
    if (!c.fid_th_minValue) return false;

    c.fid_th_maxValue = env->GetFieldID(c.cls_threshold, "maxValue", "D");
    if (!c.fid_th_maxValue) return false;

    /* ---- SensorAlert ----------------------------------------------------- */
    c.cls_alert = env->FindClass("com/example/sensor/SensorAlert");
    if (!c.cls_alert) return false;

    c.mid_alert_init = env->GetMethodID(c.cls_alert, "<init>", "()V");
    if (!c.mid_alert_init) return false;

    c.fid_al_sensorId = env->GetFieldID(c.cls_alert, "sensorId", "Ljava/lang/String;");
    if (!c.fid_al_sensorId) return false;

    c.fid_al_value = env->GetFieldID(c.cls_alert, "measurementValue", "D");
    if (!c.fid_al_value) return false;

    c.fid_al_unit = env->GetFieldID(c.cls_alert, "unit", "Ljava/lang/String;");
    if (!c.fid_al_unit) return false;

    c.fid_al_time = env->GetFieldID(c.cls_alert, "collectionTime", "J");
    if (!c.fid_al_time) return false;

    c.fid_al_threshold = env->GetFieldID(c.cls_alert, "thresholdViolated", "D");
    if (!c.fid_al_threshold) return false;

    c.fid_al_violation = env->GetFieldID(c.cls_alert, "violationType", "I");
    if (!c.fid_al_violation) return false;

    return true;
}

/**
 * Releases all local class references held in the cache.
 * Safe to call even if some entries are nullptr.
 */
static void release_jni_cache(JNIEnv* env, JniCache& c) {
    if (c.cls_reading)   env->DeleteLocalRef(c.cls_reading);
    if (c.cls_threshold) env->DeleteLocalRef(c.cls_threshold);
    if (c.cls_alert)     env->DeleteLocalRef(c.cls_alert);
}

/* ============================================================================
 * Step A — Build a threshold lookup map from the Java ThresholdConfig array.
 *
 * Returns false and leaves exception pending on any JNI error.
 * ============================================================================ */
static bool build_threshold_map(
        JNIEnv*                                      env,
        jobjectArray                                 jThresholds,
        const JniCache&                              c,
        std::unordered_map<std::string, NativeThreshold>& out_map)
{
    jsize len = env->GetArrayLength(jThresholds);

    for (jsize i = 0; i < len; ++i) {
        jobject jt = env->GetObjectArrayElement(jThresholds, i);
        if (jt == nullptr) {
            continue; // skip null entries
        }

        /* Extract sensorId */
        jstring jSensorId = static_cast<jstring>(
                env->GetObjectField(jt, c.fid_th_sensorId));
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jt);
            return false;
        }

        std::string sensor_id = jstring_to_std(env, jSensorId);
        if (jSensorId) env->DeleteLocalRef(jSensorId);

        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jt);
            return false;
        }

        /* Extract min / max */
        jdouble min_val = env->GetDoubleField(jt, c.fid_th_minValue);
        jdouble max_val = env->GetDoubleField(jt, c.fid_th_maxValue);

        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jt);
            return false;
        }

        out_map[sensor_id] = NativeThreshold{ min_val, max_val };

        /* CRITICAL: release local ref inside loop to prevent table overflow */
        env->DeleteLocalRef(jt);
    }

    return true;
}

/* ============================================================================
 * Step B — Iterate SensorReading array and collect violations.
 *
 * Returns false and leaves exception pending on any JNI error.
 * ============================================================================ */
static bool collect_violations(
        JNIEnv*                                            env,
        jobjectArray                                       jReadings,
        const JniCache&                                    c,
        const std::unordered_map<std::string, NativeThreshold>& threshold_map,
        std::vector<NativeAlert>&                          out_alerts)
{
    jsize len = env->GetArrayLength(jReadings);

    for (jsize i = 0; i < len; ++i) {
        jobject jr = env->GetObjectArrayElement(jReadings, i);
        if (jr == nullptr) {
            continue; // skip null readings
        }

        /* ---- Extract SensorReading fields -------------------------------- */
        jstring jSensorId = static_cast<jstring>(
                env->GetObjectField(jr, c.fid_rd_sensorId));
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jr);
            return false;
        }

        std::string sensor_id = jstring_to_std(env, jSensorId);
        if (jSensorId) env->DeleteLocalRef(jSensorId);

        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jr);
            return false;
        }

        jdouble measurement = env->GetDoubleField(jr, c.fid_rd_value);
        jlong   timestamp   = env->GetLongField  (jr, c.fid_rd_time);

        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jr);
            return false;
        }

        jstring jUnit = static_cast<jstring>(
                env->GetObjectField(jr, c.fid_rd_unit));
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jr);
            return false;
        }

        std::string unit = jstring_to_std(env, jUnit);
        if (jUnit) env->DeleteLocalRef(jUnit);

        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jr);
            return false;
        }

        /* ---- Threshold lookup -------------------------------------------- */
        auto it = threshold_map.find(sensor_id);
        if (it == threshold_map.end()) {
            /* No threshold configured for this sensor — skip silently */
            env->DeleteLocalRef(jr);
            continue;
        }

        const NativeThreshold& thresh = it->second;

        /* ---- Violation check --------------------------------------------- */
        if (measurement < thresh.min_value) {
            out_alerts.push_back({
                sensor_id,
                static_cast<double>(measurement),
                unit,
                static_cast<int64_t>(timestamp),
                thresh.min_value,
                VIOLATION_BELOW_MIN
            });
        } else if (measurement > thresh.max_value) {
            out_alerts.push_back({
                sensor_id,
                static_cast<double>(measurement),
                unit,
                static_cast<int64_t>(timestamp),
                thresh.max_value,
                VIOLATION_ABOVE_MAX
            });
        }

        /* CRITICAL: release local ref to prevent JNI table overflow */
        env->DeleteLocalRef(jr);
    }

    return true;
}

/* ============================================================================
 * Step C — Convert native alerts to a Java jobjectArray of SensorAlert objects.
 *
 * Returns nullptr and leaves exception pending on any JNI error.
 * ============================================================================ */
static jobjectArray build_alert_array(
        JNIEnv*                        env,
        const JniCache&                c,
        const std::vector<NativeAlert>& alerts)
{
    jsize count = static_cast<jsize>(alerts.size());

    /* Create the return array, even if empty */
    jobjectArray result = env->NewObjectArray(count, c.cls_alert, nullptr);
    if (result == nullptr) {
        return nullptr; // OutOfMemoryError thrown by JVM
    }

    for (jsize i = 0; i < count; ++i) {
        const NativeAlert& a = alerts[static_cast<size_t>(i)];

        /* Construct a new SensorAlert Java object */
        jobject jAlert = env->NewObject(c.cls_alert, c.mid_alert_init);
        if (jAlert == nullptr) {
            env->DeleteLocalRef(result);
            return nullptr; // OutOfMemoryError
        }

        /* Populate sensorId */
        jstring jSensorId = env->NewStringUTF(a.sensor_id.c_str());
        if (jSensorId == nullptr) {
            env->DeleteLocalRef(jAlert);
            env->DeleteLocalRef(result);
            return nullptr;
        }
        env->SetObjectField(jAlert, c.fid_al_sensorId, jSensorId);
        env->DeleteLocalRef(jSensorId);

        /* Populate unit */
        jstring jUnit = env->NewStringUTF(a.unit.c_str());
        if (jUnit == nullptr) {
            env->DeleteLocalRef(jAlert);
            env->DeleteLocalRef(result);
            return nullptr;
        }
        env->SetObjectField(jAlert, c.fid_al_unit, jUnit);
        env->DeleteLocalRef(jUnit);

        /* Populate scalar fields */
        env->SetDoubleField(jAlert, c.fid_al_value,     a.measurement_value);
        env->SetLongField  (jAlert, c.fid_al_time,      static_cast<jlong>(a.collection_time));
        env->SetDoubleField(jAlert, c.fid_al_threshold, a.threshold_violated);
        env->SetIntField   (jAlert, c.fid_al_violation, a.violation_type);

        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jAlert);
            env->DeleteLocalRef(result);
            return nullptr;
        }

        /* Insert into result array */
        env->SetObjectArrayElement(result, i, jAlert);
        env->DeleteLocalRef(jAlert); // array holds a global ref; local no longer needed
    }

    return result;
}

/* ============================================================================
 * JNI_OnLoad — library initialisation hook
 * ============================================================================ */
extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* /*vm*/, void* /*reserved*/) {
    return JNI_VERSION_1_8;
}

/* ============================================================================
 * Main JNI entry point
 *
 * Java signature:
 *   public native SensorAlert[] inspectReadings(SensorReading[]   readings,
 *                                               ThresholdConfig[] thresholds);
 * ============================================================================ */
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_sensor_SensorInspector_inspectReadings(
        JNIEnv*      env,
        jobject      /*thiz*/,
        jobjectArray jReadings,
        jobjectArray jThresholds)
{
    /* ------------------------------------------------------------------
     * 1. Validate inputs
     * ------------------------------------------------------------------ */
    if (jReadings == nullptr) {
        throw_java_exception(env, "java/lang/IllegalArgumentException",
                             "readings array must not be null");
        return nullptr;
    }
    if (jThresholds == nullptr) {
        throw_java_exception(env, "java/lang/IllegalArgumentException",
                             "thresholds array must not be null");
        return nullptr;
    }

    /* ------------------------------------------------------------------
     * 2. Resolve JNI IDs
     * ------------------------------------------------------------------ */
    JniCache cache;
    if (!resolve_jni_cache(env, cache)) {
        release_jni_cache(env, cache);
        return nullptr; // exception already pending
    }

    /* ------------------------------------------------------------------
     * 3. Build threshold lookup map from Java ThresholdConfig[]
     * ------------------------------------------------------------------ */
    std::unordered_map<std::string, NativeThreshold> threshold_map;
    threshold_map.reserve(
        static_cast<size_t>(env->GetArrayLength(jThresholds)));

    if (!build_threshold_map(env, jThresholds, cache, threshold_map)) {
        release_jni_cache(env, cache);
        return nullptr;
    }

    /* ------------------------------------------------------------------
     * 4. Iterate readings and collect violations
     * ------------------------------------------------------------------ */
    std::vector<NativeAlert> alerts;
    alerts.reserve(static_cast<size_t>(env->GetArrayLength(jReadings)));

    if (!collect_violations(env, jReadings, cache, threshold_map, alerts)) {
        release_jni_cache(env, cache);
        return nullptr;
    }

    /* ------------------------------------------------------------------
     * 5. Convert violations to Java SensorAlert[] and return
     * ------------------------------------------------------------------ */
    jobjectArray result = build_alert_array(env, cache, alerts);

    release_jni_cache(env, cache);
    return result; // nullptr only if exception is already pending
}