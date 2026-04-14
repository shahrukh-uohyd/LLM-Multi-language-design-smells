#include <jni.h>
#include <stdio.h>

/* ==========================================================================
 * Module-level cache
 *
 * jfieldID is a raw pointer into the JVM's internal class descriptor table.
 * The JNI specification guarantees it is stable for the full lifetime of the
 * loaded class — the GC never moves or invalidates it.
 *
 * A single jfieldID is shared by BOTH readStockCount and writeStockCount.
 * This is the direct answer to the requirement's "call GetFieldID again"
 * anti-pattern: the field is resolved once and reused indefinitely.
 *
 * Contrast with jobject/jstring globals: those ARE moved by the GC and
 * require NewGlobalRef. jfieldID needs no such treatment.
 * ========================================================================== */
static jfieldID g_stockCountFieldID = NULL;  /* Product.stockCount : int */
static jint     g_lowStockThreshold = 10;    /* policy value from Java   */


/* --------------------------------------------------------------------------
 * throwJavaException
 *
 * Queues a Java exception to be thrown when control returns to the JVM.
 * CRITICAL: after calling this, return immediately. JNI exceptions are
 * "pending" — they do NOT unwind the C call stack automatically.
 * -------------------------------------------------------------------------- */
static void throwJavaException(JNIEnv*     env,
                               const char* exceptionClassName,
                               const char* message)
{
    jclass cls = (*env)->FindClass(env, exceptionClassName);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}


/* ==========================================================================
 * Java_InventoryService_initNativeCache
 *
 * Called ONCE on the main thread before any product is processed.
 * Resolves the jfieldID for Product.stockCount and stores the policy
 * threshold. Both readStockCount and writeStockCount reuse this one ID.
 *
 * WHY productClass is passed from Java, not resolved with FindClass here:
 * -------------------------------------------------------------------------
 * FindClass() uses the calling thread's class loader. On the main JVM
 * thread that is the application class loader which can see user-defined
 * classes. On any background or native thread it degrades to the bootstrap
 * class loader, which cannot see Product and silently returns NULL —
 * causing GetFieldID to crash with NullPointerException. Passing the
 * Class<?> object from Java bypasses this entirely.
 *
 * Matches Java signature:
 *   public static native void initNativeCache(Class<?> productClass,
 *                                             int lowStockThreshold);
 * ========================================================================== */
JNIEXPORT void JNICALL Java_InventoryService_initNativeCache(
        JNIEnv* env,
        jclass  callerClass,    /* InventoryService.class — unused */
        jclass  productClass,   /* Product.class passed from Java  */
        jint    lowStockThreshold)
{
    (void)callerClass;

    /* Idempotent: safe to call more than once. */
    if (g_stockCountFieldID != NULL) {
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 0. Parameter validation                                              */
    /* ------------------------------------------------------------------ */
    if (productClass == NULL) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "initNativeCache: productClass must not be null — "
            "pass Product.class explicitly from Java.");
        return;
    }

    if (lowStockThreshold < 0) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "initNativeCache: lowStockThreshold must be >= 0.");
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Resolve the jfieldID for Product.stockCount                      */
    /*                                                                      */
    /* "I" is the JNI type descriptor for Java primitive int.              */
    /*                                                                      */
    /* This GetFieldID call is the ONLY one in the entire module.          */
    /* It runs exactly once. readStockCount and writeStockCount both reuse  */
    /* this pointer — neither ever calls GetFieldID.                       */
    /* ------------------------------------------------------------------ */
    g_stockCountFieldID = (*env)->GetFieldID(env, productClass,
                                             "stockCount", "I");
    if (g_stockCountFieldID == NULL) {
        /*
         * GetFieldID already queued a NoSuchFieldError on the Java side.
         * Leave g_stockCountFieldID as NULL so callers detect uninit state.
         */
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 2. Store the policy threshold                                        */
    /*                                                                      */
    /* Keeping the threshold here (not as a C literal) means the Java side  */
    /* owns the policy. It can be read from a config file, database, or    */
    /* environment variable without touching or recompiling this file.     */
    /* ------------------------------------------------------------------ */
    g_lowStockThreshold = lowStockThreshold;

    fprintf(stdout,
            "[InventoryService] initNativeCache: Product.stockCount field ID "
            "cached. lowStockThreshold=%d\n",
            (int)lowStockThreshold);
}


