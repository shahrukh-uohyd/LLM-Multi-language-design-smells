/**
 * transaction_storage_native.cpp
 *
 * JNI implementation for NativeStorageBridge.
 * Persists TransactionRecord objects to a SQLite database with per-row
 * ACID compliance: each commitToStorage() call wraps its INSERT inside
 * its own BEGIN IMMEDIATE … COMMIT / ROLLBACK transaction.
 *
 * Build (Android NDK CMakeLists.txt):
 *   add_library(transaction_storage_native SHARED transaction_storage_native.cpp)
 *   target_link_libraries(transaction_storage_native sqlite3 log)
 */

#include <jni.h>
#include <sqlite3.h>
#include <android/log.h>
#include <cstring>
#include <ctime>
#include <string>

#include "native_storage.h"

#define LOG_TAG  "TxStorageNative"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ── SQL constants ─────────────────────────────────────────────────────────────

static constexpr const char* SQL_CREATE_TABLE =
    "CREATE TABLE IF NOT EXISTS transaction_records ("
    "  transaction_id TEXT    PRIMARY KEY,"
    "  timestamp_ms   INTEGER NOT NULL,"
    "  amount_micros  INTEGER NOT NULL,"
    "  currency       TEXT    NOT NULL,"
    "  type           INTEGER NOT NULL DEFAULT 0,"
    "  status         INTEGER NOT NULL DEFAULT 0,"
    "  account_ref    TEXT    NOT NULL,"
    "  committed_at   INTEGER NOT NULL"
    ");";

static constexpr const char* SQL_INSERT =
    "INSERT OR REPLACE INTO transaction_records "
    "(transaction_id, timestamp_ms, amount_micros, currency, "
    " type, status, account_ref, committed_at) "
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

// ── Helpers ───────────────────────────────────────────────────────────────────

static int execSQL(sqlite3* db, const char* sql) {
    char* err = nullptr;
    int rc = sqlite3_exec(db, sql, nullptr, nullptr, &err);
    if (rc != SQLITE_OK) {
        LOGE("execSQL failed [%d]: %s | SQL: %s", rc, err ? err : "?", sql);
        sqlite3_free(err);
    }
    return rc;
}

static int64_t nowMs() {
    struct timespec ts{};
    clock_gettime(CLOCK_REALTIME, &ts);
    return static_cast<int64_t>(ts.tv_sec) * 1000LL
         + static_cast<int64_t>(ts.tv_nsec) / 1'000'000LL;
}

// ── JNI implementations ───────────────────────────────────────────────────────

