#pragma once
#include <cstdint>

// ── Commit-status codes — must mirror CommitResult Java constants ─────────────
#define COMMIT_OK             0
#define COMMIT_ERR_DUPLICATE  1
#define COMMIT_ERR_CONSTRAINT 2
#define COMMIT_ERR_IO         3
#define COMMIT_ERR_SERIALISE  4
#define COMMIT_ERR_INTERNAL   5

/**
 * Opaque native storage context.
 * Wraps a SQLite db handle plus a pre-compiled INSERT statement
 * that is reused across all 25 individual commitToStorage() calls.
 */
struct NativeStorageContext {
    sqlite3*      db;           // SQLite connection
    sqlite3_stmt* insertStmt;   // pre-compiled INSERT OR REPLACE
    bool          valid;        // false once closed
};