/**
 * native_db_bridge.cpp
 *
 * JNI implementation for SensorLogJniBridge.
 * Uses SQLite (via sqlite3.h) as the backing native database.
 *
 * Build (Android NDK example):
 *   add_library(sensorlog_native SHARED native_db_bridge.cpp)
 *   target_link_libraries(sensorlog_native sqlite3 log)
 */

#include <jni.h>
#include <sqlite3.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <stdexcept>

#define LOG_TAG  "SensorLogNative"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Executes a plain SQL statement (no bindings).
 * Returns SQLITE_OK on success.
 */
static int execSQL(sqlite3* db, const char* sql) {
    char* errMsg = nullptr;
    int rc = sqlite3_exec(db, sql, nullptr, nullptr, &errMsg);
    if (rc != SQLITE_OK) {
        LOGE("execSQL failed [%d]: %s | SQL: %s", rc, errMsg ? errMsg : "?", sql);
        sqlite3_free(errMsg);
    }
    return rc;
}

/**
 * Creates the sensor_logs table if it does not already exist.
 */
static bool ensureSchema(sqlite3* db) {
    const char* ddl =
        "CREATE TABLE IF NOT EXISTS sensor_logs ("
        "  id           INTEGER PRIMARY KEY AUTOINCREMENT,"
        "  timestamp_ms INTEGER NOT NULL,"
        "  sensor_id    TEXT    NOT NULL,"
        "  value        REAL    NOT NULL,"
        "  unit         TEXT    NOT NULL,"
        "  status       INTEGER NOT NULL DEFAULT 0,"
        "  synced_at    INTEGER NOT NULL DEFAULT (strftime('%s','now'))"
        ");";
    return execSQL(db, ddl) == SQLITE_OK;
}

// ── JNI implementations ───────────────────────────────────────────────────────