/* ==========================================================================
 * Java_InventoryService_readStockCount
 *
 * Reads Product.stockCount using the cached field ID.
 * This function has exactly ONE responsibility: field I/O.
 *
 * No threshold comparison. No calculation. No branching on business rules.
 * Those concerns belong in Java where they are testable and maintainable.
 *
 * Matches Java signature:
 *   public static native int readStockCount(Object product);
 * ========================================================================== */
JNIEXPORT jint JNICALL Java_InventoryService_readStockCount(
        JNIEnv* env,
        jclass  callerClass,   /* InventoryService.class — unused */
        jobject productObj)    /* the Product instance to read    */
{
    (void)callerClass;

    /* ------------------------------------------------------------------ */
    /* 0. Guard: ensure initNativeCache was called                         */
    /* ------------------------------------------------------------------ */
    if (g_stockCountFieldID == NULL) {
        throwJavaException(env,
            "java/lang/IllegalStateException",
            "readStockCount: native cache not initialised. "
            "Call InventoryService.initNativeCache() first.");
        return -1;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Null guard                                                        */
    /* ------------------------------------------------------------------ */
    if (productObj == NULL) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "readStockCount: product must not be null.");
        return -1;
    }

    /* ------------------------------------------------------------------ */
    /* 2. Read and return                                                   */
    /*                                                                      */
    /* GetIntField is an O(1) pointer dereference. No scan. No lookup.     */
    /* g_stockCountFieldID is the cached result of the one-time resolve.   */
    /* ------------------------------------------------------------------ */
    return (*env)->GetIntField(env, productObj, g_stockCountFieldID); /* ✅ cached */
}


/* ==========================================================================
 * Java_InventoryService_writeStockCount
 *
 * Writes newStock into Product.stockCount using the SAME cached field ID
 * that readStockCount uses. This is the direct answer to the requirement's
 * "call GetFieldID again" anti-pattern — there is no second GetFieldID.
 *
 * This function has exactly ONE responsibility: field I/O.
 *
 * No threshold comparison. No calculation. No branching on business rules.
 *
 * Matches Java signature:
 *   public static native void writeStockCount(Object product, int newStock);
 * ========================================================================== */
JNIEXPORT void JNICALL Java_InventoryService_writeStockCount(
        JNIEnv* env,
        jclass  callerClass,   /* InventoryService.class — unused */
        jobject productObj,    /* the Product instance to update  */
        jint    newStock)
{
    (void)callerClass;

    /* ------------------------------------------------------------------ */
    /* 0. Guard: ensure initNativeCache was called                         */
    /* ------------------------------------------------------------------ */
    if (g_stockCountFieldID == NULL) {
        throwJavaException(env,
            "java/lang/IllegalStateException",
            "writeStockCount: native cache not initialised. "
            "Call InventoryService.initNativeCache() first.");
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Null guard                                                        */
    /* ------------------------------------------------------------------ */
    if (productObj == NULL) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "writeStockCount: product must not be null.");
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 2. Validate the incoming value                                       */
    /*                                                                      */
    /* A negative stock count is physically meaningless. Rejecting it here  */
    /* prevents a corrupt value from silently entering the Java object.    */
    /* ------------------------------------------------------------------ */
    if (newStock < 0) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "writeStockCount: newStock must be >= 0.");
        return;
    }

    /* ------------------------------------------------------------------ */
    /* 3. Write                                                             */
    /*                                                                      */
    /* SetIntField uses g_stockCountFieldID — the same pointer that        */
    /* readStockCount uses. There is no second GetFieldID call anywhere    */
    /* in this module. The single resolve in initNativeCache serves both   */
    /* the read path and the write path for the lifetime of the process.  */
    /* ------------------------------------------------------------------ */
    (*env)->SetIntField(env, productObj, g_stockCountFieldID, newStock); /* ✅ cached */
}