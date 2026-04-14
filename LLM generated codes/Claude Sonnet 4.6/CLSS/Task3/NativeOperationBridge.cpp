#include "NativeOperationBridge.h"

#include <jni.h>
#include <string>
#include <stdexcept>

// ===========================================================================
// Internal Utilities
// ===========================================================================

/**
 * Throws a named Java exception from native code.
 * ALWAYS return immediately after calling this.
 *
 * @param env        JNI environment.
 * @param className  Fully-qualified class in JNI slash-notation.
 * @param message    Exception detail message.
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
    // If FindClass itself failed, NoClassDefFoundError is already pending.
}

/**
 * Safely converts a jstring to a std::string, respecting nullptr input.
 *
 * GetStringUTFChars pins the string in JVM memory — ReleaseStringUTFChars
 * MUST always be called to unpin it, even if the chars are not used.
 *
 * @param env  JNI environment.
 * @param jStr The jstring to convert. May be nullptr.
 * @return     Equivalent std::string, or empty string if jStr is nullptr.
 */
static std::string jstringToStdString(JNIEnv* env, jstring jStr)
{
    if (jStr == nullptr) {
        return {};
    }

    // isCopy (second param) set to nullptr — we don't need to know
    // whether the JVM returned a copy or a direct pointer.
    const char* utfChars = env->GetStringUTFChars(jStr, nullptr);
    if (utfChars == nullptr) {
        // GetStringUTFChars throws OutOfMemoryError on failure.
        return {};
    }

    std::string result(utfChars);

    // Critical: always release the pinned UTF chars.
    env->ReleaseStringUTFChars(jStr, utfChars);

    return result;
}

// ===========================================================================
// Resolved JNI Metadata Structs
// ===========================================================================

/**
 * Holds resolved JNI references for OperationHandler's instance method.
 * Populated once at the top of triggerInstanceOperation().
 */
struct InstanceMethodMeta {
    jclass    handlerClass;       // Local ref to OperationHandler
    jmethodID performOperation;   // performOperation(String, int) → void
};

/**
 * Holds resolved JNI references for OperationHandler's static method.
 * Populated once at the top of triggerStaticEvent().
 */
struct StaticMethodMeta {
    jclass    handlerClass;       // Local ref to OperationHandler
    jmethodID handleNativeEvent;  // handleNativeEvent(int, String) → void
};

// ---------------------------------------------------------------------------

/**
 * Resolves the OperationHandler class and its instance method performOperation.
 *
 * Package path  : com.example.operations  →  com/example/operations  (JNI slashes)
 * Method sig    : void performOperation(String, int)
 * JNI descriptor: (Ljava/lang/String;I)V
 *
 * @param env  JNI environment.
 * @param out  Populated on success.
 * @return     true on success; false if a Java exception is already pending.
 */
static bool resolveInstanceMethodMeta(JNIEnv* env, InstanceMethodMeta& out)
{
    // ------------------------------------------------------------------
    // Step 1 — Locate the Java class under its package
    //   JNI always uses '/' as separator in binary class names,
    //   regardless of the '.' notation used in Java source.
    // ------------------------------------------------------------------
    out.handlerClass = env->FindClass("com/example/operations/OperationHandler");
    if (out.handlerClass == nullptr) {
        throwJavaException(env,
            "java/lang/RuntimeException",
            "JNI: Class not found — com.example.operations.OperationHandler. "
            "Verify the package name and classpath."
        );
        return false;
    }

    // ------------------------------------------------------------------
    // Step 2 — Resolve the instance method ID
    //   void performOperation(String operationType, int priority)
    //   Descriptor breakdown:
    //     (                     parameter list start
    //       Ljava/lang/String;  first param: String
    //       I                   second param: int
    //     )                     parameter list end
    //     V                     return type: void
    // ------------------------------------------------------------------
    out.performOperation = env->GetMethodID(
        out.handlerClass,
        "performOperation",         // Exact Java method name
        "(Ljava/lang/String;I)V"    // JNI descriptor
    );

    if (out.performOperation == nullptr) {
        env->DeleteLocalRef(out.handlerClass);
        throwJavaException(env,
            "java/lang/RuntimeException",
            "JNI: Method not found — "
            "OperationHandler.performOperation(String, int). "
            "Verify the method name and descriptor (Ljava/lang/String;I)V."
        );
        return false;
    }

    return true;
}

// ---------------------------------------------------------------------------

/**
 * Resolves the OperationHandler class and its static method handleNativeEvent.
 *
 * Method sig    : static void handleNativeEvent(int, String)
 * JNI descriptor: (ILjava/lang/String;)V
 *
 * @param env  JNI environment.
 * @param out  Populated on success.
 * @return     true on success; false if a Java exception is already pending.
 */
