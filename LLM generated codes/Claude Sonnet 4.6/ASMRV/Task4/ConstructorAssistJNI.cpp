/*
 * ConstructorAssistJNI.cpp
 *
 * Implements three JNI native methods.  Each one:
 *   1. Uses FindClass() to locate the Person class.
 *   2. Uses GetMethodID() with the name "<init>" to verify the target
 *      constructor exists in the JVM  (constructor presence check).
 *   3. Prepares, validates, and normalises the input data in C++.
 *   4. Packs the results into a Java String[] and returns it to Java.
 *
 * Java then uses the returned array to call the correct constructor.
 *
 * No package prefix → mangled names are simply:
 *   Java_ConstructorAssistJNI_prepareFullConstructor
 *   Java_ConstructorAssistJNI_preparePartialConstructor
 *   Java_ConstructorAssistJNI_prepareDefaultConstructor
 */

#include <jni.h>
#include <string>
#include <algorithm>
#include <cctype>
#include <sstream>

// ─────────────────────────────────────────────────────────────────────────────
// Utility helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Trim leading and trailing whitespace from a std::string. */
static std::string trim(const std::string &s) {
    size_t start = s.find_first_not_of(" \t\r\n");
    size_t end   = s.find_last_not_of (" \t\r\n");
    if (start == std::string::npos) return "";
    return s.substr(start, end - start + 1);
}

/** Convert a std::string to Title Case (first letter of each word upper). */
static std::string toTitleCase(const std::string &s) {
    std::string result = s;
    bool capitaliseNext = true;
    for (char &c : result) {
        if (std::isspace(static_cast<unsigned char>(c))) {
            capitaliseNext = true;
        } else if (capitaliseNext) {
            c = static_cast<char>(std::toupper(static_cast<unsigned char>(c)));
            capitaliseNext = false;
        } else {
            c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
        }
    }
    return result;
}

/** Convert a std::string to all-lowercase. */
static std::string toLower(const std::string &s) {
    std::string result = s;
    std::transform(result.begin(), result.end(), result.begin(),
                   [](unsigned char c){ return std::tolower(c); });
    return result;
}

/** Return true if the string contains the '@' character (basic email check). */
static bool isValidEmail(const std::string &email) {
    return email.find('@') != std::string::npos;
}

/** Clamp an integer between lo and hi (inclusive). */
static int clamp(int value, int lo, int hi) {
    if (value < lo) return lo;
    if (value > hi) return hi;
    return value;
}

/**
 * Convert a jstring parameter to a std::string.
 * Returns an empty string if jstr is null.
 */
static std::string jstringToStdString(JNIEnv *env, jstring jstr) {
    if (jstr == nullptr) return "";
    const char *cstr = env->GetStringUTFChars(jstr, nullptr);
    if (cstr == nullptr) return "";
    std::string result(cstr);
    env->ReleaseStringUTFChars(jstr, cstr);
    return result;
}

/**
 * Build a Java String[] of fixed size 5 and populate all slots.
 * Slots: [0]=tag, [1]=name, [2]=age(string), [3]=email, [4]=role
 */
static jobjectArray buildResultArray(JNIEnv            *env,
                                     const std::string &tag,
                                     const std::string &name,
                                     int                age,
                                     const std::string &email,
                                     const std::string &role)
{
    jclass    stringClass = env->FindClass("java/lang/String");
    jstring   emptyStr    = env->NewStringUTF("");

    // Create a String[5] pre-filled with empty strings
    jobjectArray arr = env->NewObjectArray(5, stringClass, emptyStr);
    if (arr == nullptr) return nullptr;

    // Convert age to string
    std::ostringstream oss;
    oss << age;
    std::string ageStr = oss.str();

    env->SetObjectArrayElement(arr, 0, env->NewStringUTF(tag.c_str()));
    env->SetObjectArrayElement(arr, 1, env->NewStringUTF(name.c_str()));
    env->SetObjectArrayElement(arr, 2, env->NewStringUTF(ageStr.c_str()));
    env->SetObjectArrayElement(arr, 3, env->NewStringUTF(email.c_str()));
    env->SetObjectArrayElement(arr, 4, env->NewStringUTF(role.c_str()));

    env->DeleteLocalRef(stringClass);
    env->DeleteLocalRef(emptyStr);

    return arr;
}

