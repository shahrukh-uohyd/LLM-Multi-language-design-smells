/*
 * log_analyzer.c
 *
 * JNI native implementation of LogAnalyzer.analyzeBatch().
 *
 * Processes a Java array of LogEvent objects, computes per-severity
 * event counts, timestamp range, and most-active source component,
 * then constructs and returns a populated LogSummary Java object.
 *
 * Build: see CMakeLists.txt
 */

#include "LogAnalyzer_jni.h"

#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/* =========================================================================
 * Internal constants — MUST stay in sync with LogEvent.SEVERITY_* in Java
 * ========================================================================= */
#define SEVERITY_DEBUG   0
#define SEVERITY_INFO    1
#define SEVERITY_WARNING 2
#define SEVERITY_ERROR   3
#define SEVERITY_FATAL   4
#define SEVERITY_COUNT   5   /* total number of severity levels */

#define MAX_COMPONENTS   256 /* maximum unique component names tracked */
#define MAX_COMP_LEN     256 /* maximum component name length (bytes)  */

/* =========================================================================
 * Internal data structures
 * ========================================================================= */

/** Tracks occurrence count for a single source component name. */
typedef struct {
    char name[MAX_COMP_LEN];
    int  count;
} ComponentEntry;

/** Aggregated statistics computed over the entire batch. */
typedef struct {
    int      total;
    int      severity_counts[SEVERITY_COUNT];
    int64_t  first_timestamp;
    int64_t  last_timestamp;

    ComponentEntry components[MAX_COMPONENTS];
    int            component_count;
} BatchStats;

/* =========================================================================
 * Helper: throw a named Java exception with a message
 * ========================================================================= */
static void throw_exception(JNIEnv *env, const char *class_name, const char *msg) {
    jclass ex_class = (*env)->FindClass(env, class_name);
    if (ex_class != NULL) {
        (*env)->ThrowNew(env, ex_class, msg);
        (*env)->DeleteLocalRef(env, ex_class);
    }
}

/* =========================================================================
 * Helper: safely convert a jstring to a C string (caller must free())
 * Returns NULL and throws RuntimeException on failure.
 * ========================================================================= */
static char *jstring_to_cstr(JNIEnv *env, jstring jstr) {
    if (jstr == NULL) return NULL;

    const char *utf = (*env)->GetStringUTFChars(env, jstr, NULL);
    if (utf == NULL) {
        throw_exception(env, "java/lang/RuntimeException",
                        "GetStringUTFChars returned NULL");
        return NULL;
    }

    size_t len = strlen(utf);
    char  *buf = (char *)malloc(len + 1);
    if (buf == NULL) {
        (*env)->ReleaseStringUTFChars(env, jstr, utf);
        throw_exception(env, "java/lang/OutOfMemoryError",
                        "Failed to allocate buffer for string");
        return NULL;
    }

    memcpy(buf, utf, len + 1);
    (*env)->ReleaseStringUTFChars(env, jstr, utf);
    return buf;
}

/* =========================================================================
 * Helper: record a component occurrence in stats
 * ========================================================================= */
static void record_component(BatchStats *stats, const char *name) {
    /* Search for existing entry */
    for (int i = 0; i < stats->component_count; i++) {
        if (strncmp(stats->components[i].name, name, MAX_COMP_LEN - 1) == 0) {
            stats->components[i].count++;
            return;
        }
    }

    /* Add new entry if there is room */
    if (stats->component_count < MAX_COMPONENTS) {
        ComponentEntry *entry = &stats->components[stats->component_count++];
        strncpy(entry->name, name, MAX_COMP_LEN - 1);
        entry->name[MAX_COMP_LEN - 1] = '\0';
        entry->count = 1;
    }
}

/* =========================================================================
 * Helper: find the most active component in stats
 * ========================================================================= */
static const ComponentEntry *find_most_active(const BatchStats *stats) {
    const ComponentEntry *best = NULL;
    for (int i = 0; i < stats->component_count; i++) {
        if (best == NULL || stats->components[i].count > best->count) {
            best = &stats->components[i];
        }
    }
    return best;
}

/* =========================================================================
 * JNI field/class cache — resolved once per call (could also be cached
 * in a global initializer via JNI_OnLoad for higher performance)
 * ========================================================================= */
typedef struct {
    /* LogEvent */
    jclass    logEventClass;
    jfieldID  fid_timestamp;
    jfieldID  fid_severity;
    jfieldID  fid_sourceComponent;
    jfieldID  fid_message;

    /* LogSummary */
    jclass    logSummaryClass;
    jmethodID mid_logSummaryInit;
    jfieldID  fid_totalEvents;
    jfieldID  fid_debugCount;
    jfieldID  fid_infoCount;
    jfieldID  fid_warningCount;
    jfieldID  fid_errorCount;
    jfieldID  fid_fatalCount;
    jfieldID  fid_firstTimestamp;
    jfieldID  fid_lastTimestamp;
    jfieldID  fid_mostActiveComponent;
    jfieldID  fid_mostActiveComponentCount;
} JniCache;

