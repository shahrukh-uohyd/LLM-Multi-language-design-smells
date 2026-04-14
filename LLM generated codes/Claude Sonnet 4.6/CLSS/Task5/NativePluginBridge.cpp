#include "NativePluginBridge.h"

#include <jni.h>
#include <string>
#include <vector>
#include <utility>

// ============================================================================
// Section 1 — JNI Utility Helpers
// ============================================================================

/**
 * Throws a named Java exception from native code.
 * Always return immediately from the calling frame after invoking this.
 *
 * @param env        JNI environment.
 * @param className  Fully-qualified class in JNI slash-notation.
 * @param message    Detail message for the exception.
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
    // If FindClass failed, NoClassDefFoundError is already pending.
}

/**
 * Safely converts a jstring to a std::string.
 * Pins JVM memory with GetStringUTFChars and immediately unpins it.
 *
 * @param env   JNI environment.
 * @param jStr  The jstring to convert. May be nullptr.
 * @return      Equivalent std::string, or "" if jStr is nullptr.
 */
static std::string jstringToStd(JNIEnv* env, jstring jStr)
{
    if (jStr == nullptr) return {};
    const char* chars = env->GetStringUTFChars(jStr, nullptr);
    if (chars == nullptr) return {};          // OutOfMemoryError pending
    std::string result(chars);
    env->ReleaseStringUTFChars(jStr, chars);  // Always release
    return result;
}

/**
 * Converts a std::string to a new jstring local reference.
 * Caller must call env->DeleteLocalRef() when done.
 */
static jstring stdToJstring(JNIEnv* env, const std::string& str)
{
    return env->NewStringUTF(str.c_str());
}

/**
 * Checks for a pending JNI exception, logs it, and clears it.
 *
 * @param env      JNI environment.
 * @param context  Short description of where the check is happening
 *                 (used in the thrown RuntimeException message).
 * @return         true  if an exception was found and cleared.
 *                 false if no exception was pending.
 */
static bool checkAndClearException(JNIEnv* env, const char* context)
{
    if (!env->ExceptionCheck()) return false;
    env->ExceptionDescribe();   // Prints Java stack trace to stderr
    env->ExceptionClear();
    throwJavaException(env, "java/lang/RuntimeException", context);
    return true;
}

// ============================================================================
// Section 2 — Resolved JNI Metadata Structs
// ============================================================================
// Each struct caches the class and method IDs needed to call one Java type.
// All fields are local references valid for the lifetime of the native call.
// ============================================================================

// ----------------------------------------------------------------------------
// 2a. PluginRegistry — static lookup
//
//   Class   : com.example.plugins.PluginRegistry
//   Method  : static IPlugin lookupPlugin(String pluginId)
//   Desc    : (Ljava/lang/String;)Lcom/example/plugins/IPlugin;
// ----------------------------------------------------------------------------
struct RegistryMeta {
    jclass    cls;           // PluginRegistry class
    jmethodID lookupPlugin;  // GetStaticMethodID result
};

static bool resolveRegistryMeta(JNIEnv* env, RegistryMeta& out)
{
    out.cls = env->FindClass("com/example/plugins/PluginRegistry");
    if (out.cls == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Class not found — com.example.plugins.PluginRegistry. "
            "Verify the package path and classpath.");
        return false;
    }

    out.lookupPlugin = env->GetStaticMethodID(
        out.cls,
        "lookupPlugin",
        "(Ljava/lang/String;)Lcom/example/plugins/IPlugin;"
    );
    if (out.lookupPlugin == nullptr) {
        env->DeleteLocalRef(out.cls);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Static method not found — PluginRegistry.lookupPlugin(String). "
            "Descriptor: (Ljava/lang/String;)Lcom/example/plugins/IPlugin;");
        return false;
    }

    return true;
}

// ----------------------------------------------------------------------------
// 2b. IPlugin — interface method execute()
//
//   Interface : com.example.plugins.IPlugin
//   Method    : PluginResult execute(PluginContext context)
//   Desc      : (Lcom/example/plugins/PluginContext;)
//                   Lcom/example/plugins/PluginResult;
// ----------------------------------------------------------------------------
struct IPluginMeta {
    jclass    cls;      // IPlugin interface class
    jmethodID execute;  // execute(PluginContext) → PluginResult
};

