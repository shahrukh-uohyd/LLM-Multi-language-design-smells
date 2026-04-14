#include <jni.h>
#include <chrono>
#include <cstdio>
#include <atomic>

/* ==========================================================================
 * Module-level field ID cache
 *
 * jfieldID is a raw pointer into the JVM's internal class descriptor table.
 * The JNI specification guarantees it is stable for the full lifetime of the
 * loaded class — it is never moved or invalidated by the GC.
 *
 * std::atomic is used for the pointer itself so that a visibility guarantee
 * exists if initNativeCache() and the first recordTimestamp() call ever race
 * on separate threads (e.g. in a test harness). Under normal use
 * initNativeCache() is called on the main thread before any background thread
 * is started, so the atomic is belt-and-suspenders safety, not a mutex.
 *
 * Contrast with jobject / jstring globals: those ARE moved by the GC and
 * require NewGlobalRef to pin them. jfieldID needs no such treatment.
 * ========================================================================== */
static std::atomic<jfieldID> g_timestampFieldID { nullptr };


/* --------------------------------------------------------------------------
 * throwJavaException
 *
 * Queues a Java exception to be thrown when control returns to the JVM.
 *
 * CRITICAL: after calling this you MUST return immediately.
 * JNI exceptions are "pending" — they do NOT unwind the C++ call stack.
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
    /*
     * If FindClass itself failed, a NoClassDefFoundError is already pending.
     * Nothing more to do — just return after this call in the caller.
     */
}


/* ==========================================================================
 * Java_NetworkMonitor_initNativeCache
 *
 * Called ONCE on the main thread before any NetworkPacket is constructed
 * and before the background listener thread is started.
 *
 * Resolves the jfieldID for NetworkPacket.timestamp and stores it in the
 * module-level atomic. Both call sites (constructor + refresh) will reuse
 * this single cached value with zero per-call lookup cost.
 *
 * WHY packetClass is passed from Java instead of using FindClass() here:
 * -----------------------------------------------------------------------
 * FindClass() uses the calling thread's class loader. On the main JVM
 * thread that is the application class loader, which can see user-defined
 * classes. But on a native background thread the calling loader is the
 * bootstrap class loader, which only knows core JDK classes. Calling
 * FindClass("NetworkPacket") on a background thread silently returns null,
 * and the subsequent GetFieldID call crashes with NullPointerException.
 *
 * Passing the Class<?> object from Java entirely bypasses this hazard:
 * the JVM has already resolved the class correctly before the call arrives
 * in C++, regardless of which thread it originates from.
 *
 * Matches Java signature:
 *   public static native void initNativeCache(Class<?> packetClass);
 * ========================================================================== */
