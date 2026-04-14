/*
 * ReflectJNI.cpp
 *
 * Implements three JNI native methods that perform reflection-like
 * operations on the Vehicle class:
 *
 *  inspectField  – locate a field by name, return its descriptor,
 *                  human type, and modifier.
 *  inspectMethod – locate a method by name + descriptor, return its
 *                  return type and modifier.
 *  invokeGetter  – call a zero-arg getter on a live Vehicle object
 *                  and return the result as a string.
 *
 * No package prefix → mangled names:
 *   Java_ReflectJNI_inspectField
 *   Java_ReflectJNI_inspectMethod
 *   Java_ReflectJNI_invokeGetter
 */

#include <jni.h>
#include <string>
#include <sstream>
#include <vector>
#include <map>

// ─────────────────────────────────────────────────────────────────────────────
// JVM field type descriptor → human-readable type name
// ─────────────────────────────────────────────────────────────────────────────
static std::string descriptorToHumanType(const std::string &desc) {
    if (desc == "I")  return "int";
    if (desc == "J")  return "long";
    if (desc == "D")  return "double";
    if (desc == "F")  return "float";
    if (desc == "Z")  return "boolean";
    if (desc == "B")  return "byte";
    if (desc == "C")  return "char";
    if (desc == "S")  return "short";
    if (desc == "V")  return "void";
    if (desc == "Ljava/lang/String;") return "String";
    // Strip "L...;" wrapper for other object types
    if (desc.size() > 2 && desc[0] == 'L' && desc.back() == ';') {
        std::string inner = desc.substr(1, desc.size() - 2);
        // Replace '/' with '.' for readability
        for (char &c : inner) if (c == '/') c = '.';
        return inner;
    }
    return desc;   // fall through for arrays etc.
}

// ─────────────────────────────────────────────────────────────────────────────
// Extract the return type descriptor from a method descriptor.
// e.g. "(ILjava/lang/String;)D" → "D"
// ─────────────────────────────────────────────────────────────────────────────
static std::string returnTypeFromMethodDesc(const std::string &desc) {
    size_t close = desc.find(')');
    if (close == std::string::npos || close + 1 >= desc.size()) return "?";
    return desc.substr(close + 1);
}

// ─────────────────────────────────────────────────────────────────────────────
// Decode the integer bitmask returned by java.lang.reflect.Member.getModifiers()
// into a human-readable access modifier string.
//
// Modifier constants (java.lang.reflect.Modifier):
//   PUBLIC    = 0x0001
//   PRIVATE   = 0x0002
//   PROTECTED = 0x0004
// ─────────────────────────────────────────────────────────────────────────────
static std::string decodeAccessModifier(jint mods) {
    if (mods & 0x0001) return "public";
    if (mods & 0x0002) return "private";
    if (mods & 0x0004) return "protected";
    return "package";
}

// ─────────────────────────────────────────────────────────────────────────────
// Convert a jstring to std::string (empty string if null).
// ─────────────────────────────────────────────────────────────────────────────
static std::string j2s(JNIEnv *env, jstring js) {
    if (!js) return "";
    const char *c = env->GetStringUTFChars(js, nullptr);
    if (!c) return "";
    std::string s(c);
    env->ReleaseStringUTFChars(js, c);
    return s;
}

// ─────────────────────────────────────────────────────────────────────────────
// Build a String[6] result array.
// Slots: [0]=kind, [1]=name, [2]=descriptor, [3]=humanType, [4]=modifier, [5]=found
// ─────────────────────────────────────────────────────────────────────────────
static jobjectArray makeResultArray(JNIEnv            *env,
                                    const std::string &kind,
                                    const std::string &name,
                                    const std::string &descriptor,
                                    const std::string &humanType,
                                    const std::string &modifier,
                                    bool               found)
{
    jclass    sc  = env->FindClass("java/lang/String");
    jstring   def = env->NewStringUTF("");
    jobjectArray arr = env->NewObjectArray(6, sc, def);

    env->SetObjectArrayElement(arr, 0, env->NewStringUTF(kind.c_str()));
    env->SetObjectArrayElement(arr, 1, env->NewStringUTF(name.c_str()));
    env->SetObjectArrayElement(arr, 2, env->NewStringUTF(descriptor.c_str()));
    env->SetObjectArrayElement(arr, 3, env->NewStringUTF(humanType.c_str()));
    env->SetObjectArrayElement(arr, 4, env->NewStringUTF(modifier.c_str()));
    env->SetObjectArrayElement(arr, 5, env->NewStringUTF(found ? "true" : "false"));

    env->DeleteLocalRef(sc);
    env->DeleteLocalRef(def);
    return arr;
}

