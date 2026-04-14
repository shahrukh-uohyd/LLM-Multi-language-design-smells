#include <jni.h>
#include <cstdio>
#include <cstring>

/* ==========================================================================
 * Module-level field ID cache
 *
 * jfieldID values are raw pointers into the JVM's internal class descriptor
 * table.  They are guaranteed stable for the lifetime of the loaded class —
 * they are never moved or invalidated by GC.  Caching them here is both safe
 * and explicitly recommended by the JNI specification.
 *
 * Contrast with jobject / jstring, which ARE moved by the GC and must never
 * be stored as plain globals; those require NewGlobalRef to pin them.
 * ========================================================================== */
static jfieldID g_userIdFieldID        = nullptr; /* User.userId        String */
static jfieldID g_statusFieldID        = nullptr; /* User.status        String */
static jfieldID g_loginAttemptsFieldID = nullptr; /* User.loginAttempts int    */
static jint     g_maxLoginAttempts     = 5;       /* configurable policy limit */

/* Validation result codes — must match the constants in UserBatchValidator.java */
static constexpr jint RESULT_OK                = 0;
static constexpr jint RESULT_INVALID_STATUS    = 1;
static constexpr jint RESULT_TOO_MANY_ATTEMPTS = 2;
static constexpr jint RESULT_NULL_USER         = 3;

/* Valid status values accepted by the validator */
static const char* VALID_STATUSES[] = { "ACTIVE", "INACTIVE", "SUSPENDED" };
static constexpr int VALID_STATUS_COUNT = 3;


/* --------------------------------------------------------------------------
 * throwJavaException
 *
 * Queues a Java exception to be thrown when control returns to the JVM.
 * CRITICAL: after calling this you MUST return immediately.  JNI exceptions
 * are "pending" — they do NOT unwind the C++ call stack automatically.
 * -------------------------------------------------------------------------- */
static void throwJavaException(JNIEnv*     env,
                               const char* exceptionClassName,
                               const char* message)
{
    jclass cls = env->FindClass(exceptionClassName);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
    /* If FindClass itself failed, a NoClassDefFoundError is already pending. */
}


/* --------------------------------------------------------------------------
 * isValidStatus (pure C++ helper — no JNI calls)
 *
 * Compares a C string against the table of accepted status values.
 * Kept as a separate helper so the JNI function stays focused on
 * JNI mechanics only.
 * -------------------------------------------------------------------------- */
static bool isValidStatus(const char* status)
{
    if (status == nullptr) return false;
    for (int i = 0; i < VALID_STATUS_COUNT; ++i) {
        if (strcmp(status, VALID_STATUSES[i]) == 0) return true;
    }
    return false;
}


/* ==========================================================================
 * Java_UserBatchValidator_initNativeCache
 *
 * Called ONCE from Java before any batch is processed.
 * Resolves all three jfieldIDs and stores them alongside the policy limit.
 *
 * Matches Java signature:
 *   public static native void initNativeCache(Class<?> userClass,
 *                                             int maxLoginAttempts);
 *
 * WHY the class is passed from Java and not resolved here with FindClass:
 *   FindClass uses the calling thread's class loader.  On native or
 *   background threads that loader is the bootstrap loader, which cannot
 *   see application classes and returns null silently.  Passing Class<?>
 *   from Java is the only fully portable solution.
 * ========================================================================== */