static bool resolveIPluginMeta(JNIEnv* env, IPluginMeta& out)
{
    out.cls = env->FindClass("com/example/plugins/IPlugin");
    if (out.cls == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Interface not found — com.example.plugins.IPlugin.");
        return false;
    }

    // JNI resolves interface methods identically to class instance methods.
    // The JVM dispatches to the concrete implementation at runtime via vtable.
    out.execute = env->GetMethodID(
        out.cls,
        "execute",
        "(Lcom/example/plugins/PluginContext;)"
        "Lcom/example/plugins/PluginResult;"
    );
    if (out.execute == nullptr) {
        env->DeleteLocalRef(out.cls);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Method not found — IPlugin.execute(PluginContext). "
            "Descriptor: (Lcom/example/plugins/PluginContext;)"
            "Lcom/example/plugins/PluginResult;");
        return false;
    }

    return true;
}

// ----------------------------------------------------------------------------
// 2c. PluginContext — constructor
//
//   Class       : com.example.plugins.PluginContext
//   Constructor : PluginContext(String operationName,
//                               Map<String,String> parameters, int flags)
//   Desc        : (Ljava/lang/String;Ljava/util/Map;I)V
// ----------------------------------------------------------------------------
struct PluginContextMeta {
    jclass    cls;       // PluginContext class
    jmethodID ctor;      // <init>(String, Map, int) → void
};

static bool resolvePluginContextMeta(JNIEnv* env, PluginContextMeta& out)
{
    out.cls = env->FindClass("com/example/plugins/PluginContext");
    if (out.cls == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Class not found — com.example.plugins.PluginContext.");
        return false;
    }

    out.ctor = env->GetMethodID(
        out.cls,
        "<init>",                                   // Constructor
        "(Ljava/lang/String;Ljava/util/Map;I)V"     // (String, Map, int) → void
    );
    if (out.ctor == nullptr) {
        env->DeleteLocalRef(out.cls);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Constructor not found — PluginContext(String, Map, int). "
            "Descriptor: (Ljava/lang/String;Ljava/util/Map;I)V");
        return false;
    }

    return true;
}

// ----------------------------------------------------------------------------
// 2d. PluginResult — result accessors
//
//   Class  : com.example.plugins.PluginResult
//   Methods: int    getCode()
//            String getMessage()
//            String getPayload()
// ----------------------------------------------------------------------------
struct PluginResultMeta {
    jclass    cls;        // PluginResult class
    jmethodID getCode;    // getCode()    → int
    jmethodID getMessage; // getMessage() → String
    jmethodID getPayload; // getPayload() → String
};

static bool resolvePluginResultMeta(JNIEnv* env, PluginResultMeta& out)
{
    out.cls = env->FindClass("com/example/plugins/PluginResult");
    if (out.cls == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Class not found — com.example.plugins.PluginResult.");
        return false;
    }

    out.getCode = env->GetMethodID(out.cls, "getCode", "()I");
    if (out.getCode == nullptr) {
        env->DeleteLocalRef(out.cls);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Method not found — PluginResult.getCode(). Descriptor: ()I");
        return false;
    }

    out.getMessage = env->GetMethodID(
        out.cls, "getMessage", "()Ljava/lang/String;");
    if (out.getMessage == nullptr) {
        env->DeleteLocalRef(out.cls);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Method not found — PluginResult.getMessage(). "
            "Descriptor: ()Ljava/lang/String;");
        return false;
    }

    out.getPayload = env->GetMethodID(
        out.cls, "getPayload", "()Ljava/lang/String;");
    if (out.getPayload == nullptr) {
        env->DeleteLocalRef(out.cls);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Method not found — PluginResult.getPayload(). "
            "Descriptor: ()Ljava/lang/String;");
        return false;
    }

    return true;
}

