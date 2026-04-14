#include <jni.h>
#include <stdexcept>
#include <cstdio>

// ---------------------------------------------------------------------------
// Cached field ID — stored at global scope so it survives across JNI calls.
// jfieldID values are stable for the lifetime of the JVM; they never change
// once resolved, so caching them is both safe and recommended by the JNI spec.
// ---------------------------------------------------------------------------
static jfieldID g_velocityFieldID = nullptr;

// ---------------------------------------------------------------------------
// Helper: throws a Java RuntimeException from C++ code.
// After calling this, you MUST return immediately — JNI exceptions are
// "pending" and do not unwind the C++ stack automatically.
// ---------------------------------------------------------------------------
static void throwJavaRuntimeException(JNIEnv* env, const char* message) {
    jclass runtimeExceptionClass = env->FindClass("java/lang/RuntimeException");
    if (runtimeExceptionClass != nullptr) {
        env->ThrowNew(runtimeExceptionClass, message);
        env->DeleteLocalRef(runtimeExceptionClass);
    }
}

// ---------------------------------------------------------------------------
// Java_PhysicsEngine_initNativeCache
//
// Called ONCE from Java before the game loop to resolve and cache the
// jfieldID for Entity.velocity.
//
// Signature matches:
//   public static native void initNativeCache(Class<?> entityClass);
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT void JNICALL Java_PhysicsEngine_initNativeCache(
        JNIEnv* env,
        jclass  /*callerClass*/,   // PhysicsEngine.class (static method)
        jclass  entityClass)       // Entity.class passed from Java
{
    if (g_velocityFieldID != nullptr) {
        // Already initialised — nothing to do.
        return;
    }

    if (entityClass == nullptr) {
        throwJavaRuntimeException(env, "initNativeCache: entityClass must not be null");
        return;
    }

    // Resolve the field ID for the 'float velocity' field on Entity.
    // "F" is the JNI type descriptor for a Java float.
    g_velocityFieldID = env->GetFieldID(entityClass, "velocity", "F");

    if (g_velocityFieldID == nullptr) {
        // GetFieldID already threw a NoSuchFieldError; just return.
        // We leave g_velocityFieldID as nullptr so callers can detect failure.
        return;
    }

    std::printf("[JNI] initNativeCache: Entity.velocity field ID cached successfully.\n");
}

// ---------------------------------------------------------------------------
// Java_PhysicsEngine_updateVelocityNative
//
// Called 60 times per second from the Java game loop.
// Uses the cached g_velocityFieldID — NO per-frame field lookup.
//
// Signature matches:
//   public static native void updateVelocityNative(Object entityObj, float newVelocity);
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT void JNICALL Java_PhysicsEngine_updateVelocityNative(
        JNIEnv* env,
        jclass  /*callerClass*/,  // PhysicsEngine.class (static method)
        jobject entityObj,        // the Entity instance
        jfloat  newVelocity)      // the new velocity value
{
    // Guard: make sure the one-time init was called first.
    if (g_velocityFieldID == nullptr) {
        throwJavaRuntimeException(
            env,
            "updateVelocityNative: field ID not cached. "
            "Did you forget to call PhysicsEngine.initNativeCache(Entity.class)?");
        return;
    }

    if (entityObj == nullptr) {
        throwJavaRuntimeException(env, "updateVelocityNative: entityObj must not be null");
        return;
    }

    // ✅ Set the float field directly — O(1), no string lookup, no class scan.
    env->SetFloatField(entityObj, g_velocityFieldID, newVelocity);
}