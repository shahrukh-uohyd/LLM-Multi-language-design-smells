/*
 * device_monitor.cpp
 *
 * JNI native implementation of DeviceMonitor.analyzeUpdates().
 *
 * Processing pipeline:
 *  A) resolve_jni_cache()      — look up all Java class/field/method IDs once
 *  B) extract_updates()        — convert Java StatusUpdate[] → NativeUpdate[]
 *  C) build_device_stats()     — group by deviceId, compute failure metrics
 *  D) filter_and_sort()        — keep devices >= threshold, sort by totalFailures
 *  E) build_report_array()     — construct Java FailureReport[] from native data
 *
 * Failure state definition (mirrors StatusUpdate Java constants):
 *   STATUS_FAILURE  = 2
 *   STATUS_CRITICAL = 3
 *   Any other code is treated as non-failure.
 */

#include <jni.h>

#include <string>
#include <vector>
#include <unordered_map>
#include <algorithm>
#include <stdexcept>
#include <cstdint>
#include <climits>

/* ============================================================================
 * Internal constants — MUST stay in sync with StatusUpdate Java constants
 * ============================================================================ */
static constexpr int STATUS_OK       = 0;
static constexpr int STATUS_WARNING  = 1;
static constexpr int STATUS_FAILURE  = 2;
static constexpr int STATUS_CRITICAL = 3;
static constexpr int STATUS_UNKNOWN  = 4;

/** Returns true if the status code represents a failure state. */
static inline bool is_failure(int code) {
    return (code == STATUS_FAILURE || code == STATUS_CRITICAL);
}

/* ============================================================================
 * Internal data structures
 * ============================================================================ */

/** Mirror of one Java StatusUpdate element — held in native memory. */
struct NativeUpdate {
    std::string device_id;
    int         status_code;
    std::string description;
    int64_t     update_time;
};

/**
 * Accumulated failure statistics for one device.
 * Built incrementally as updates are processed in chronological order.
 */
struct DeviceStats {
    std::string device_id;

    int     total_failure_count      = 0;
    int     max_consecutive_failures = 0;
    int64_t first_failure_time       = INT64_MAX;
    int64_t last_failure_time        = INT64_MIN;
    int     last_failure_code        = 0;
    std::string last_failure_desc;

    /* Running state — tracks the current streak */
    int     current_streak           = 0;

    /** Feed one update into this device's running statistics. */
    void feed(const NativeUpdate& upd) {
        if (is_failure(upd.status_code)) {
            ++total_failure_count;
            ++current_streak;

            if (current_streak > max_consecutive_failures)
                max_consecutive_failures = current_streak;

            if (upd.update_time < first_failure_time)
                first_failure_time = upd.update_time;

            if (upd.update_time > last_failure_time) {
                last_failure_time  = upd.update_time;
                last_failure_code  = upd.status_code;
                last_failure_desc  = upd.description;
            }
        } else {
            /* Non-failure update breaks the consecutive streak */
            current_streak = 0;
        }
    }
};

/* ============================================================================
 * JNI ID cache — resolved once per JNI call
 * ============================================================================ */
struct JniCache {
    /* StatusUpdate */
    jclass    cls_update         = nullptr;
    jfieldID  fid_deviceId       = nullptr;
    jfieldID  fid_statusCode     = nullptr;
    jfieldID  fid_description    = nullptr;
    jfieldID  fid_updateTime     = nullptr;

    /* FailureReport */
    jclass    cls_report                  = nullptr;
    jmethodID mid_report_init             = nullptr;
    jfieldID  fid_rpt_deviceId            = nullptr;
    jfieldID  fid_rpt_totalFailures       = nullptr;
    jfieldID  fid_rpt_maxConsecutive      = nullptr;
    jfieldID  fid_rpt_firstFailureTime    = nullptr;
    jfieldID  fid_rpt_lastFailureTime     = nullptr;
    jfieldID  fid_rpt_lastFailureCode     = nullptr;
    jfieldID  fid_rpt_lastFailureDesc     = nullptr;
};

/* ============================================================================
 * Utility helpers
 * ============================================================================ */

/** Throw a named Java exception. Always returns false for chaining. */
static bool throw_java_exception(JNIEnv*     env,
                                 const char* class_name,
                                 const char* message)
{
    jclass cls = env->FindClass(class_name);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
    return false;
}

/**
 * Convert a jstring → std::string (UTF-8).
 * Sets exception and returns "" on any failure.
 */