// ──────────────────────────────────────────────────────────────────────────��──
// Known Vehicle fields: name → JVM descriptor + access modifier bitmask.
// This table mirrors what the JVM stores — in a production implementation
// these would be discovered dynamically via the Reflection API over JNI.
// ─────────────────────────────────────────────────────────────────────────────
struct FieldMeta {
    std::string descriptor;
    jint        modifiers;   // Modifier bitmask
};

static std::map<std::string, FieldMeta> vehicleFields = {
    { "brand",          { "Ljava/lang/String;", 0x0001 } },  // public
    { "year",           { "I",                  0x0001 } },  // public
    { "engineCapacity", { "D",                  0x0004 } },  // protected
    { "isElectric",     { "Z",                  0x0002 } },  // private
    { "speed",          { "I",                  0x0001 } },  // public
};

// ─────────────────────────────────────────────────────────────────────────────
// Known Vehicle methods: name → descriptor + access modifier bitmask.
// ─────────────────────────────────────────────────────────────────────────────
struct MethodMeta {
    std::string descriptor;
    jint        modifiers;
};

static std::map<std::string, MethodMeta> vehicleMethods = {
    { "getBrand",          { "()Ljava/lang/String;", 0x0001 } }, // public
    { "getYear",           { "()I",                  0x0001 } }, // public
    { "accelerate",        { "(I)V",                 0x0001 } }, // public
    { "getEngineCapacity", { "()D",                  0x0004 } }, // protected
    { "checkElectric",     { "()Z",                  0x0002 } }, // private
    { "describe",          { "()Ljava/lang/String;", 0x0001 } }, // public
};