/**
 * Resolves all required JNI class/field/method IDs.
 * Returns 0 on success, -1 on failure (exception already pending).
 */
static int resolve_jni_cache(JNIEnv *env, JniCache *cache) {
    /* ---- LogEvent ---- */
    cache->logEventClass = (*env)->FindClass(env,
            "com/example/loganalyzer/LogEvent");
    if (cache->logEventClass == NULL) return -1;  /* NoClassDefFoundError thrown */

    cache->fid_timestamp = (*env)->GetFieldID(env,
            cache->logEventClass, "timestamp", "J");
    if (cache->fid_timestamp == NULL) return -1;

    cache->fid_severity = (*env)->GetFieldID(env,
            cache->logEventClass, "severity", "I");
    if (cache->fid_severity == NULL) return -1;

    cache->fid_sourceComponent = (*env)->GetFieldID(env,
            cache->logEventClass, "sourceComponent", "Ljava/lang/String;");
    if (cache->fid_sourceComponent == NULL) return -1;

    cache->fid_message = (*env)->GetFieldID(env,
            cache->logEventClass, "message", "Ljava/lang/String;");
    if (cache->fid_message == NULL) return -1;

    /* ---- LogSummary ---- */
    cache->logSummaryClass = (*env)->FindClass(env,
            "com/example/loganalyzer/LogSummary");
    if (cache->logSummaryClass == NULL) return -1;

    cache->mid_logSummaryInit = (*env)->GetMethodID(env,
            cache->logSummaryClass, "<init>", "()V");
    if (cache->mid_logSummaryInit == NULL) return -1;

    cache->fid_totalEvents = (*env)->GetFieldID(env,
            cache->logSummaryClass, "totalEvents", "I");
    if (cache->fid_totalEvents == NULL) return -1;

    cache->fid_debugCount = (*env)->GetFieldID(env,
            cache->logSummaryClass, "debugCount", "I");
    if (cache->fid_debugCount == NULL) return -1;

    cache->fid_infoCount = (*env)->GetFieldID(env,
            cache->logSummaryClass, "infoCount", "I");
    if (cache->fid_infoCount == NULL) return -1;

    cache->fid_warningCount = (*env)->GetFieldID(env,
            cache->logSummaryClass, "warningCount", "I");
    if (cache->fid_warningCount == NULL) return -1;

    cache->fid_errorCount = (*env)->GetFieldID(env,
            cache->logSummaryClass, "errorCount", "I");
    if (cache->fid_errorCount == NULL) return -1;

    cache->fid_fatalCount = (*env)->GetFieldID(env,
            cache->logSummaryClass, "fatalCount", "I");
    if (cache->fid_fatalCount == NULL) return -1;

    cache->fid_firstTimestamp = (*env)->GetFieldID(env,
            cache->logSummaryClass, "firstTimestamp", "J");
    if (cache->fid_firstTimestamp == NULL) return -1;

    cache->fid_lastTimestamp = (*env)->GetFieldID(env,
            cache->logSummaryClass, "lastTimestamp", "J");
    if (cache->fid_lastTimestamp == NULL) return -1;

    cache->fid_mostActiveComponent = (*env)->GetFieldID(env,
            cache->logSummaryClass, "mostActiveComponent",
            "Ljava/lang/String;");
    if (cache->fid_mostActiveComponent == NULL) return -1;

    cache->fid_mostActiveComponentCount = (*env)->GetFieldID(env,
            cache->logSummaryClass, "mostActiveComponentCount", "I");
    if (cache->fid_mostActiveComponentCount == NULL) return -1;

    return 0; /* success */
}

/* =========================================================================
 * JNI_OnLoad — optional global native library initialisation hook
 * ========================================================================= */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)vm; (void)reserved;
    return JNI_VERSION_1_8;
}

/* =========================================================================
 * Main JNI entry point
 *
 * Java signature:
 *   public native LogSummary analyzeBatch(LogEvent[] events);
 * ========================================================================= */