static std::string jstring_to_std(JNIEnv* env, jstring jstr)
{
    if (jstr == nullptr) return {};

    const char* utf = env->GetStringUTFChars(jstr, nullptr);
    if (utf == nullptr) {
        throw_java_exception(env, "java/lang/RuntimeException",
                             "GetStringUTFChars returned NULL");
        return {};
    }

    jsize byte_len = env->GetStringUTFLength(jstr);
    std::string result(utf, static_cast<size_t>(byte_len));
    env->ReleaseStringUTFChars(jstr, utf);
    return result;
}

/** Release all local class references held in the cache. */
static void release_jni_cache(JNIEnv* env, JniCache& c)
{
    if (c.cls_update)  env->DeleteLocalRef(c.cls_update);
    if (c.cls_report)  env->DeleteLocalRef(c.cls_report);
}

/* ============================================================================
 * Step A — resolve_jni_cache
 *
 * Looks up every class, field ID, and method ID required for this call.
 * Returns true on success; false if any lookup failed (exception pending).
 * ============================================================================ */
static bool resolve_jni_cache(JNIEnv* env, JniCache& c)
{
    /* ── StatusUpdate ─────────────────────────────────────────────────── */
    c.cls_update = env->FindClass("com/example/monitor/StatusUpdate");
    if (!c.cls_update) return false;

    c.fid_deviceId = env->GetFieldID(
            c.cls_update, "deviceId", "Ljava/lang/String;");
    if (!c.fid_deviceId) return false;

    c.fid_statusCode = env->GetFieldID(
            c.cls_update, "statusCode", "I");
    if (!c.fid_statusCode) return false;

    c.fid_description = env->GetFieldID(
            c.cls_update, "description", "Ljava/lang/String;");
    if (!c.fid_description) return false;

    c.fid_updateTime = env->GetFieldID(
            c.cls_update, "updateTime", "J");
    if (!c.fid_updateTime) return false;

    /* ── FailureReport ────────────────────────────────────────────────── */
    c.cls_report = env->FindClass("com/example/monitor/FailureReport");
    if (!c.cls_report) return false;

    c.mid_report_init = env->GetMethodID(c.cls_report, "<init>", "()V");
    if (!c.mid_report_init) return false;

    c.fid_rpt_deviceId = env->GetFieldID(
            c.cls_report, "deviceId", "Ljava/lang/String;");
    if (!c.fid_rpt_deviceId) return false;

    c.fid_rpt_totalFailures = env->GetFieldID(
            c.cls_report, "totalFailureCount", "I");
    if (!c.fid_rpt_totalFailures) return false;

    c.fid_rpt_maxConsecutive = env->GetFieldID(
            c.cls_report, "maxConsecutiveFailures", "I");
    if (!c.fid_rpt_maxConsecutive) return false;

    c.fid_rpt_firstFailureTime = env->GetFieldID(
            c.cls_report, "firstFailureTime", "J");
    if (!c.fid_rpt_firstFailureTime) return false;

    c.fid_rpt_lastFailureTime = env->GetFieldID(
            c.cls_report, "lastFailureTime", "J");
    if (!c.fid_rpt_lastFailureTime) return false;

    c.fid_rpt_lastFailureCode = env->GetFieldID(
            c.cls_report, "lastFailureCode", "I");
    if (!c.fid_rpt_lastFailureCode) return false;

    c.fid_rpt_lastFailureDesc = env->GetFieldID(
            c.cls_report, "lastFailureDescription", "Ljava/lang/String;");
    if (!c.fid_rpt_lastFailureDesc) return false;

    return true;
}

/* ============================================================================
 * Step B — extract_updates
 *
 * Converts Java StatusUpdate[] → std::vector<NativeUpdate>.
 * Returns false and leaves exception pending on any JNI error.
 * ============================================================================ */