// ----------------------------------------------------------------------------
// 2e. PluginEventBus — static publisher
//
//   Class  : com.example.plugins.PluginEventBus
//   Method : static void publishEvent(String pluginId,
//                                     int    resultCode,
//                                     String message)
//   Desc   : (Ljava/lang/String;ILjava/lang/String;)V
// ----------------------------------------------------------------------------
struct EventBusMeta {
    jclass    cls;          // PluginEventBus class
    jmethodID publishEvent; // publishEvent(String, int, String) → void
};

static bool resolveEventBusMeta(JNIEnv* env, EventBusMeta& out)
{
    out.cls = env->FindClass("com/example/plugins/PluginEventBus");
    if (out.cls == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Class not found — com.example.plugins.PluginEventBus.");
        return false;
    }

    out.publishEvent = env->GetStaticMethodID(
        out.cls,
        "publishEvent",
        "(Ljava/lang/String;ILjava/lang/String;)V"
    );
    if (out.publishEvent == nullptr) {
        env->DeleteLocalRef(out.cls);
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: Static method not found — "
            "PluginEventBus.publishEvent(String, int, String). "
            "Descriptor: (Ljava/lang/String;ILjava/lang/String;)V");
        return false;
    }

    return true;
}

// ============================================================================
// Section 3 — HashMap Builder
// ============================================================================

/**
 * Builds a java.util.HashMap from parallel C++ key/value string vectors.
 * Called to construct the parameters Map passed to the PluginContext constructor.
 *
 * Resolves:
 *   java.util.HashMap  <init>(int initialCapacity)
 *   java.util.HashMap  put(Object key, Object value) → Object
 *
 * @param env     JNI environment.
 * @param keys    Parameter key strings.
 * @param values  Parameter value strings (must be same length as keys).
 * @return        A jobject representing the populated HashMap,
 *                or nullptr if construction failed.
 */
static jobject buildHashMap(JNIEnv*                          env,
                             const std::vector<std::string>& keys,
                             const std::vector<std::string>& values)
{
    // ------------------------------------------------------------------
    // Resolve java.util.HashMap
    // ------------------------------------------------------------------
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    if (hashMapClass == nullptr) return nullptr;

    // HashMap(int initialCapacity)
    jmethodID hashMapCtor = env->GetMethodID(hashMapClass, "<init>", "(I)V");
    if (hashMapCtor == nullptr) {
        env->DeleteLocalRef(hashMapClass);
        return nullptr;
    }

    // HashMap.put(Object, Object) → Object
    jmethodID putMethod = env->GetMethodID(
        hashMapClass, "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    );
    if (putMethod == nullptr) {
        env->DeleteLocalRef(hashMapClass);
        return nullptr;
    }

    // ------------------------------------------------------------------
    // Instantiate: new HashMap<>(keys.size())
    // ------------------------------------------------------------------
    jobject hashMap = env->NewObject(
        hashMapClass, hashMapCtor,
        static_cast<jint>(keys.size())
    );
    env->DeleteLocalRef(hashMapClass);

    if (hashMap == nullptr || env->ExceptionCheck()) {
        return nullptr;
    }

    // ------------------------------------------------------------------
    // Populate: hashMap.put(key, value) for each pair
    // ------------------------------------------------------------------
    for (std::size_t i = 0; i < keys.size(); ++i) {
        jstring jKey   = stdToJstring(env, keys[i]);
        jstring jValue = stdToJstring(env, values[i]);

        if (jKey == nullptr || jValue == nullptr) {
            env->DeleteLocalRef(jKey);
            env->DeleteLocalRef(jValue);
            env->DeleteLocalRef(hashMap);
            return nullptr;
        }

        // put() returns the previous value (jobject) — discard it
        jobject prev = env->CallObjectMethod(hashMap, putMethod, jKey, jValue);
        env->DeleteLocalRef(prev);   // Release the displaced entry (likely null)
        env->DeleteLocalRef(jKey);
        env->DeleteLocalRef(jValue);

        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(hashMap);
            return nullptr;
        }
    }

    return hashMap;  // Caller owns this local ref
}

// ============================================================================
// Section 4 — JNI Entry Point
// ============================================================================

