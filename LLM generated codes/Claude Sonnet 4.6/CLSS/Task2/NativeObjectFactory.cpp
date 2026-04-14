#include "NativeObjectFactory.h"

#include <jni.h>
#include <string>

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/**
 * Throws a Java exception of the given class from native code.
 * After calling this, return immediately — JNI will deliver the exception
 * to Java as soon as control returns from the native frame.
 *
 * @param env        JNI environment.
 * @param className  Fully-qualified Java exception class (e.g. "java/lang/RuntimeException").
 * @param message    The exception detail message.
 */
static void throwJavaException(JNIEnv* env,
                                const char* className,
                                const char* message)
{
    jclass exClass = env->FindClass(className);
    if (exClass != nullptr) {
        env->ThrowNew(exClass, message);
        env->DeleteLocalRef(exClass);
    }
    // If FindClass itself failed, a NoClassDefFoundError is already pending.
}

/**
 * Encapsulates the resolved JNI references needed to construct a DataContainer.
 * Resolved once per call to avoid redundant lookups.
 */
struct DataContainerMeta {
    jclass    clazz;       // Local ref to com.example.jni.DataContainer
    jmethodID constructor; // Method ID of DataContainer(float)
};

/**
 * Resolves the DataContainer class and its float constructor.
 *
 * @param env  JNI environment.
 * @param out  Output struct populated on success.
 * @return     true on success; false if resolution failed
 *             (a Java exception will already be pending).
 */
static bool resolveDataContainerMeta(JNIEnv* env, DataContainerMeta& out)
{
    // ------------------------------------------------------------------
    // Step 1 — Find the class
    //   Use the fully-qualified binary name with forward slashes,
    //   NOT dots (dots are used only in Java source, not in JNI).
    // ------------------------------------------------------------------
    out.clazz = env->FindClass("com/example/jni/DataContainer");
    if (out.clazz == nullptr) {
        // FindClass throws NoClassDefFoundError automatically.
        // We add a clearer RuntimeException on top for diagnostics.
        throwJavaException(env,
            "java/lang/RuntimeException",
            "JNI: Could not find class com.example.jni.DataContainer. "
            "Ensure the class is on the classpath and the package is correct."
        );
        return false;
    }

    // ------------------------------------------------------------------
    // Step 2 — Resolve the constructor
    //   Constructors are always named "<init>" in JNI.
    //   DataContainer(float) → descriptor "(F)V"
    //     F = float parameter
    //     V = void return type (constructors always return void in JNI)
    // ------------------------------------------------------------------
    out.constructor = env->GetMethodID(
        out.clazz,
        "<init>",   // Constructor name in JNI — always literal "<init>"
        "(F)V"      // Descriptor: takes one float, returns void
    );

    if (out.constructor == nullptr) {
        // GetMethodID throws NoSuchMethodError automatically.
        env->DeleteLocalRef(out.clazz);
        throwJavaException(env,
            "java/lang/RuntimeException",
            "JNI: Could not find constructor DataContainer(float). "
            "Verify the constructor signature matches descriptor (F)V."
        );
        return false;
    }

    return true;
}

// ---------------------------------------------------------------------------
// JNI Entry Point
// ---------------------------------------------------------------------------

JNIEXPORT jobject JNICALL
Java_com_example_jni_NativeObjectFactory_createDataContainer(
    JNIEnv* env,
    jobject /* thiz */,
    jfloat  value)
{
    // -----------------------------------------------------------------------
    // 1. Resolve the DataContainer class and constructor
    // -----------------------------------------------------------------------
    DataContainerMeta meta{};
    if (!resolveDataContainerMeta(env, meta)) {
        return nullptr; // Exception already pending
    }

    // -----------------------------------------------------------------------
    // 2. Instantiate the Java object from C++
    //    NewObject(class, constructorMethodID, args...)
    //    This is equivalent to:  new DataContainer(value)  in Java.
    // -----------------------------------------------------------------------
    jobject instance = env->NewObject(meta.clazz, meta.constructor, value);

    // -----------------------------------------------------------------------
    // 3. Release the local class reference — we no longer need it.
    //    The returned 'instance' is itself a local ref managed by the JVM.
    // -----------------------------------------------------------------------
    env->DeleteLocalRef(meta.clazz);

    // -----------------------------------------------------------------------
    // 4. Verify that NewObject succeeded.
    //    It returns nullptr if an exception was thrown (e.g. OutOfMemoryError,
    //    or an exception thrown inside the DataContainer constructor itself).
    // -----------------------------------------------------------------------
    if (instance == nullptr || env->ExceptionCheck()) {
        if (!env->ExceptionCheck()) {
            // NewObject returned null but no exception — throw one explicitly
            throwJavaException(env,
                "java/lang/RuntimeException",
                "JNI: NewObject returned null for DataContainer — "
                "possible OutOfMemoryError."
            );
        }
        return nullptr;
    }

    // -----------------------------------------------------------------------
    // 5. Return the newly created Java object to the caller.
    //    The JVM automatically promotes the local ref to the calling frame.
    // -----------------------------------------------------------------------
    return instance;
}