static bool extract_updates(JNIEnv*                    env,
                             jobjectArray               jUpdates,
                             const JniCache&            c,
                             std::vector<NativeUpdate>& out)
{
    jsize length = env->GetArrayLength(jUpdates);
    out.reserve(static_cast<size_t>(length));

    for (jsize i = 0; i < length; ++i) {

        jobject jUpd = env->GetObjectArrayElement(jUpdates, i);
        if (env->ExceptionCheck()) return false;
        if (jUpd == nullptr) continue; // skip null entries gracefully

        /* ── deviceId ── */
        jstring jDevId = static_cast<jstring>(
                env->GetObjectField(jUpd, c.fid_deviceId));
        if (env->ExceptionCheck()) { env->DeleteLocalRef(jUpd); return false; }

        std::string device_id = jstring_to_std(env, jDevId);
        if (jDevId) env->DeleteLocalRef(jDevId);
        if (env->ExceptionCheck()) { env->DeleteLocalRef(jUpd); return false; }

        /* ── statusCode ── */
        jint status_code = env->GetIntField(jUpd, c.fid_statusCode);
        if (env->ExceptionCheck()) { env->DeleteLocalRef(jUpd); return false; }

        /* ── description ── */
        jstring jDesc = static_cast<jstring>(
                env->GetObjectField(jUpd, c.fid_description));
        if (env->ExceptionCheck()) { env->DeleteLocalRef(jUpd); return false; }

        std::string description = jstring_to_std(env, jDesc);
        if (jDesc) env->DeleteLocalRef(jDesc);
        if (env->ExceptionCheck()) { env->DeleteLocalRef(jUpd); return false; }

        /* ── updateTime ── */
        jlong update_time = env->GetLongField(jUpd, c.fid_updateTime);
        if (env->ExceptionCheck()) { env->DeleteLocalRef(jUpd); return false; }

        out.push_back({
            std::move(device_id),
            static_cast<int>(status_code),
            std::move(description),
            static_cast<int64_t>(update_time)
        });

        /*
         * CRITICAL: release local ref inside the loop.
         * JNI local ref table is typically capped at ~512 slots.
         * Large batches will overflow it without explicit cleanup.
         */
        env->DeleteLocalRef(jUpd);
    }

    return true;
}

/* ============================================================================
 * Step C — build_device_stats
 *
 * Groups NativeUpdates by device ID (preserving arrival order per device)
 * and accumulates failure metrics into a DeviceStats map.
 * ============================================================================ */
static std::unordered_map<std::string, DeviceStats>
build_device_stats(const std::vector<NativeUpdate>& updates)
{
    std::unordered_map<std::string, DeviceStats> stats_map;
    stats_map.reserve(updates.size() / 2 + 1); // heuristic pre-allocation

    for (const NativeUpdate& upd : updates) {
        DeviceStats& ds = stats_map[upd.device_id];
        if (ds.device_id.empty()) {
            ds.device_id = upd.device_id; // initialise on first encounter
        }
        ds.feed(upd);
    }

    return stats_map;
}

/* ============================================================================
 * Step D — filter_and_sort
 *
 * Collects DeviceStats entries that meet the failure threshold, then sorts
 * them by totalFailureCount descending (most-failing device first).
 * ============================================================================ */
static std::vector<DeviceStats>
filter_and_sort(const std::unordered_map<std::string, DeviceStats>& stats_map,
                int                                                  threshold)
{
    std::vector<DeviceStats> qualifying;
    qualifying.reserve(stats_map.size());

    for (const auto& kv : stats_map) {
        if (kv.second.total_failure_count >= threshold) {
            qualifying.push_back(kv.second);
        }
    }

    std::sort(qualifying.begin(), qualifying.end(),
              [](const DeviceStats& a, const DeviceStats& b) {
                  if (a.total_failure_count != b.total_failure_count)
                      return a.total_failure_count > b.total_failure_count;
                  /* Secondary sort: longest consecutive streak descending */
                  if (a.max_consecutive_failures != b.max_consecutive_failures)
                      return a.max_consecutive_failures > b.max_consecutive_failures;
                  /* Tertiary sort: device ID ascending for determinism */
                  return a.device_id < b.device_id;
              });

    return qualifying;
}

/* ============================================================================
 * Step E — build_report_array
 *
 * Converts std::vector<DeviceStats> → Java FailureReport[].
 * Returns nullptr and leaves exception pending on any JNI error.
 * ============================================================================ */