/** Throw a Java IllegalArgumentException from native code. */
static void throwIllegalArgument(JNIEnv *env, const char *msg) {
    jclass cls = env->FindClass("java/lang/IllegalArgumentException");
    if (cls != nullptr) {
        env->ThrowNew(cls, msg);
        env->DeleteLocalRef(cls);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
extern "C" {
// ─────────────────────────────────────────────────────────────────────────────

/*
 * ═══════════════════════════════════════════════════════════════════
 * Method 1 – prepareFullConstructor
 *
 * Java signature:
 *   public native String[] prepareFullConstructor(
 *       String rawName, int rawAge, String rawEmail, String rawRole);
 *
 * What happens in native code:
 *   1. Locate Person class with FindClass().
 *   2. Verify the full 4-arg constructor exists with GetMethodID("<init>").
 *   3. Trim + title-case the name   (fallback: "Anonymous" if blank).
 *   4. Clamp age to [0, 120]         (fallback: 0 if out of range).
 *   5. Lowercase + validate email    (fallback: "fallback@example.com").
 *   6. Trim + title-case role        (fallback: "Employee" if blank).
 *   7. Pack into String[5] and return.
 * ═══════════════════════════════════════════════════════════════════
 */
JNIEXPORT jobjectArray JNICALL
Java_ConstructorAssistJNI_prepareFullConstructor(JNIEnv *env,
                                                 jobject thisObj,
                                                 jstring jRawName,
                                                 jint    rawAge,
                                                 jstring jRawEmail,
                                                 jstring jRawRole)
{
    // ── Step 1: Locate the Person class ─────────────────────────────
    jclass personClass = env->FindClass("Person");
    if (personClass == nullptr) {
        throwIllegalArgument(env, "prepareFullConstructor: Person class not found");
        return nullptr;
    }

    // ── Step 2: Verify the full constructor exists in the JVM ───────
    //
    // Constructor descriptor: (Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V
    //   L...;  = object reference type
    //   I      = int
    //   V      = void (constructors always return void)
    //
    jmethodID fullCtor = env->GetMethodID(
        personClass,
        "<init>",                                                // constructor name is always "<init>"
        "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V"
    );
    if (fullCtor == nullptr) {
        env->DeleteLocalRef(personClass);
        throwIllegalArgument(env,
            "prepareFullConstructor: full 4-arg constructor not found in Person");
        return nullptr;
    }
    // fullCtor confirms the constructor exists; Java will call it using
    // the prepared data we return.

    // ── Step 3: Prepare and validate each field ──────────────────────

    // Name: trim whitespace → title-case → fallback if blank
    std::string name = trim(jstringToStdString(env, jRawName));
    if (name.empty()) name = "Anonymous";
    else              name = toTitleCase(name);

    // Age: clamp to valid human age range [0, 120]
    int age = clamp(static_cast<int>(rawAge), 0, 120);

    // Email: lowercase → validate → fallback if invalid
    std::string email = toLower(trim(jstringToStdString(env, jRawEmail)));
    if (!isValidEmail(email)) email = "fallback@example.com";

    // Role: trim → title-case → fallback if blank
    std::string role = trim(jstringToStdString(env, jRawRole));
    if (role.empty()) role = "Employee";
    else              role = toTitleCase(role);

    // ── Step 4: Pack result and return ───────────────────────────────
    env->DeleteLocalRef(personClass);
    return buildResultArray(env, "FULL", name, age, email, role);
}

/*
 * ═══════════════════════════════════════════════════════════════════
 * Method 2 – preparePartialConstructor
 *
 * Java signature:
 *   public native String[] preparePartialConstructor(
 *       String rawName, int rawAge);
 *
 * What happens in native code:
 *   1. Locate Person class.
 *   2. Verify the 2-arg (String, int) constructor with GetMethodID().
 *   3. Validate name and age.
 *   4. Return String[5] with tag "PARTIAL".
 * ═══════════════════════════════════════════════════════════════════
 */
JNIEXPORT jobjectArray JNICALL
Java_ConstructorAssistJNI_preparePartialConstructor(JNIEnv *env,
                                                    jobject thisObj,
                                                    jstring jRawName,
                                                    jint    rawAge)
{
    // ── Step 1: Locate the Person class ─────────────────────────────
    jclass personClass = env->FindClass("Person");
    if (personClass == nullptr) {
        throwIllegalArgument(env, "preparePartialConstructor: Person class not found");
        return nullptr;
    }

    // ── Step 2: Verify the 2-arg constructor exists ─────────────────
    //
    // Descriptor: (Ljava/lang/String;I)V
    //   One String reference + one int → returns void
    //
    jmethodID partialCtor = env->GetMethodID(
        personClass,
        "<init>",
        "(Ljava/lang/String;I)V"
    );
    if (partialCtor == nullptr) {
        env->DeleteLocalRef(personClass);
        throwIllegalArgument(env,
            "preparePartialConstructor: 2-arg constructor not found in Person");
        return nullptr;
    }

    // ── Step 3: Prepare and validate ────────────────────────────────

    // Name: trim → title-case → fallback
    std::string name = trim(jstringToStdString(env, jRawName));
    if (name.empty()) name = "Anonymous";
    else              name = toTitleCase(name);

    // Age: clamp to [0, 120]
    int age = clamp(static_cast<int>(rawAge), 0, 120);

    // ── Step 4: Pack result and return ───────────────────────────────
    // email and role slots left as empty strings (not used for PARTIAL)
    env->DeleteLocalRef(personClass);
    return buildResultArray(env, "PARTIAL", name, age, "", "");
}

/*
 * ═══════════════════════════════════════════════════════════════════
 * Method 3 – prepareDefaultConstructor
 *
 * Java signature:
 *   public native String[] prepareDefaultConstructor();
 *
 * What happens in native code:
 *   1. Locate Person class.
 *   2. Verify the no-arg constructor with GetMethodID("()V").
 *   3. Return String[5] with tag "DEFAULT" and zeroed data,
 *      signalling Java to call new Person().
 * ═══════════════════════════════════════════════════════════════════
 */
JNIEXPORT jobjectArray JNICALL
Java_ConstructorAssistJNI_prepareDefaultConstructor(JNIEnv *env,
                                                    jobject thisObj)
{
    // ── Step 1: Locate the Person class ─────────────────────────────
    jclass personClass = env->FindClass("Person");
    if (personClass == nullptr) {
        throwIllegalArgument(env, "prepareDefaultConstructor: Person class not found");
        return nullptr;
    }

    // ── Step 2: Verify the no-arg constructor exists ─────────────────
    //
    // Descriptor: ()V
    //   No arguments → returns void
    //
    jmethodID defaultCtor = env->GetMethodID(
        personClass,
        "<init>",
        "()V"
    );
    if (defaultCtor == nullptr) {
        env->DeleteLocalRef(personClass);
        throwIllegalArgument(env,
            "prepareDefaultConstructor: no-arg constructor not found in Person");
        return nullptr;
    }

    // ── Step 3: Return DEFAULT tag ───────────────────────────────────
    env->DeleteLocalRef(personClass);
    return buildResultArray(env, "DEFAULT", "Unknown", 0,
                            "unknown@example.com", "None");
}

// ─────────────────────────────────────────────────────────────────────────────
} // extern "C"
// ─────────────────────────────────────────────────────────────────────────────