static bool resolveStaticMethodMeta(JNIEnv* env, StaticMethodMeta& out)
{
    // ------------------------------------------------------------------
    // Step 1 — Same class lookup, same binary name format
    // ------------------------------------------------------------------
    out.handlerClass = env->FindClass("com/example/operations/OperationHandler");
    if (out.handlerClass == nullptr) {
        throwJavaException(env,
            "java/lang/RuntimeException",
            "JNI: Class not found — com.example.operations.OperationHandler."
        );
        return false;
    }

    // ------------------------------------------------------------------
    // Step 2 — Resolve the STATIC method ID
    //   GetStaticMethodID is used exclusively for static methods.
    //   Using GetMethodID for a static method (or vice versa) will fail.
    //
    //   static void handleNativeEvent(int eventCode, String payload)
    //   Descriptor breakdown:
    //     (                     parameter list start
    //       I                   first param: int
    //       Ljava/lang/String;  second param: String
    //     )                     parameter list end
    //     V                     return type: void
    // ------------------------------------------------------------------
    out.handleNativeEvent = env->GetStaticMethodID(
        out.handlerClass,
        "handleNativeEvent",        // Exact Java method name
        "(ILjava/lang/String;)V"    // JNI descriptor
    );

    if (out.handleNativeEvent == nullptr) {
        env->DeleteLocalRef(out.handlerClass);
        throwJavaException(env,
            "java/lang/RuntimeException",
            "JNI: Static method not found — "
            "OperationHandler.handleNativeEvent(int, String). "
            "Verify the method name and descriptor (ILjava/lang/String;)V."
        );
        return false;
    }

    return true;
}

// ===========================================================================
// JNI Entry Points
// ===========================================================================

// ---------------------------------------------------------------------------
// triggerInstanceOperation — calls an instance method on a passed-in object
// ---------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_example_operations_NativeOperationBridge_triggerInstanceOperation(
    JNIEnv* env,
    jobject /* thiz */,
    jobject handler,
    jstring operationType,
    jint    priority)
{
    // -----------------------------------------------------------------------
    // 1. Validate the handler object passed from Java
    // -----------------------------------------------------------------------
    if (handler == nullptr) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "JNI: 'handler' argument must not be null."
        );
        return;
    }

    // -----------------------------------------------------------------------
    // 2. Resolve the class (under com.example.operations) and method ID
    // -----------------------------------------------------------------------
    InstanceMethodMeta meta{};
    if (!resolveInstanceMethodMeta(env, meta)) {
        return; // Exception already pending
    }

    // -----------------------------------------------------------------------
    // 3. Convert the jstring operationType to a usable C++ value
    //    (Optional here since we pass jstring directly to Java,
    //     but useful for any native-side logic or logging.)
    // -----------------------------------------------------------------------
    std::string opTypeStr = jstringToStdString(env, operationType);
    if (env->ExceptionCheck()) {
        env->DeleteLocalRef(meta.handlerClass);
        return;
    }

    // -----------------------------------------------------------------------
    // 4. Invoke the instance method on the provided handler object
    //
    //    CallVoidMethod(object, methodID, args...)
    //      - object    : the Java instance to call the method on
    //      - methodID  : resolved via GetMethodID (NOT GetStaticMethodID)
    //      - args      : forwarded as variadic C args, matched to descriptor
    //
    //    Equivalent Java: handler.performOperation(operationType, priority);
    // -----------------------------------------------------------------------
    env->CallVoidMethod(handler, meta.performOperation, operationType, priority);

    // -----------------------------------------------------------------------
    // 5. Check for exceptions thrown inside the Java method body
    // -----------------------------------------------------------------------
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe(); // Logs the stack trace to stderr
        env->ExceptionClear();    // Clear to allow graceful native-side handling
        throwJavaException(env,
            "java/lang/RuntimeException",
            "JNI: Exception occurred inside OperationHandler.performOperation()."
        );
    }

    // -----------------------------------------------------------------------
    // 6. Release local references explicitly
    //    The JVM cleans up local refs at the end of the native frame,
    //    but explicit cleanup is good practice, especially in loops.
    // -----------------------------------------------------------------------
    env->DeleteLocalRef(meta.handlerClass);
}

// ---------------------------------------------------------------------------
// triggerStaticEvent — calls a static method without any object instance
// ---------------------------------------------------------------------------

JNIEXPORT void JNICALL
Java_com_example_operations_NativeOperationBridge_triggerStaticEvent(
    JNIEnv* env,
    jobject /* thiz */,
    jint    eventCode,
    jstring payload)
{
    // -----------------------------------------------------------------------
    // 1. Resolve the class (under com.example.operations) and static method ID
    // -----------------------------------------------------------------------
    StaticMethodMeta meta{};
    if (!resolveStaticMethodMeta(env, meta)) {
        return; // Exception already pending
    }

    // -----------------------------------------------------------------------
    // 2. Invoke the static method
    //
    //    CallStaticVoidMethod(class, staticMethodID, args...)
    //      - class          : the jclass (NOT an object instance)
    //      - staticMethodID : resolved via GetStaticMethodID
    //      - args           : forwarded as variadic C args
    //
    //    Equivalent Java: OperationHandler.handleNativeEvent(eventCode, payload);
    //
    //    Key distinction:
    //      Instance method  →  CallVoidMethod(object,  methodID,       args)
    //      Static method    →  CallStaticVoidMethod(class, methodID,   args)
    // -----------------------------------------------------------------------
    env->CallStaticVoidMethod(meta.handlerClass, meta.handleNativeEvent,
                              eventCode, payload);

    // -----------------------------------------------------------------------
    // 3. Check for exceptions thrown inside the static method
    // -----------------------------------------------------------------------
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        throwJavaException(env,
            "java/lang/RuntimeException",
            "JNI: Exception occurred inside "
            "OperationHandler.handleNativeEvent()."
        );
    }

    // -----------------------------------------------------------------------
    // 4. Release local references
    // -----------------------------------------------------------------------
    env->DeleteLocalRef(meta.handlerClass);
}