static jobjectArray
build_report_array(JNIEnv*                          env,
                   const JniCache&                  c,
                   const std::vector<DeviceStats>&  qualifying)
{
    jsize count = static_cast<jsize>(qualifying.size());

    /* Allocate the result array — all slots initialised to null */
    jobjectArray jArray = env->NewObjectArray(count, c.cls_report, nullptr);
    if (jArray == nullptr) return nullptr; // OutOfMemoryError pending

    for (jsize i = 0; i < count; ++i) {
        const DeviceStats& ds = qualifying[static_cast<size_t>(i)];

        /* Construct a new FailureReport Java object */
        jobject jReport = env->NewObject(c.cls_report, c.mid_report_init);
        if (jReport == nullptr) {
            env->DeleteLocalRef(jArray);
            return nullptr;
        }

        /* ── deviceId ── */
        jstring jDevId = env->NewStringUTF(ds.device_id.c_str());
        if (jDevId == nullptr) {
            env->DeleteLocalRef(jReport);
            env->DeleteLocalRef(jArray);
            return nullptr;
        }
        env->SetObjectField(jReport, c.fid_rpt_deviceId, jDevId);
        env->DeleteLocalRef(jDevId);

        /* ── scalar fields ── */
        env->SetIntField (jReport, c.fid_rpt_totalFailures,
                          ds.total_failure_count);
        env->SetIntField (jReport, c.fid_rpt_maxConsecutive,
                          ds.max_consecutive_failures);
        env->SetLongField(jReport, c.fid_rpt_firstFailureTime,
                          static_cast<jlong>(
                              ds.first_failure_time == INT64_MAX
                              ? 0LL : ds.first_failure_time));
        env->SetLongField(jReport, c.fid_rpt_lastFailureTime,
                          static_cast<jlong>(
                              ds.last_failure_time == INT64_MIN
                              ? 0LL : ds.last_failure_time));
        env->SetIntField (jReport, c.fid_rpt_lastFailureCode,
                          ds.last_failure_code);

        /* ── lastFailureDescription ── */
        jstring jDesc = env->NewStringUTF(ds.last_failure_desc.c_str());
        if (jDesc == nullptr) {
            env->DeleteLocalRef(jReport);
            env->DeleteLocalRef(jArray);
            return nullptr;
        }
        env->SetObjectField(jReport, c.fid_rpt_lastFailureDesc, jDesc);
        env->DeleteLocalRef(jDesc);

        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(jReport);
            env->DeleteLocalRef(jArray);
            return nullptr;
        }

        /* Insert into result array */
        env->SetObjectArrayElement(jArray, i, jReport);

        /* CRITICAL: array retains its own ref — release the local one */
        env->DeleteLocalRef(jReport);
    }

    return jArray;
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
 *   public native FailureReport[] analyzeUpdates(StatusUpdate[] updates,
 *                                                int            failureThreshold);
 * ============================================================================ */
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_monitor_DeviceMonitor_analyzeUpdates(
        JNIEnv*      env,
        jobject      /*thiz*/,
        jobjectArray jUpdates,
        jint         jThreshold)
{
    /* ------------------------------------------------------------------
     * 1. Validate inputs
     * ------------------------------------------------------------------ */
    if (jUpdates == nullptr) {
        throw_java_exception(env,
            "java/lang/IllegalArgumentException",
            "updates array must not be null");
        return nullptr;
    }
    if (jThreshold < 1) {
        throw_java_exception(env,
            "java/lang/IllegalArgumentException",
            "failureThreshold must be >= 1");
        return nullptr;
    }

    /* ------------------------------------------------------------------
     * 2. Resolve JNI IDs
     * ------------------------------------------------------------------ */
    JniCache cache;
    if (!resolve_jni_cache(env, cache)) {
        release_jni_cache(env, cache);
        return nullptr;
    }

    /* ------------------------------------------------------------------
     * 3. Extract Java StatusUpdate[] → native vector
     * ------------------------------------------------------------------ */
    std::vector<NativeUpdate> updates;
    updates.reserve(
        static_cast<size_t>(env->GetArrayLength(jUpdates)));

    if (!extract_updates(env, jUpdates, cache, updates)) {
        release_jni_cache(env, cache);
        return nullptr;
    }

    /* ------------------------------------------------------------------
     * 4. Compute per-device failure statistics
     * ------------------------------------------------------------------ */
    auto stats_map = build_device_stats(updates);

    /* ------------------------------------------------------------------
     * 5. Filter by threshold and sort by failure count descending
     * ------------------------------------------------------------------ */
    auto qualifying = filter_and_sort(stats_map,
                                      static_cast<int>(jThreshold));

    /* ------------------------------------------------------------------
     * 6. Build and return Java FailureReport[]
     * ------------------------------------------------------------------ */
    jobjectArray result = build_report_array(env, cache, qualifying);

    release_jni_cache(env, cache);
    return result; // nullptr only if exception is already pending
}