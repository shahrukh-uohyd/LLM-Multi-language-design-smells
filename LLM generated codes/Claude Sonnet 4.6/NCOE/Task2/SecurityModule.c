#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

/* =========================================================================
 * Module-level cache
 *
 * jfieldID is a raw pointer into the JVM's internal class descriptor table.
 * It is stable for the full lifetime of the loaded class — the JVM guarantees
 * it never changes or moves, so caching it here is both safe and correct.
 *
 * Contrast with jobject / jstring, which ARE moved by the GC and must never
 * be stored as plain globals without calling NewGlobalRef first.
 * ========================================================================= */
static jfieldID g_bioFieldID    = NULL;  /* cached field ID for UserProfile.bio */
static jint     g_maxBioLength  = 100;   /* configurable policy value            */


/* -------------------------------------------------------------------------
 * throwJavaException
 *
 * Queues a Java exception of the given class to be thrown when control
 * returns to the JVM.  After calling this you MUST return immediately —
 * a pending JNI exception does NOT unwind the C call stack.
 * ------------------------------------------------------------------------- */
static void throwJavaException(JNIEnv* env,
                               const char* exceptionClassName,
                               const char* message)
{
    jclass cls = (*env)->FindClass(env, exceptionClassName);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
    /* If FindClass itself failed, a NoClassDefFoundError is already pending. */
}


/* =========================================================================
 * Java_SecurityModule_initNativeCache
 *
 * Called ONCE from Java before the first sanitizeUserProfile() invocation.
 * Resolves the jfieldID for UserProfile.bio and stores both it and the
 * configured policy limit in module-level statics.
 *
 * Matches:
 *   public static native void initNativeCache(Class<?> userProfileClass,
 *                                             int maxBioLength);
 * ========================================================================= */
JNIEXPORT void JNICALL Java_SecurityModule_initNativeCache(
        JNIEnv* env,
        jclass  callerClass,       /* SecurityModule.class — unused */
        jclass  userProfileClass,  /* UserProfile.class passed from Java */
        jint    maxBioLength)
{
    (void)callerClass;

    if (g_bioFieldID != NULL) {
        /* Already initialised — idempotent re-entry is safe. */
        return;
    }

    if (userProfileClass == NULL) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "initNativeCache: userProfileClass must not be null");
        return;
    }

    if (maxBioLength <= 0) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "initNativeCache: maxBioLength must be > 0");
        return;
    }

    /*
     * "Ljava/lang/String;" is the JNI type descriptor for java.lang.String.
     *
     * We pass userProfileClass directly from Java instead of calling
     * FindClass("UserProfile") here in C.  Why?  FindClass uses the calling
     * thread's class loader.  On native/background threads that classloader
     * is the bootstrap loader, which cannot see application classes, causing
     * a silent NULL return.  Passing the Class object from Java is the only
     * fully portable solution.
     */
    g_bioFieldID = (*env)->GetFieldID(env, userProfileClass, "bio", "Ljava/lang/String;");

    if (g_bioFieldID == NULL) {
        /*
         * GetFieldID already queued a NoSuchFieldError on the Java side.
         * Leave g_bioFieldID as NULL so callers can detect uninitialised state.
         */
        return;
    }

    g_maxBioLength = maxBioLength;

    fprintf(stdout,
            "[SecurityModule] initNativeCache: bio field ID cached, maxBioLength=%d.\n",
            (int)maxBioLength);
}


/* =========================================================================
 * Java_SecurityModule_sanitizeUserProfile
 *
 * Reads UserProfile.bio ONCE using the cached field ID.
 * If the bio string exceeds g_maxBioLength it is truncated and written back
 * using that SAME cached field ID — no second lookup, no redundancy.
 *
 * Matches:
 *   public static native boolean sanitizeUserProfile(Object profile);
 *
 * Returns JNI_TRUE if the bio was truncated, JNI_FALSE otherwise.
 * ========================================================================= */
JNIEXPORT jboolean JNICALL Java_SecurityModule_sanitizeUserProfile(
        JNIEnv* env,
        jclass  callerClass,   /* SecurityModule.class — unused */
        jobject profileObj)    /* the UserProfile instance       */
{
    (void)callerClass;

    /* ------------------------------------------------------------------ */
    /* 0. Pre-condition checks                                              */
    /* ------------------------------------------------------------------ */
    if (g_bioFieldID == NULL) {
        throwJavaException(env,
            "java/lang/IllegalStateException",
            "sanitizeUserProfile: native cache not initialised. "
            "Call SecurityModule.initNativeCache() first.");
        return JNI_FALSE;
    }

    if (profileObj == NULL) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "sanitizeUserProfile: profile must not be null");
        return JNI_FALSE;
    }

    /* ------------------------------------------------------------------ */
    /* 1. Read the bio field — ONE lookup using the cached field ID        */
    /* ------------------------------------------------------------------ */
    jstring bioString = (jstring)(*env)->GetObjectField(env, profileObj, g_bioFieldID);

    /*
     * A null bio is valid (the user may not have set one).
     * Nothing to sanitize — return clean.
     */
    if (bioString == NULL) {
        return JNI_FALSE;
    }

    /* ------------------------------------------------------------------ */
    /* 2. Measure the bio length                                           */
    /*                                                                      */
    /* GetStringLength returns the number of UTF-16 code units, matching   */
    /* Java's String.length().  For a security check this is the correct   */
    /* measure because Java itself counts characters this way.             */
    /* ------------------------------------------------------------------ */
    jsize bioLength = (*env)->GetStringLength(env, bioString);

    if (bioLength <= g_maxBioLength) {
        /* Bio is within the allowed limit — release the local ref and exit. */
        (*env)->DeleteLocalRef(env, bioString);
        return JNI_FALSE;
    }

    /* ------------------------------------------------------------------ */
    /* 3. Truncate: bio exceeds limit                                       */
    /*                                                                      */
    /* GetStringChars gives us a const pointer to the UTF-16 backing store  */
    /* (or a copy — the JVM decides).  We MUST release it with             */
    /* ReleaseStringChars when done to avoid a memory/pin leak.            */
    /* ------------------------------------------------------------------ */
    jboolean isCopy   = JNI_FALSE;
    const jchar* chars = (*env)->GetStringChars(env, bioString, &isCopy);

    if (chars == NULL) {
        /* Out of memory — JVM already threw OutOfMemoryError. */
        (*env)->DeleteLocalRef(env, bioString);
        return JNI_FALSE;
    }

    /*
     * NewString creates a new Java String from the first g_maxBioLength
     * UTF-16 code units.  This is the truncated replacement value.
     */
    jstring truncatedBio = (*env)->NewString(env, chars, (jsize)g_maxBioLength);

    /* Release the character array/pin before any further JNI calls. */
    (*env)->ReleaseStringChars(env, bioString, chars);
    (*env)->DeleteLocalRef(env, bioString);

    if (truncatedBio == NULL) {
        /* NewString threw OutOfMemoryError — propagate it. */
        return JNI_FALSE;
    }

    /* ------------------------------------------------------------------ */
    /* 4. Write the truncated bio back using the SAME cached field ID      */
    /*                                                                      */
    /* ✅ This is the key correctness point: g_bioFieldID is reused here.  */
    /*    There is no second GetFieldID call.  The single resolved pointer  */
    /*    is used for both the read in step 1 and this write in step 4.    */
    /* ------------------------------------------------------------------ */
    (*env)->SetObjectField(env, profileObj, g_bioFieldID, truncatedBio);

    (*env)->DeleteLocalRef(env, truncatedBio);

    return JNI_TRUE;
}