extern "C"
JNIEXPORT void JNICALL Java_UserBatchValidator_initNativeCache(
        JNIEnv* env,
        jclass  /*callerClass*/,   /* UserBatchValidator.class — unused */
        jclass  userClass,         /* User.class passed from Java       */
        jint    maxLoginAttempts)
{
    /* Idempotent: safe to call more than once (e.g. after hot-reload). */
    if (g_userIdFieldID != nullptr &&
        g_statusFieldID != nullptr &&
        g_loginAttemptsFieldID != nullptr)
    {
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 0. Validate parameters                                               */
    /* ------------------------------------------------------------------ */
    if (userClass == nullptr) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "initNativeCache: userClass must not be null");
        return;
    }

    if (maxLoginAttempts <= 0) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "initNativeCache: maxLoginAttempts must be > 0");
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Resolve User.userId  (type descriptor: Ljava/lang/String;)       */
    /* ------------------------------------------------------------------ */
    g_userIdFieldID = env->GetFieldID(userClass, "userId", "Ljava/lang/String;");
    if (g_userIdFieldID == nullptr) {
        /* GetFieldID already queued a NoSuchFieldError — just return.    */
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 2. Resolve User.status  (type descriptor: Ljava/lang/String;)       */
    /* ------------------------------------------------------------------ */
    g_statusFieldID = env->GetFieldID(userClass, "status", "Ljava/lang/String;");
    if (g_statusFieldID == nullptr) {
        g_userIdFieldID = nullptr; /* roll back partial init             */
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 3. Resolve User.loginAttempts  (type descriptor: I = int)           */
    /* ------------------------------------------------------------------ */
    g_loginAttemptsFieldID = env->GetFieldID(userClass, "loginAttempts", "I");
    if (g_loginAttemptsFieldID == nullptr) {
        g_userIdFieldID = nullptr; /* roll back partial init             */
        g_statusFieldID = nullptr;
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 4. Store the policy limit                                            */
    /* ------------------------------------------------------------------ */
    g_maxLoginAttempts = maxLoginAttempts;

    std::printf("[UserBatchValidator] initNativeCache: all field IDs cached. "
                "maxLoginAttempts=%d\n", static_cast<int>(maxLoginAttempts));
}


/* ==========================================================================
 * Java_UserBatchValidator_validateUser
 *
 * Validates a single User object by reading all three fields using ONLY
 * the cached field IDs — zero per-call GetFieldID lookups.
 *
 * Called from the Java for-loop in UserBatchValidator.processBatch().
 * The loop drives the iteration; this function sees one user at a time.
 *
 * Matches Java signature:
 *   public static native int validateUser(Object user);
 *
 * Returns one of the RESULT_* constants defined above.
 * ========================================================================== */
extern "C"
JNIEXPORT jint JNICALL Java_UserBatchValidator_validateUser(
        JNIEnv* env,
        jclass  /*callerClass*/,  /* UserBatchValidator.class — unused */
        jobject userObj)          /* the User instance to validate     */
{
    /* ------------------------------------------------------------------ */
    /* 0. Guard: ensure initNativeCache was called                         */
    /* ------------------------------------------------------------------ */
    if (g_userIdFieldID        == nullptr ||
        g_statusFieldID        == nullptr ||
        g_loginAttemptsFieldID == nullptr)
    {
        throwJavaException(env,
            "java/lang/IllegalStateException",
            "validateUser: native cache not initialised. "
            "Call UserBatchValidator.initNativeCache() before processing any batch.");
        return RESULT_NULL_USER;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Null-user guard                                                   */
    /*                                                                      */
    /* A null element is legal in the Java List — the batch builder may    */
    /* include one as a sentinel or error marker.  We handle it gracefully */
    /* rather than crashing the entire batch.                               */
    /* ------------------------------------------------------------------ */
    if (userObj == nullptr) {
        return RESULT_NULL_USER;
    }

    /* ================================================================== */
    /* FIELD READS — all three use cached IDs, zero redundant lookups      */
    /* ================================================================== */

    /* ------------------------------------------------------------------ */
    /* 2. Read User.userId                                                  */
    /*                                                                      */
    /* GetObjectField returns a local reference valid for this JNI call.   */
    /* We must DeleteLocalRef when done to avoid overflowing the 16-slot   */
    /* local reference frame (critical inside a tight batch loop).         */
    /* ------------------------------------------------------------------ */
    jstring userIdStr = static_cast<jstring>(
        env->GetObjectField(userObj, g_userIdFieldID));   /* ✅ cached ID */

    /* ------------------------------------------------------------------ */
    /* 3. Read User.status                                                  */
    /* ------------------------------------------------------------------ */
    jstring statusStr = static_cast<jstring>(
        env->GetObjectField(userObj, g_statusFieldID));   /* ✅ cached ID */

    /* ------------------------------------------------------------------ */
    /* 4. Read User.loginAttempts (primitive int — no local ref needed)    */
    /* ------------------------------------------------------------------ */
    jint loginAttempts =
        env->GetIntField(userObj, g_loginAttemptsFieldID); /* ✅ cached ID */

    /* ================================================================== */
    /* VALIDATION LOGIC                                                     */
    /* ================================================================== */

    /* ------------------------------------------------------------------ */
    /* 5. Validate status                                                   */
    /*                                                                      */
    /* GetStringUTFChars pins a UTF-8 view of the Java string (or copies   */
    /* it).  MUST be released with ReleaseStringUTFChars before returning. */
    /* ------------------------------------------------------------------ */
    jint result = RESULT_OK; /* optimistic default */

    if (statusStr == nullptr) {
        /* A null status is treated as invalid. */
        result = RESULT_INVALID_STATUS;
    } else {
        jboolean isCopy       = JNI_FALSE;
        const char* statusCStr = env->GetStringUTFChars(statusStr, &isCopy);

        if (statusCStr == nullptr) {
            /* GetStringUTFChars threw OutOfMemoryError — propagate. */
            env->DeleteLocalRef(userIdStr);
            env->DeleteLocalRef(statusStr);
            return RESULT_NULL_USER;
        }

        if (!isValidStatus(statusCStr)) {
            result = RESULT_INVALID_STATUS;
        }

        /* ✅ Release the UTF-8 buffer BEFORE any further JNI calls that   */
        /*    might trigger a GC safepoint (e.g., SetObjectField).         */
        env->ReleaseStringUTFChars(statusStr, statusCStr);
    }

    /* ------------------------------------------------------------------ */
    /* 6. Validate loginAttempts                                            */
    /*                                                                      */
    /* Only check if status was OK — avoids masking the status error with  */
    /* an attempt error for the same user.                                 */
    /* ------------------------------------------------------------------ */
    if (result == RESULT_OK && loginAttempts > g_maxLoginAttempts) {
        result = RESULT_TOO_MANY_ATTEMPTS;
    }

    /* ------------------------------------------------------------------ */
    /* 7. Log the validated user (reads userId for display purposes only)  */
    /* ------------------------------------------------------------------ */
    if (userIdStr != nullptr) {
        jboolean isCopy        = JNI_FALSE;
        const char* userIdCStr  = env->GetStringUTFChars(userIdStr, &isCopy);

        if (userIdCStr != nullptr) {
            std::printf("[validateUser] userId=%-12s loginAttempts=%2d  result=%d\n",
                        userIdCStr,
                        static_cast<int>(loginAttempts),
                        static_cast<int>(result));
            env->ReleaseStringUTFChars(userIdStr, userIdCStr);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 8. Release all local references                                      */
    /*                                                                      */
    /* The default JNI local frame holds 16 slots.  With 50 users per      */
    /* batch, failing to release here would overflow the frame on ~frame 5  */
    /* and crash the JVM with: "JNI ERROR: local reference table overflow"  */
    /* ------------------------------------------------------------------ */
    if (userIdStr != nullptr) env->DeleteLocalRef(userIdStr);
    if (statusStr != nullptr) env->DeleteLocalRef(statusStr);

    return result;
}