extern "C" {

/**
 * com.example.datalogger.jni.SensorLogJniBridge.nativeOpenDatabase
 *
 * Opens (or creates) the SQLite database at the given path.
 * Returns the sqlite3* cast to jlong, or 0 on failure.
 */
JNIEXPORT jlong JNICALL
Java_com_example_datalogger_jni_SensorLogJniBridge_nativeOpenDatabase(
        JNIEnv* env, jobject /* thiz */, jstring jDbPath) {

    const char* dbPath = env->GetStringUTFChars(jDbPath, nullptr);
    if (!dbPath) {
        LOGE("nativeOpenDatabase: failed to get DB path string");
        return 0L;
    }

    sqlite3* db = nullptr;
    int rc = sqlite3_open_v2(
        dbPath, &db,
        SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX,
        nullptr
    );
    env->ReleaseStringUTFChars(jDbPath, dbPath);

    if (rc != SQLITE_OK) {
        LOGE("sqlite3_open_v2 failed [%d]: %s", rc, sqlite3_errmsg(db));
        sqlite3_close(db);
        return 0L;
    }

    // Enable WAL mode for better concurrent read/write performance
    execSQL(db, "PRAGMA journal_mode=WAL;");
    execSQL(db, "PRAGMA synchronous=NORMAL;");

    if (!ensureSchema(db)) {
        LOGE("Schema creation failed — closing database");
        sqlite3_close(db);
        return 0L;
    }

    LOGI("Native database opened successfully");
    return reinterpret_cast<jlong>(db);
}

/**
 * com.example.datalogger.jni.SensorLogJniBridge.nativeSyncLogs
 *
 * Inserts or replaces a batch of sensor logs inside a single transaction.
 * Returns the number of rows successfully written.
 */
JNIEXPORT jint JNICALL
Java_com_example_datalogger_jni_SensorLogJniBridge_nativeSyncLogs(
        JNIEnv*   env,
        jobject   /* thiz */,
        jlong     dbHandle,
        jlongArray  jTimestamps,
        jobjectArray jSensorIds,
        jfloatArray  jValues,
        jobjectArray jUnits,
        jintArray    jStatuses,
        jint         count) {

    auto* db = reinterpret_cast<sqlite3*>(dbHandle);
    if (!db || count <= 0) {
        LOGE("nativeSyncLogs: invalid arguments (db=%p, count=%d)", db, count);
        return 0;
    }

    // ── Acquire primitive array elements ────────────────────────────────────
    jlong*  timestamps = env->GetLongArrayElements(jTimestamps, nullptr);
    jfloat* values     = env->GetFloatArrayElements(jValues,     nullptr);
    jint*   statuses   = env->GetIntArrayElements(jStatuses,   nullptr);

    if (!timestamps || !values || !statuses) {
        LOGE("nativeSyncLogs: failed to pin JNI primitive arrays");
        if (timestamps) env->ReleaseLongArrayElements(jTimestamps, timestamps, JNI_ABORT);
        if (values)     env->ReleaseFloatArrayElements(jValues,    values,     JNI_ABORT);
        if (statuses)   env->ReleaseIntArrayElements(jStatuses,   statuses,   JNI_ABORT);
        return 0;
    }

    // ── Prepare the INSERT statement (reused across all 30 rows) ────────────
    const char* insertSQL =
        "INSERT OR REPLACE INTO sensor_logs "
        "(timestamp_ms, sensor_id, value, unit, status) "
        "VALUES (?, ?, ?, ?, ?);";

    sqlite3_stmt* stmt = nullptr;
    if (sqlite3_prepare_v2(db, insertSQL, -1, &stmt, nullptr) != SQLITE_OK) {
        LOGE("sqlite3_prepare_v2 failed: %s", sqlite3_errmsg(db));
        env->ReleaseLongArrayElements(jTimestamps, timestamps, JNI_ABORT);
        env->ReleaseFloatArrayElements(jValues,    values,     JNI_ABORT);
        env->ReleaseIntArrayElements(jStatuses,   statuses,   JNI_ABORT);
        return 0;
    }

    // ── Begin transaction ────────────────────────────────────────────────────
    if (execSQL(db, "BEGIN IMMEDIATE TRANSACTION;") != SQLITE_OK) {
        LOGE("nativeSyncLogs: failed to begin transaction");
        sqlite3_finalize(stmt);
        env->ReleaseLongArrayElements(jTimestamps, timestamps, JNI_ABORT);
        env->ReleaseFloatArrayElements(jValues,    values,     JNI_ABORT);
        env->ReleaseIntArrayElements(jStatuses,   statuses,   JNI_ABORT);
        return 0;
    }

    int successCount = 0;

    for (int i = 0; i < count; ++i) {
        // Retrieve Java String elements for sensorId and unit
        auto jSensorId = static_cast<jstring>(env->GetObjectArrayElement(jSensorIds, i));
        auto jUnit     = static_cast<jstring>(env->GetObjectArrayElement(jUnits,     i));

        const char* sensorId = jSensorId ? env->GetStringUTFChars(jSensorId, nullptr) : nullptr;
        const char* unit     = jUnit     ? env->GetStringUTFChars(jUnit,     nullptr) : nullptr;

        if (!sensorId || !unit) {
            LOGE("nativeSyncLogs: null sensorId or unit at index %d — skipping", i);
            if (sensorId) env->ReleaseStringUTFChars(jSensorId, sensorId);
            if (unit)     env->ReleaseStringUTFChars(jUnit,     unit);
            if (jSensorId) env->DeleteLocalRef(jSensorId);
            if (jUnit)     env->DeleteLocalRef(jUnit);
            continue;
        }

        // ── Bind parameters (1-indexed) ──────────────────────────────────────
        sqlite3_bind_int64(stmt, 1, static_cast<sqlite3_int64>(timestamps[i]));
        sqlite3_bind_text (stmt, 2, sensorId, -1, SQLITE_TRANSIENT);
        sqlite3_bind_double(stmt, 3, static_cast<double>(values[i]));
        sqlite3_bind_text (stmt, 4, unit,     -1, SQLITE_TRANSIENT);
        sqlite3_bind_int  (stmt, 5, static_cast<int>(statuses[i]));

        int rc = sqlite3_step(stmt);
        if (rc == SQLITE_DONE) {
            ++successCount;
        } else {
            LOGE("sqlite3_step failed at index %d [%d]: %s", i, rc, sqlite3_errmsg(db));
        }

        // Reset for next iteration (does NOT clear bindings, but we rebind anyway)
        sqlite3_reset(stmt);
        sqlite3_clear_bindings(stmt);

        // Release JNI string references
        env->ReleaseStringUTFChars(jSensorId, sensorId);
        env->ReleaseStringUTFChars(jUnit,     unit);
        env->DeleteLocalRef(jSensorId);
        env->DeleteLocalRef(jUnit);
    }

    // ── Commit or rollback ───────────────────────────────────────────────────
    if (successCount == count) {
        execSQL(db, "COMMIT;");
        LOGI("nativeSyncLogs: committed %d/%d sensor logs", successCount, count);
    } else {
        LOGE("nativeSyncLogs: only %d/%d succeeded — rolling back", successCount, count);
        execSQL(db, "ROLLBACK;");
        successCount = 0;   // signal full failure to caller
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────
    sqlite3_finalize(stmt);
    env->ReleaseLongArrayElements(jTimestamps, timestamps, JNI_ABORT);
    env->ReleaseFloatArrayElements(jValues,    values,     JNI_ABORT);
    env->ReleaseIntArrayElements(jStatuses,   statuses,   JNI_ABORT);

    return static_cast<jint>(successCount);
}

/**
 * com.example.datalogger.jni.SensorLogJniBridge.nativeCloseDatabase
 *
 * Closes the SQLite database and frees all associated resources.
 */
JNIEXPORT void JNICALL
Java_com_example_datalogger_jni_SensorLogJniBridge_nativeCloseDatabase(
        JNIEnv* /* env */, jobject /* thiz */, jlong dbHandle) {

    auto* db = reinterpret_cast<sqlite3*>(dbHandle);
    if (db) {
        sqlite3_close_v2(db);   // v2 is safe even with unfinalized statements
        LOGI("Native database closed");
    }
}

} // extern "C"