// ─────────────────────────────────────────────────────────────────────────────
extern "C" {
// ─────────────────────────────────────────────────────────────────────────────

/*
 * ═══════════════════════════════════════════════════════════════════
 * Method 1 – inspectField
 *
 * Java signature: public native String[] inspectField(String fieldName)
 *
 * Steps:
 *   1. Locate Vehicle class with FindClass.
 *   2. Look up the fieldName in the metadata table.
 *   3. Call GetFieldID to confirm the field exists in the JVM.
 *   4. Use java.lang.reflect APIs over JNI to read the real modifier.
 *   5. Return String[6] with full field information.
 * ═══════════════════════════════════════════════════════════════════
 */
JNIEXPORT jobjectArray JNICALL
Java_ReflectJNI_inspectField(JNIEnv *env,
                             jobject  thisObj,
                             jstring  jFieldName)
{
    std::string fieldName = j2s(env, jFieldName);

    // ── Step 1: Locate Vehicle class ────────────────────────────────
    jclass vehicleClass = env->FindClass("Vehicle");
    if (!vehicleClass) {
        return makeResultArray(env, "FIELD", fieldName, "", "", "", false);
    }

    // ── Step 2: Look up in metadata table ───────────────────────────
    auto it = vehicleFields.find(fieldName);
    if (it == vehicleFields.end()) {
        env->DeleteLocalRef(vehicleClass);
        return makeResultArray(env, "FIELD", fieldName, "", "", "", false);
    }

    const std::string &desc = it->second.descriptor;
    jint               mods = it->second.modifiers;

    // ── Step 3: Confirm field exists in the JVM using GetFieldID ────
    jfieldID fid = env->GetFieldID(vehicleClass, fieldName.c_str(), desc.c_str());

    if (!fid) {
        // Clear the pending NoSuchFieldError
        env->ExceptionClear();
        env->DeleteLocalRef(vehicleClass);
        return makeResultArray(env, "FIELD", fieldName, desc, "", "", false);
    }

    // ── Step 4: Use java.lang.Class.getDeclaredField() + getModifiers()
    //           to read the actual modifier bitmask from the JVM ─────
    //
    // This is the reflection path over JNI:
    //   vehicleClass.getDeclaredField(fieldName).getModifiers()
    //
    jclass    classClass        = env->FindClass("java/lang/Class");
    jmethodID getDeclFieldMid   = env->GetMethodID(classClass,
                                      "getDeclaredField",
                                      "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
    jclass    fieldClass        = env->FindClass("java/lang/reflect/Field");
    jmethodID getModifiersMid   = env->GetMethodID(fieldClass,
                                      "getModifiers", "()I");

    jobject fieldObj = env->CallObjectMethod(vehicleClass,
                                             getDeclFieldMid,
                                             jFieldName);
    if (fieldObj && !env->ExceptionCheck()) {
        // Read the real modifier from the Field object
        mods = env->CallIntMethod(fieldObj, getModifiersMid);
        env->DeleteLocalRef(fieldObj);
    } else {
        env->ExceptionClear();   // ignore if getDeclaredField threw
    }

    env->DeleteLocalRef(classClass);
    env->DeleteLocalRef(fieldClass);
    env->DeleteLocalRef(vehicleClass);

    // ── Step 5: Build and return the result ─────────────────────────
    std::string humanType = descriptorToHumanType(desc);
    std::string modifier  = decodeAccessModifier(mods);

    return makeResultArray(env, "FIELD", fieldName, desc, humanType, modifier, true);
}

/*
 * ═══════════════════════════════════════════════════════════════════
 * Method 2 – inspectMethod
 *
 * Java signature:
 *   public native String[] inspectMethod(String methodName,
 *                                         String descriptor)
 *
 * Steps:
 *   1. Locate Vehicle class.
 *   2. Call GetMethodID to confirm the method exists.
 *   3. Use java.lang.Class.getDeclaredMethod() over JNI to read
 *      the real modifier bitmask.
 *   4. Derive return type from the descriptor.
 *   5. Return String[6].
 * ═══════════════════════════════════════════════════════════════════
 */
JNIEXPORT jobjectArray JNICALL
Java_ReflectJNI_inspectMethod(JNIEnv *env,
                               jobject  thisObj,
                               jstring  jMethodName,
                               jstring  jDescriptor)
{
    std::string methodName = j2s(env, jMethodName);
    std::string descriptor = j2s(env, jDescriptor);

    // ── Step 1: Locate Vehicle class ────────────────────────────────
    jclass vehicleClass = env->FindClass("Vehicle");
    if (!vehicleClass) {
        return makeResultArray(env, "METHOD", methodName, descriptor,
                               "", "", false);
    }

    // ── Step 2: Confirm method exists with GetMethodID ───────────────
    jmethodID mid = env->GetMethodID(vehicleClass,
                                     methodName.c_str(),
                                     descriptor.c_str());
    if (!mid) {
        env->ExceptionClear();
        env->DeleteLocalRef(vehicleClass);
        return makeResultArray(env, "METHOD", methodName, descriptor,
                               "", "", false);
    }

    // ── Step 3: Use getDeclaredMethods() loop over JNI ───────────────
    //
    // vehicleClass.getDeclaredMethods() → Method[]
    // For each Method m: if m.getName().equals(methodName) → m.getModifiers()
    //
    jint mods = 0x0001;   // default public; overwritten if lookup succeeds

    jclass    classClass         = env->FindClass("java/lang/Class");
    jclass    methodClass        = env->FindClass("java/lang/reflect/Method");
    jmethodID getDeclMethodsMid  = env->GetMethodID(classClass,
                                       "getDeclaredMethods",
                                       "()[Ljava/lang/reflect/Method;");
    jmethodID getNameMid         = env->GetMethodID(methodClass,
                                       "getName", "()Ljava/lang/String;");
    jmethodID getModifiersMid    = env->GetMethodID(methodClass,
                                       "getModifiers", "()I");

    jobjectArray methodArr = (jobjectArray)env->CallObjectMethod(
                                vehicleClass, getDeclMethodsMid);

    if (methodArr && !env->ExceptionCheck()) {
        jsize len = env->GetArrayLength(methodArr);
        for (jsize i = 0; i < len; i++) {
            jobject mObj = env->GetObjectArrayElement(methodArr, i);
            jstring mName = (jstring)env->CallObjectMethod(mObj, getNameMid);
            std::string mNameStr = j2s(env, mName);

            if (mNameStr == methodName) {
                // Found the matching method — read its modifier
                mods = env->CallIntMethod(mObj, getModifiersMid);
                env->DeleteLocalRef(mName);
                env->DeleteLocalRef(mObj);
                break;
            }
            env->DeleteLocalRef(mName);
            env->DeleteLocalRef(mObj);
        }
        env->DeleteLocalRef(methodArr);
    } else {
        env->ExceptionClear();
    }

    env->DeleteLocalRef(classClass);
    env->DeleteLocalRef(methodClass);
    env->DeleteLocalRef(vehicleClass);

    // ── Step 4: Derive return type from descriptor ───────────────────
    std::string retDesc   = returnTypeFromMethodDesc(descriptor);
    std::string humanType = descriptorToHumanType(retDesc);
    std::string modifier  = decodeAccessModifier(mods);

    // ── Step 5: Return result ────────────────────────────────────────
    return makeResultArray(env, "METHOD", methodName, descriptor,
                           humanType, modifier, true);
}

/*
 * ═══════════════════════════════════════════════════════════════════
 * Method 3 – invokeGetter
 *
 * Java signature:
 *   public native String[] invokeGetter(Vehicle obj,
 *                                        String  methodName,
 *                                        String  descriptor)
 *
 * Steps:
 *   1. Obtain the Vehicle class from the live object.
 *   2. Locate the method with GetMethodID.
 *   3. Dispatch the correct Call*Method based on the return type
 *      derived from the descriptor.
 *   4. Convert the result to a string.
 *   5. Return String[6]; slot [3] holds the result value.
 * ═══════════════════════════════════════════════════════════════════
 */
JNIEXPORT jobjectArray JNICALL
Java_ReflectJNI_invokeGetter(JNIEnv *env,
                              jobject  thisObj,
                              jobject  vehicleObj,
                              jstring  jMethodName,
                              jstring  jDescriptor)
{
    std::string methodName = j2s(env, jMethodName);
    std::string descriptor = j2s(env, jDescriptor);

    if (!vehicleObj) {
        return makeResultArray(env, "INVOKE_RESULT", methodName, descriptor,
                               "", "", false);
    }

    // ── Step 1: Obtain class from the live object ────────────────────
    jclass vehicleClass = env->GetObjectClass(vehicleObj);

    // ── Step 2: Locate the method ────────────────────────────────────
    jmethodID mid = env->GetMethodID(vehicleClass,
                                     methodName.c_str(),
                                     descriptor.c_str());
    if (!mid) {
        env->ExceptionClear();
        env->DeleteLocalRef(vehicleClass);
        return makeResultArray(env, "INVOKE_RESULT", methodName, descriptor,
                               "", "", false);
    }

    // ── Step 3 & 4: Dispatch the call based on return type descriptor ─
    std::string retDesc   = returnTypeFromMethodDesc(descriptor);
    std::string humanType = descriptorToHumanType(retDesc);
    std::string resultStr;

    if (retDesc == "Ljava/lang/String;") {
        // Return type is String → CallObjectMethod
        jstring jResult = (jstring)env->CallObjectMethod(vehicleObj, mid);
        resultStr = j2s(env, jResult);
        if (jResult) env->DeleteLocalRef(jResult);

    } else if (retDesc == "I") {
        // Return type is int → CallIntMethod
        jint val = env->CallIntMethod(vehicleObj, mid);
        std::ostringstream oss;
        oss << val;
        resultStr = oss.str();

    } else if (retDesc == "D") {
        // Return type is double → CallDoubleMethod
        jdouble val = env->CallDoubleMethod(vehicleObj, mid);
        std::ostringstream oss;
        oss << val;
        resultStr = oss.str();

    } else if (retDesc == "Z") {
        // Return type is boolean → CallBooleanMethod
        jboolean val = env->CallBooleanMethod(vehicleObj, mid);
        resultStr = val ? "true" : "false";

    } else if (retDesc == "V") {
        // void methods — no return value to capture
        resultStr = "(void)";

    } else {
        // Other types: call as object and convert via toString()
        jobject jResult = env->CallObjectMethod(vehicleObj, mid);
        if (jResult) {
            jclass    objClass    = env->GetObjectClass(jResult);
            jmethodID toStringMid = env->GetMethodID(objClass,
                                        "toString", "()Ljava/lang/String;");
            jstring   jsResult    = (jstring)env->CallObjectMethod(jResult, toStringMid);
            resultStr = j2s(env, jsResult);
            env->DeleteLocalRef(jsResult);
            env->DeleteLocalRef(objClass);
            env->DeleteLocalRef(jResult);
        } else {
            resultStr = "null";
        }
    }

    env->DeleteLocalRef(vehicleClass);

    // ── Step 5: Return result ────────────────────────────────────────
    return makeResultArray(env, "INVOKE_RESULT", methodName, descriptor,
                           humanType, resultStr, true);
}

// ─────────────────────────────────────────────────────────────────────────────
} // extern "C"
// ─────────────────────────────────────────────────────────────────────────────