extern "C"
JNIEXPORT void JNICALL Java_NetworkMonitor_initNativeCache(
        JNIEnv* env,
        jclass  /* callerClass */,   /* NetworkMonitor.class — unused */
        jclass  packetClass)         /* NetworkPacket.class from Java */
{
    /* Idempotent: if already initialised, nothing to do. */
    if (g_timestampFieldID.load(std::memory_order_acquire) != nullptr) {
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 0. Parameter validation                                              */
    /* ------------------------------------------------------------------ */
    if (packetClass == nullptr) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "initNativeCache: packetClass must not be null — "
            "pass NetworkPacket.class explicitly from Java.");
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Resolve the jfieldID for NetworkPacket.timestamp                  */
    /*                                                                      */
    /* Type descriptor "J" = Java primitive long.                          */
    /*                                                                      */
    /* GetFieldID scans the class descriptor's field table exactly once    */
    /* here. After this point the returned ID is used directly for every   */
    /* subsequent read or write — no further scanning ever occurs.         */
    /* ------------------------------------------------------------------ */
    jfieldID fid = env->GetFieldID(packetClass, "timestamp", "J");

    if (fid == nullptr) {
        /*
         * GetFieldID already queued a NoSuchFieldError on the Java side.
         * Leave g_timestampFieldID as nullptr so callers can detect the
         * uninitialised state and throw a clear IllegalStateException
         * rather than segfaulting on a null pointer dereference.
         */
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 2. Publish the resolved ID with release semantics                   */
    /*                                                                      */
    /* memory_order_release pairs with the acquire load in recordTimestamp  */
    /* and the acquire load in the idempotency check above, ensuring every  */
    /* thread that observes a non-null value also observes the fully        */
    /* initialised jfieldID value written by this store.                   */
    /* ------------------------------------------------------------------ */
    g_timestampFieldID.store(fid, std::memory_order_release);

    std::printf("[NetworkMonitor] initNativeCache: "
                "NetworkPacket.timestamp field ID cached successfully.\n");
}


/* ==========================================================================
 * Java_NetworkMonitor_recordTimestamp
 *
 * Writes the current wall-clock time (milliseconds since the Unix epoch)
 * into the timestamp field of the supplied NetworkPacket object.
 *
 * Called from TWO call sites:
 *   1. NetworkPacket constructor  — main or any constructing thread
 *   2. NetworkPacket.refresh()    — background listener thread
 *
 * Both call sites reuse the single cached g_timestampFieldID.
 * There is NO GetFieldID call inside this function — ever.
 *
 * Matches Java signature:
 *   public static native void recordTimestamp(Object packet);
 * ========================================================================== */
extern "C"
JNIEXPORT void JNICALL Java_NetworkMonitor_recordTimestamp(
        JNIEnv* env,
        jclass  /* callerClass */,  /* NetworkMonitor.class — unused */
        jobject packetObj)          /* the NetworkPacket instance     */
{
    /* ------------------------------------------------------------------ */
    /* 0. Guard: ensure initNativeCache() was called                       */
    /*                                                                      */
    /* Acquire load pairs with the release store in initNativeCache(),     */
    /* guaranteeing this thread sees the fully written jfieldID value.     */
    /* ------------------------------------------------------------------ */
    jfieldID fid = g_timestampFieldID.load(std::memory_order_acquire);

    if (fid == nullptr) {
        throwJavaException(env,
            "java/lang/IllegalStateException",
            "recordTimestamp: native cache not initialised. "
            "Call NetworkMonitor.initNativeCache(NetworkPacket.class) "
            "on the main thread before constructing packets or starting "
            "the background listener.");
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Null-packet guard                                                 */
    /*                                                                      */
    /* Defensive check: the Java call sites are controlled, but a null     */
    /* could arrive from a test harness or serialisation edge case.        */
    /* ------------------------------------------------------------------ */
    if (packetObj == nullptr) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "recordTimestamp: packet must not be null.");
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 2. Capture current wall-clock time                                  */
    /*                                                                      */
    /* std::chrono::system_clock measures real-world (wall-clock) time.   */
    /* time_since_epoch() gives the duration since the Unix epoch          */
    /* (1970-01-01 00:00:00 UTC), matching Java's System.currentTimeMillis */
    /* semantics exactly. Cast to milliseconds to match Java's long field. */
    /* ------------------------------------------------------------------ */
    auto now = std::chrono::system_clock::now();
    jlong timestampMs = static_cast<jlong>(
        std::chrono::duration_cast<std::chrono::milliseconds>(
            now.time_since_epoch()
        ).count()
    );

    /* ------------------------------------------------------------------ */
    /* 3. Write the timestamp into the Java object                         */
    /*                                                                      */
    /* SetLongField uses the cached fid directly — this is an O(1)        */
    /* pointer dereference into the object's field storage. There is no    */
    /* class scan, no hash lookup, and no string comparison.               */
    /*                                                                      */
    /* ✅ The same fid is used here whether this call originates from      */
    /*    the constructor (call site 1) or refresh() (call site 2).        */
    /*    The background thread safety concern is fully handled by the      */
    /*    acquire/release pair on g_timestampFieldID — not by a mutex,     */
    /*    because jfieldID itself requires no synchronisation once cached. */
    /* ------------------------------------------------------------------ */
    env->SetLongField(packetObj, fid, timestampMs);
}