extern "C" {

// ─────────────────────────────────────────────────────────────────────────────
// NativeStorageBridge.nativeOpenStorage()
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jlong JNICALL
Java_com_example_dbridge_jni_NativeStorageBridge_nativeOpenStorage(
        JNIEnv* env, jobject /*thiz*/, jstring jStoragePath) {

    const char* path = env->GetStringUTFChars(jStoragePath, nullptr);
    if (!path) {
        LOGE("nativeOpenStorage: cannot read storage path string");
        return 0L;
    }

    sqlite3* db = nullptr;
    int rc = sqlite3_open_v2(
        path, &db,
        SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX,
        nullptr);
    env->ReleaseStringUTFChars(jStoragePath, path);

    if (rc != SQLITE_OK) {
        LOGE("nativeOpenStorage: sqlite3_open_v2 failed [%d]: %s", rc, sqlite3_errmsg(db));
        sqlite3_close(db);
        return 0L;
    }

    // Performance pragmas
    execSQL(db, "PRAGMA journal_mode=WAL;");
    execSQL(db, "PRAGMA synchronous=FULL;");  // FULL = fsync on each COMMIT (ACID)

    // Schema bootstrap
    if (execSQL(db, SQL_CREATE_TABLE) != SQLITE_OK) {
        sqlite3_close(db);
        return 0L;
    }

    // Pre-compile the INSERT statement — reused across all 25 commitToStorage() calls
    sqlite3_stmt* stmt = nullptr;
    rc = sqlite3_prepare_v2(db, SQL_INSERT, -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        LOGE("nativeOpenStorage: prepare INSERT failed [%d]: %s", rc, sqlite3_errmsg(db));
        sqlite3_close(db);
        return 0L;
    }

    auto* ctx = new (std::nothrow) NativeStorageContext{ db, stmt, true };
    if (!ctx) {
        LOGE("nativeOpenStorage: out of memory");
        sqlite3_finalize(stmt);
        sqlite3_close(db);
        return 0L;
    }

    LOGI("nativeOpenStorage: storage opened (ctx=%p)", ctx);
    return reinterpret_cast<jlong>(ctx);
}

// ─────────────────────────────────────────────────────────────────────────────
// NativeStorageBridge.commitToStorage()
//
// Per-row ACID strategy:
//   BEGIN IMMEDIATE   — acquires a write lock before any work begins
//   <bind & step>     — execute the INSERT OR REPLACE
//   COMMIT            — flush to WAL + fsync (PRAGMA synchronous=FULL)
//   ROLLBACK          — issued only on step failure; leaves prior rows intact
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT jobject JNICALL
Java_com_example_dbridge_jni_NativeStorageBridge_commitToStorage(
        JNIEnv*  env,
        jobject  /*thiz*/,
        jlong    jStorageHandle,
        jstring  jTxId,
        jlong    jTimestampMs,
        jlong    jAmountMicros,
        jstring  jCurrency,
        jint     jType,
        jint     jStatus,
        jstring  jAccountRef) {

    // ── Look up Java CommitResult class and constructor ───────────────────────
    jclass resultClass = env->FindClass(
        "com/example/dbridge/model/CommitResult");
    jmethodID resultCtor = env->GetMethodID(
        resultClass, "<init>", "(Ljava/lang/String;ZILjava/lang/String;J)V");

    // Helper lambda: constructs a CommitResult Java object
    auto makeResult = [&](const char* txId,
                          bool        committed,
                          int         code,
                          const char* msg,
                          int64_t     ts) -> jobject {
        jstring jId  = env->NewStringUTF(txId);
        jstring jMsg = env->NewStringUTF(msg);
        jobject obj  = env->NewObject(
            resultClass, resultCtor,
            jId,
            static_cast<jboolean>(committed),
            static_cast<jint>(code),
            jMsg,
            static_cast<jlong>(ts));
        env->DeleteLocalRef(jId);
        env->DeleteLocalRef(jMsg);
        return obj;
    };

    // ── Validate the storage context ──────────────────────────────────────────
    auto* ctx = reinterpret_cast<NativeStorageContext*>(jStorageHandle);
    const char* txId = env->GetStringUTFChars(jTxId, nullptr);

    if (!ctx || !ctx->valid || !txId) {
        LOGE("commitToStorage: invalid context or txId");
        if (txId) env->ReleaseStringUTFChars(jTxId, txId);
        jobject r = makeResult("UNKNOWN", false,
                               COMMIT_ERR_INTERNAL, "Invalid storage context", nowMs());
        env->DeleteLocalRef(resultClass);
        return r;
    }

    // ── Get remaining string fields ───────────────────────────────────────────
    const char* currency   = env->GetStringUTFChars(jCurrency,   nullptr);
    const char* accountRef = env->GetStringUTFChars(jAccountRef, nullptr);

    if (!currency || !accountRef) {
        LOGE("commitToStorage: null string field for txId='%s'", txId);
        if (currency)   env->ReleaseStringUTFChars(jCurrency,   currency);
        if (accountRef) env->ReleaseStringUTFChars(jAccountRef, accountRef);
        env->ReleaseStringUTFChars(jTxId, txId);
        jobject r = makeResult(txId, false,
                               COMMIT_ERR_SERIALISE, "Null currency or accountRef", nowMs());
        env->DeleteLocalRef(resultClass);
        return r;
    }

    int64_t  commitTs   = nowMs();
    int      commitCode = COMMIT_OK;
    std::string errMsg;

    // ── BEGIN IMMEDIATE — per-row transaction ─────────────────────────────────
    if (execSQL(ctx->db, "BEGIN IMMEDIATE;") != SQLITE_OK) {
        commitCode = COMMIT_ERR_IO;
        errMsg     = "Failed to begin transaction for: ";
        errMsg    += txId;
        LOGE("commitToStorage: BEGIN IMMEDIATE failed for txId='%s'", txId);
        goto cleanup;
    }

    {
        // ── Bind all eight parameters (1-indexed) ─────────────────────────────
        sqlite3_bind_text (ctx->insertStmt, 1, txId,       -1, SQLITE_TRANSIENT);
        sqlite3_bind_int64(ctx->insertStmt, 2, static_cast<sqlite3_int64>(jTimestampMs));
        sqlite3_bind_int64(ctx->insertStmt, 3, static_cast<sqlite3_int64>(jAmountMicros));
        sqlite3_bind_text (ctx->insertStmt, 4, currency,   -1, SQLITE_TRANSIENT);
        sqlite3_bind_int  (ctx->insertStmt, 5, static_cast<int>(jType));
        sqlite3_bind_int  (ctx->insertStmt, 6, static_cast<int>(jStatus));
        sqlite3_bind_text (ctx->insertStmt, 7, accountRef, -1, SQLITE_TRANSIENT);
        sqlite3_bind_int64(ctx->insertStmt, 8, static_cast<sqlite3_int64>(commitTs));

        int rc = sqlite3_step(ctx->insertStmt);
        sqlite3_reset(ctx->insertStmt);
        sqlite3_clear_bindings(ctx->insertStmt);

        if (rc == SQLITE_DONE) {
            // ── COMMIT ────────────────────────────────────────────────────────
            if (execSQL(ctx->db, "COMMIT;") == SQLITE_OK) {
                LOGI("commitToStorage: COMMITTED txId='%s'", txId);
                commitCode = COMMIT_OK;
            } else {
                commitCode = COMMIT_ERR_IO;
                errMsg     = "COMMIT failed for: ";
                errMsg    += txId;
                execSQL(ctx->db, "ROLLBACK;");  // safety rollback
            }
        } else {
            // ── ROLLBACK — step failed ────────────────────────────────────────
            const char* sqliteErr = sqlite3_errmsg(ctx->db);
            LOGE("commitToStorage: step failed [%d] '%s' for txId='%s'",
                 rc, sqliteErr, txId);

            execSQL(ctx->db, "ROLLBACK;");

            if (rc == SQLITE_CONSTRAINT) {
                commitCode = COMMIT_ERR_CONSTRAINT;
                errMsg     = "Constraint violation: ";
            } else {
                commitCode = COMMIT_ERR_IO;
                errMsg     = "Step error: ";
            }
            errMsg += sqliteErr ? sqliteErr : "unknown";
        }
    }

cleanup:
    // ── Release JNI string references ─────────────────────────────────────────
    env->ReleaseStringUTFChars(jTxId,       txId);
    env->ReleaseStringUTFChars(jCurrency,   currency);
    env->ReleaseStringUTFChars(jAccountRef, accountRef);

    // ── Build and return the Java CommitResult ────────────────────────────────
    bool success = (commitCode == COMMIT_OK);
    jobject result = makeResult(
        txId,
        success,
        commitCode,
        success ? "" : errMsg.c_str(),
        commitTs);

    env->DeleteLocalRef(resultClass);
    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// NativeStorageBridge.nativeCloseStorage()
// ─────────────────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_example_dbridge_jni_NativeStorageBridge_nativeCloseStorage(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong jStorageHandle) {

    auto* ctx = reinterpret_cast<NativeStorageContext*>(jStorageHandle);
    if (!ctx) return;

    ctx->valid = false;
    sqlite3_finalize(ctx->insertStmt);
    sqlite3_close_v2(ctx->db);   // v2 is safe even if statements are not all finalised
    delete ctx;

    LOGI("nativeCloseStorage: storage closed and context freed");
}

} // extern "C"