JNIEXPORT jobject JNICALL
Java_com_example_loganalyzer_LogAnalyzer_analyzeBatch(
        JNIEnv      *env,
        jobject      thiz,
        jobjectArray events)
{
    (void)thiz; /* not used */

    /* ------------------------------------------------------------------
     * 1. Validate input
     * ------------------------------------------------------------------ */
    if (events == NULL) {
        throw_exception(env, "java/lang/IllegalArgumentException",
                        "events array must not be null");
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, events);
    if (length == 0) {
        throw_exception(env, "java/lang/IllegalArgumentException",
                        "events array must not be empty");
        return NULL;
    }

    /* ------------------------------------------------------------------
     * 2. Resolve JNI class, field, and method IDs
     * ------------------------------------------------------------------ */
    JniCache cache;
    memset(&cache, 0, sizeof(cache));

    if (resolve_jni_cache(env, &cache) != 0) {
        return NULL; /* exception already thrown by resolve_jni_cache */
    }

    /* ------------------------------------------------------------------
     * 3. Initialise statistics accumulator
     * ------------------------------------------------------------------ */
    BatchStats stats;
    memset(&stats, 0, sizeof(stats));
    stats.first_timestamp = INT64_MAX;
    stats.last_timestamp  = INT64_MIN;

    /* ------------------------------------------------------------------
     * 4. Iterate over the event array and accumulate statistics
     * ------------------------------------------------------------------ */
    for (jsize i = 0; i < length; i++) {

        jobject event = (*env)->GetObjectArrayElement(env, events, i);
        if (event == NULL) {
            /* Skip null entries gracefully */
            continue;
        }

        /* Extract primitive fields */
        jlong timestamp = (*env)->GetLongField(env,  event, cache.fid_timestamp);
        jint  severity  = (*env)->GetIntField(env,   event, cache.fid_severity);

        /* Propagate any pending JVM exception immediately */
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, event);
            goto cleanup;
        }

        /* Extract string fields */
        jstring jComponent = (jstring)(*env)->GetObjectField(env,
                                        event, cache.fid_sourceComponent);

        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, event);
            goto cleanup;
        }

        /* Convert component name to C string for hash-map lookup */
        char *component_name = jstring_to_cstr(env, jComponent);
        if (component_name == NULL) {
            /* Exception already thrown by jstring_to_cstr */
            if (jComponent != NULL) (*env)->DeleteLocalRef(env, jComponent);
            (*env)->DeleteLocalRef(env, event);
            goto cleanup;
        }

        /* ---- Accumulate statistics ---- */

        stats.total++;

        /* Update per-severity count */
        if (severity >= SEVERITY_DEBUG && severity < SEVERITY_COUNT) {
            stats.severity_counts[severity]++;
        }

        /* Update timestamp range */
        if ((int64_t)timestamp < stats.first_timestamp)
            stats.first_timestamp = (int64_t)timestamp;
        if ((int64_t)timestamp > stats.last_timestamp)
            stats.last_timestamp  = (int64_t)timestamp;

        /* Update component frequency table */
        record_component(&stats, component_name);

        /* ---- Release resources for this iteration ---- */
        free(component_name);
        if (jComponent != NULL) (*env)->DeleteLocalRef(env, jComponent);

        /*
         * CRITICAL: Release the local ref to the event object inside the
         * loop.  The JNI local reference table has a limited capacity
         * (~512 slots on most JVMs). Always delete local refs in loops
         * to avoid JNI table overflow errors.
         */
        (*env)->DeleteLocalRef(env, event);
    }

    /* ------------------------------------------------------------------
     * 5. Construct the LogSummary return object
     * ------------------------------------------------------------------ */
    jobject summary = (*env)->NewObject(env,
                        cache.logSummaryClass,
                        cache.mid_logSummaryInit);
    if (summary == NULL) {
        /* OutOfMemoryError thrown by JVM */
        goto cleanup;
    }

    /* Populate scalar fields */
    (*env)->SetIntField(env,  summary, cache.fid_totalEvents,   stats.total);
    (*env)->SetIntField(env,  summary, cache.fid_debugCount,    stats.severity_counts[SEVERITY_DEBUG]);
    (*env)->SetIntField(env,  summary, cache.fid_infoCount,     stats.severity_counts[SEVERITY_INFO]);
    (*env)->SetIntField(env,  summary, cache.fid_warningCount,  stats.severity_counts[SEVERITY_WARNING]);
    (*env)->SetIntField(env,  summary, cache.fid_errorCount,    stats.severity_counts[SEVERITY_ERROR]);
    (*env)->SetIntField(env,  summary, cache.fid_fatalCount,    stats.severity_counts[SEVERITY_FATAL]);
    (*env)->SetLongField(env, summary, cache.fid_firstTimestamp,
                         (jlong)(stats.first_timestamp == INT64_MAX ? 0 : stats.first_timestamp));
    (*env)->SetLongField(env, summary, cache.fid_lastTimestamp,
                         (jlong)(stats.last_timestamp  == INT64_MIN ? 0 : stats.last_timestamp));

    /* Populate most-active component */
    const ComponentEntry *best = find_most_active(&stats);
    if (best != NULL) {
        jstring jBestName = (*env)->NewStringUTF(env, best->name);
        if (jBestName == NULL) goto cleanup; /* OutOfMemoryError */

        (*env)->SetObjectField(env, summary,
                               cache.fid_mostActiveComponent, jBestName);
        (*env)->SetIntField(env,    summary,
                               cache.fid_mostActiveComponentCount, best->count);
        (*env)->DeleteLocalRef(env, jBestName);
    }

    /* Release JNI class references and return */
    (*env)->DeleteLocalRef(env, cache.logEventClass);
    (*env)->DeleteLocalRef(env, cache.logSummaryClass);
    return summary;

cleanup:
    /* Reached only when a fatal error or exception occurred mid-loop */
    if (cache.logEventClass   != NULL) (*env)->DeleteLocalRef(env, cache.logEventClass);
    if (cache.logSummaryClass != NULL) (*env)->DeleteLocalRef(env, cache.logSummaryClass);
    return NULL;
}