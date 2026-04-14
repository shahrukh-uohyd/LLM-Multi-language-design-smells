#include "NativeModuleBridge.h"

#include <jni.h>
#include <string>

// ===========================================================================
// Internal Utilities
// ===========================================================================

/**
 * Throws a named Java exception from native code.
 * Always return immediately after calling this.
 */
static void throwJavaException(JNIEnv*     env,
                                const char* className,
                                const char* message)
{
    jclass exClass = env->FindClass(className);
    if (exClass != nullptr) {
        env->ThrowNew(exClass, message);
        env->DeleteLocalRef(exClass);
    }
}

/**
 * Safely converts a jstring to std::string.
 * Pins the JVM string with GetStringUTFChars and immediately releases it.
 */
static std::string jstringToStd(JNIEnv* env, jstring jStr)
{
    if (jStr == nullptr) return {};

    const char* chars = env->GetStringUTFChars(jStr, nullptr);
    if (chars == nullptr) return {}; // OutOfMemoryError pending

    std::string result(chars);
    env->ReleaseStringUTFChars(jStr, chars);
    return result;
}

/**
 * Converts a std::string to a new jstring.
 * Caller is responsible for DeleteLocalRef on the returned jstring.
 */
static jstring stdToJstring(JNIEnv* env, const std::string& str)
{
    return env->NewStringUTF(str.c_str());
}

// ===========================================================================
// Resolved JNI Metadata — ComponentRegistry
// ===========================================================================

/**
 * Holds resolved JNI references for ComponentRegistry.getComponent().
 *
 * Class   : com.example.app.ComponentRegistry
 * Method  : static NativeControllable getComponent(String componentId)
 * Descriptor: (Ljava/lang/String;)Lcom/example/app/NativeControllable;
 */
struct RegistryMeta {
    jclass    registryClass;   // Local ref to ComponentRegistry
    jmethodID getComponent;    // Static: getComponent(String) → NativeControllable
};

static bool resolveRegistryMeta(JNIEnv* env, RegistryMeta& out)
{
    out.registryClass = env->FindClass("com/example/app/ComponentRegistry");
    if (out.registryClass == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Class not found — com.example.app.ComponentRegistry. "
            "Verify the package path and classpath configuration.");
        return false;
    }

    // getComponent returns a NativeControllable (interface type)
    // Descriptor: takes String, returns NativeControllable
    out.getComponent = env->GetStaticMethodID(
        out.registryClass,
        "getComponent",
        "(Ljava/lang/String;)Lcom/example/app/NativeControllable;"
    );

    if (out.getComponent == nullptr) {
        env->DeleteLocalRef(out.registryClass);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Static method not found — "
            "ComponentRegistry.getComponent(String). "
            "Descriptor: (Ljava/lang/String;)Lcom/example/app/NativeControllable;");
        return false;
    }

    return true;
}

// ===========================================================================
// Resolved JNI Metadata — NativeControllable.trigger()
// ===========================================================================

/**
 * Holds resolved JNI references for NativeControllable.trigger().
 *
 * Interface : com.example.app.NativeControllable
 * Method    : String trigger(String params, int flags)
 * Descriptor: (Ljava/lang/String;I)Ljava/lang/String;
 */
struct ControllableMeta {
    jclass    controllableClass; // Local ref to NativeControllable interface
    jmethodID trigger;           // trigger(String, int) → String
};

static bool resolveControllableMeta(JNIEnv* env, ControllableMeta& out)
{
    // JNI resolves interface methods the same way as class methods.
    // GetMethodID works on the interface type directly.
    out.controllableClass = env->FindClass("com/example/app/NativeControllable");
    if (out.controllableClass == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Interface not found — com.example.app.NativeControllable.");
        return false;
    }

    // String trigger(String params, int flags)
    // Descriptor: (Ljava/lang/String;I)Ljava/lang/String;
    //   param 1: Ljava/lang/String;   → String
    //   param 2: I                    → int
    //   return : Ljava/lang/String;   → String
    out.trigger = env->GetMethodID(
        out.controllableClass,
        "trigger",
        "(Ljava/lang/String;I)Ljava/lang/String;"
    );

    if (out.trigger == nullptr) {
        env->DeleteLocalRef(out.controllableClass);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Method not found — NativeControllable.trigger(String, int). "
            "Descriptor: (Ljava/lang/String;I)Ljava/lang/String;");
        return false;
    }

    return true;
}

// ===========================================================================
// Resolved JNI Metadata — AppLifecycleManager.onLifecycleEvent()
// ===========================================================================

/**
 * Holds resolved JNI references for AppLifecycleManager.onLifecycleEvent().
 *
 * Class      : com.example.app.AppLifecycleManager
 * Method     : static void onLifecycleEvent(String event, String reason)
 * Descriptor : (Ljava/lang/String;Ljava/lang/String;)V
 */
struct LifecycleMeta {
    jclass    lifecycleClass;     // Local ref to AppLifecycleManager
    jmethodID onLifecycleEvent;   // Static: onLifecycleEvent(String, String) → void
};