JNIEXPORT jint JNICALL
Java_com_example_plugins_NativePluginBridge_executePlugin(
    JNIEnv*      env,
    jobject      /* thiz */,
    jstring      pluginId,
    jstring      operationName,
    jobjectArray paramKeys,
    jobjectArray paramValues,
    jint         flags)
{
    // ========================================================================
    // Step 1 — Validate mandatory inputs
    // ========================================================================
    if (pluginId == nullptr) {
        throwJavaException(env, "java/lang/IllegalArgumentException",
            "JNI: 'pluginId' must not be null.");
        return -1;
    }
    if (operationName == nullptr) {
        throwJavaException(env, "java/lang/IllegalArgumentException",
            "JNI: 'operationName' must not be null.");
        return -1;
    }

    // ========================================================================
    // Step 2 — Resolve all required JNI metadata up front
    //          Fail fast with descriptive errors before any Java calls.
    // ========================================================================
    RegistryMeta      regMeta{};
    IPluginMeta       pluginMeta{};
    PluginContextMeta ctxMeta{};
    PluginResultMeta  resultMeta{};
    EventBusMeta      busMeta{};

    if (!resolveRegistryMeta     (env, regMeta))    return -1;
    if (!resolveIPluginMeta      (env, pluginMeta)) return -1;
    if (!resolvePluginContextMeta(env, ctxMeta))    return -1;
    if (!resolvePluginResultMeta (env, resultMeta)) return -1;
    if (!resolveEventBusMeta     (env, busMeta))    return -1;

    // ========================================================================
    // Step 3 — Look up the plugin via PluginRegistry.lookupPlugin(pluginId)
    //
    //   CallStaticObjectMethod → static method returning an Object.
    //   Equivalent Java: IPlugin plugin =
    //                        PluginRegistry.lookupPlugin(pluginId);
    // ========================================================================
    jobject plugin = env->CallStaticObjectMethod(
        regMeta.cls,
        regMeta.lookupPlugin,
        pluginId
    );
    env->DeleteLocalRef(regMeta.cls);

    if (checkAndClearException(env,
            "JNI: Exception in PluginRegistry.lookupPlugin().")) {
        return -1;
    }
    if (plugin == nullptr) {
        std::string id = jstringToStd(env, pluginId);
        std::string msg = "JNI: Plugin not found in registry — id='" + id + "'.";
        throwJavaException(env, "java/lang/IllegalArgumentException", msg.c_str());
        return -1;
    }

    // ========================================================================
    // Step 4 — Unpack the parallel String[] arrays into C++ vectors
    //
    //   GetObjectArrayElement retrieves one element from a jobjectArray.
    //   Each retrieved element is a local jstring ref — release after use.
    // ========================================================================
    std::vector<std::string> cppKeys;
    std::vector<std::string> cppValues;

    if (paramKeys != nullptr && paramValues != nullptr) {
        jsize keyCount   = env->GetArrayLength(paramKeys);
        jsize valueCount = env->GetArrayLength(paramValues);
        jsize count      = (keyCount < valueCount) ? keyCount : valueCount;

        cppKeys.reserve(count);
        cppValues.reserve(count);

        for (jsize i = 0; i < count; ++i) {
            // Retrieve and convert each key
            auto jKey = static_cast<jstring>(
                env->GetObjectArrayElement(paramKeys, i));
            cppKeys.push_back(jstringToStd(env, jKey));
            env->DeleteLocalRef(jKey);

            // Retrieve and convert each value
            auto jVal = static_cast<jstring>(
                env->GetObjectArrayElement(paramValues, i));
            cppValues.push_back(jstringToStd(env, jVal));
            env->DeleteLocalRef(jVal);
        }
    }

    // ========================================================================
    // Step 5 — Build the java.util.HashMap for PluginContext parameters
    // ========================================================================
    jobject paramsMap = buildHashMap(env, cppKeys, cppValues);
    if (paramsMap == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(plugin);
        checkAndClearException(env, "JNI: Failed to build parameters HashMap.");
        return -1;
    }

    // ========================================================================
    // Step 6 — Construct the PluginContext object
    //
    //   NewObject(class, constructorID, args...)
    //   Equivalent Java: new PluginContext(operationName, paramsMap, flags)
    // ========================================================================
    jobject pluginContext = env->NewObject(
        ctxMeta.cls,
        ctxMeta.ctor,
        operationName,   // jstring  — directly passed
        paramsMap,       // jobject  — the HashMap we just built
        flags            // jint
    );
    env->DeleteLocalRef(ctxMeta.cls);
    env->DeleteLocalRef(paramsMap);   // Context constructor copies the map

    if (pluginContext == nullptr || checkAndClearException(env,
            "JNI: Exception constructing PluginContext.")) {
        env->DeleteLocalRef(plugin);
        return -1;
    }

    // ========================================================================
    // Step 7 — Invoke plugin.execute(pluginContext)
    //
    //   CallObjectMethod → instance method returning an Object.
    //   The JVM dispatches to the concrete plugin implementation at runtime.
    //   Equivalent Java: PluginResult result = plugin.execute(pluginContext);
    // ========================================================================
    jobject pluginResult = env->CallObjectMethod(
        plugin,
        pluginMeta.execute,
        pluginContext
    );
    env->DeleteLocalRef(plugin);
    env->DeleteLocalRef(pluginMeta.cls);
    env->DeleteLocalRef(pluginContext);

    if (checkAndClearException(env,
            "JNI: Exception during plugin.execute().")) {
        return -1;
    }
    if (pluginResult == nullptr) {
        throwJavaException(env, "java/lang/RuntimeException",
            "JNI: plugin.execute() returned null PluginResult.");
        return -1;
    }

    // ========================================================================
    // Step 8 — Read result fields from the PluginResult object
    //
    //   CallIntMethod    → instance method returning int
    //   CallObjectMethod → instance method returning Object (cast to jstring)
    //
    //   Equivalent Java:
    //     int    code    = pluginResult.getCode();
    //     String message = pluginResult.getMessage();
    //     String payload = pluginResult.getPayload();
    // ========================================================================
    jint resultCode = env->CallIntMethod(pluginResult, resultMeta.getCode);

    if (checkAndClearException(env,
            "JNI: Exception calling PluginResult.getCode().")) {
        env->DeleteLocalRef(pluginResult);
        env->DeleteLocalRef(resultMeta.cls);
        return -1;
    }

    auto resultMessage = static_cast<jstring>(
        env->CallObjectMethod(pluginResult, resultMeta.getMessage));

    if (checkAndClearException(env,
            "JNI: Exception calling PluginResult.getMessage().")) {
        env->DeleteLocalRef(pluginResult);
        env->DeleteLocalRef(resultMeta.cls);
        return -1;
    }

    auto resultPayload = static_cast<jstring>(
        env->CallObjectMethod(pluginResult, resultMeta.getPayload));

    if (checkAndClearException(env,
            "JNI: Exception calling PluginResult.getPayload().")) {
        env->DeleteLocalRef(pluginResult);
        env->DeleteLocalRef(resultMeta.cls);
        env->DeleteLocalRef(resultMessage);
        return -1;
    }

    // Convert results for native-side logging
    std::string stdMessage = jstringToStd(env, resultMessage);
    std::string stdPayload = jstringToStd(env, resultPayload);

    env->DeleteLocalRef(pluginResult);
    env->DeleteLocalRef(resultMeta.cls);

    // ========================================================================
    // Step 9 — Publish the result to PluginEventBus
    //
    //   CallStaticVoidMethod → static method with void return type.
    //   Equivalent Java:
    //     PluginEventBus.publishEvent(pluginId, resultCode, resultMessage);
    // ========================================================================
    env->CallStaticVoidMethod(
        busMeta.cls,
        busMeta.publishEvent,
        pluginId,        // jstring — original param, still valid
        resultCode,      // jint
        resultMessage    // jstring
    );
    env->DeleteLocalRef(busMeta.cls);
    env->DeleteLocalRef(resultMessage);
    env->DeleteLocalRef(resultPayload);

    checkAndClearException(env,
        "JNI: Exception during PluginEventBus.publishEvent().");

    // ========================================================================
    // Step 10 — Return the result code to the Java caller
    // ========================================================================
    return resultCode;
}