static bool resolveLifecycleMeta(JNIEnv* env, LifecycleMeta& out)
{
    out.lifecycleClass = env->FindClass("com/example/app/AppLifecycleManager");
    if (out.lifecycleClass == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Class not found — com.example.app.AppLifecycleManager.");
        return false;
    }

    // static void onLifecycleEvent(String event, String reason)
    // Descriptor: (Ljava/lang/String;Ljava/lang/String;)V
    out.onLifecycleEvent = env->GetStaticMethodID(
        out.lifecycleClass,
        "onLifecycleEvent",
        "(Ljava/lang/String;Ljava/lang/String;)V"
    );

    if (out.onLifecycleEvent == nullptr) {
        env->DeleteLocalRef(out.lifecycleClass);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Static method not found — "
            "AppLifecycleManager.onLifecycleEvent(String, String). "
            "Descriptor: (Ljava/lang/String;Ljava/lang/String;)V");
        return false;
    }

    return true;
}

// ===========================================================================
// JNI Entry Point 1 — triggerComponent
// ===========================================================================

JNIEXPORT jstring JNICALL
Java_com_example_app_NativeModuleBridge_triggerComponent(
    JNIEnv* env,
    jobject /* thiz */,
    jstring componentId,
    jstring params,
    jint    flags)
{
    // -----------------------------------------------------------------------
    // 1. Validate inputs
    // -----------------------------------------------------------------------
    if (componentId == nullptr) {
        throwJavaException(env, "java/lang/IllegalArgumentException",
            "JNI: 'componentId' must not be null.");
        return nullptr;
    }

    // -----------------------------------------------------------------------
    // 2. Resolve ComponentRegistry metadata
    // -----------------------------------------------------------------------
    RegistryMeta regMeta{};
    if (!resolveRegistryMeta(env, regMeta)) {
        return nullptr;
    }

    // -----------------------------------------------------------------------
    // 3. Call ComponentRegistry.getComponent(componentId)
    //    Returns a NativeControllable object (or null if not found).
    //
    //    CallStaticObjectMethod → used for static methods returning an object.
    //    Equivalent Java: NativeControllable comp =
    //                         ComponentRegistry.getComponent(componentId);
    // -----------------------------------------------------------------------
    jobject component = env->CallStaticObjectMethod(
        regMeta.registryClass,
        regMeta.getComponent,
        componentId              // jstring passed directly as argument
    );

    env->DeleteLocalRef(regMeta.registryClass);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Exception thrown inside ComponentRegistry.getComponent().");
        return nullptr;
    }

    if (component == nullptr) {
        // Component not registered — build a descriptive error
        std::string compIdStr = jstringToStd(env, componentId);
        std::string msg = "JNI: No component registered with ID '"
                        + compIdStr + "' in ComponentRegistry.";
        throwJavaException(env, "java/lang/IllegalArgumentException", msg.c_str());
        return nullptr;
    }

    // -----------------------------------------------------------------------
    // 4. Resolve NativeControllable.trigger() on the interface
    // -----------------------------------------------------------------------
    ControllableMeta ctrlMeta{};
    if (!resolveControllableMeta(env, ctrlMeta)) {
        env->DeleteLocalRef(component);
        return nullptr;
    }

    // -----------------------------------------------------------------------
    // 5. Invoke component.trigger(params, flags)
    //
    //    CallObjectMethod → used for instance methods returning an object.
    //    The actual runtime type of 'component' (UIController, DataProcessor,
    //    etc.) is resolved by the JVM through its vtable — JNI does NOT need
    //    to know the concrete class, only the interface method ID.
    //
    //    Equivalent Java: String result = component.trigger(params, flags);
    // -----------------------------------------------------------------------
    jobject resultObj = env->CallObjectMethod(
        component,
        ctrlMeta.trigger,
        params,    // jstring — passed directly
        flags      // jint
    );

    env->DeleteLocalRef(component);
    env->DeleteLocalRef(ctrlMeta.controllableClass);

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Exception thrown inside component.trigger().");
        return nullptr;
    }

    // -----------------------------------------------------------------------
    // 6. Return the String result back to Java
    //    CallObjectMethod returns a jobject; cast to jstring since we know
    //    the Java method declares String as its return type.
    // -----------------------------------------------------------------------
    return static_cast<jstring>(resultObj);
}

// ===========================================================================
// JNI Entry Point 2 — signalLifecycleEvent
// ===========================================================================

JNIEXPORT void JNICALL
Java_com_example_app_NativeModuleBridge_signalLifecycleEvent(
    JNIEnv* env,
    jobject /* thiz */,
    jstring event,
    jstring reason)
{
    // -----------------------------------------------------------------------
    // 1. Validate inputs
    // -----------------------------------------------------------------------
    if (event == nullptr) {
        throwJavaException(env, "java/lang/IllegalArgumentException",
            "JNI: 'event' must not be null.");
        return;
    }

    // -----------------------------------------------------------------------
    // 2. Resolve AppLifecycleManager metadata
    // -----------------------------------------------------------------------
    LifecycleMeta meta{};
    if (!resolveLifecycleMeta(env, meta)) {
        return;
    }

    // -----------------------------------------------------------------------
    // 3. Invoke AppLifecycleManager.onLifecycleEvent(event, reason)
    //
    //    CallStaticVoidMethod → static method with void return type.
    //    Equivalent Java: AppLifecycleManager.onLifecycleEvent(event, reason);
    // -----------------------------------------------------------------------
    env->CallStaticVoidMethod(
        meta.lifecycleClass,
        meta.onLifecycleEvent,
        event,    // jstring
        reason    // jstring
    );

    env->DeleteLocalRef(meta.lifecycleClass);

    // -----------------------------------------------------------------------
    // 4. Check for exceptions thrown inside the lifecycle handler
    // -----------------------------------------------------------------------
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Exception thrown inside "
            "AppLifecycleManager.onLifecycleEvent().");
    }
}