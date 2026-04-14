#include <jni.h>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>

/* ═══════════════════════════════════════════════════════════════════
 *  RAII wrapper for jdoubleArray region access.
 *
 *  Pins the native double buffer of a jdoubleArray and automatically
 *  commits + releases it when the wrapper goes out of scope.
 *  Using GetDoubleArrayElements / ReleaseDoubleArrayElements ensures
 *  the JVM sees every value we write into the buffer.
 * ═══════════════════════════════════════════════════════════════════ */
class DoubleArrayGuard {
public:
    DoubleArrayGuard(JNIEnv *env, jdoubleArray arr)
        : m_env(env), m_arr(arr), m_elems(nullptr)
    {
        m_elems = env->GetDoubleArrayElements(arr, nullptr);
        if (m_elems == nullptr)
            throw std::runtime_error("GetDoubleArrayElements returned null.");
    }

    /* Commit written values back to the Java array and release buffer */
    ~DoubleArrayGuard() {
        if (m_elems != nullptr)
            m_env->ReleaseDoubleArrayElements(m_arr, m_elems, 0); // 0 = commit + free
    }

    jdouble &operator[](jsize i) { return m_elems[i]; }

    /* Non-copyable */
    DoubleArrayGuard(const DoubleArrayGuard &)            = delete;
    DoubleArrayGuard &operator=(const DoubleArrayGuard &) = delete;

private:
    JNIEnv       *m_env;
    jdoubleArray  m_arr;
    jdouble      *m_elems;
};

/* ═══════════════════════════════════════════════════════════════════
 *  RAII wrapper for jstring → UTF-8 C string.
 *  Automatically releases the string on destruction.
 * ═══════════════════════════════════════════════════════════════════ */
class JStringUTF {
public:
    JStringUTF(JNIEnv *env, jstring jstr)
        : m_env(env), m_jstr(jstr), m_cstr(nullptr)
    {
        if (jstr != nullptr)
            m_cstr = env->GetStringUTFChars(jstr, nullptr);
    }

    ~JStringUTF() {
        if (m_cstr != nullptr)
            m_env->ReleaseStringUTFChars(m_jstr, m_cstr);
    }

    const char *get()       const { return m_cstr ? m_cstr : "(null)"; }
    operator const char *() const { return get(); }

    JStringUTF(const JStringUTF &)            = delete;
    JStringUTF &operator=(const JStringUTF &) = delete;

private:
    JNIEnv     *m_env;
    jstring     m_jstr;
    const char *m_cstr;
};

/* ═══════════════════════════════════════════════════════════════════
 *  Utility: resolve a jmethodID or throw a descriptive C++ exception.
 * ═══════════════════════════════════════════════════════════════════ */
static jmethodID getMethodIDOrThrow(JNIEnv     *env,
                                     jclass      cls,
                                     const char *name,
                                     const char *sig)
{
    jmethodID mid = env->GetMethodID(cls, name, sig);
    if (mid == nullptr) {
        env->ExceptionClear();
        throw std::runtime_error(
            std::string("Method not found: ") + name + " " + sig);
    }
    return mid;
}

/* ═══════════════════════════════════════════════════════════════════
 *  Utility: resolve a jfieldID or throw a descriptive C++ exception.
 * ═══════════════════════════════════════════════════════════════════ */
static jfieldID getFieldIDOrThrow(JNIEnv     *env,
                                   jclass      cls,
                                   const char *name,
                                   const char *sig)
{
    jfieldID fid = env->GetFieldID(cls, name, sig);
    if (fid == nullptr) {
        env->ExceptionClear();
        throw std::runtime_error(
            std::string("Field not found: ") + name + " (" + sig + ")");
    }
    return fid;
}

/* ═══════════════════════════════════════════════════════════════════
 *  JNI Entry Point
 *
 *  Implements: public native double[] processProducts(Product[] products);
 *
 *  Array iteration steps:
 *    1.  Null-check the incoming object array.
 *    2.  Get the array length with GetArrayLength.
 *    3.  Create a new jdoubleArray of the same length for results.
 *    4.  Resolve method IDs ONCE before the loop (performance critical).
 *    5.  Loop: GetObjectArrayElement → inspect element → CallDoubleMethod.
 *    6.  Check for Java exceptions after each method call.
 *    7.  Store each result into the output array via DoubleArrayGuard.
 *    8.  Delete local refs inside the loop to avoid JNI table overflow.
 *    9.  Return the filled jdoubleArray to Java.
 * ═══════════════════════════════════════════════════════════════════ */
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_NativeBridge_processProducts(JNIEnv      *env,
                                   jobject     /*thisObj*/,
                                   jobjectArray productsArray)
{
    /* ── Step 1: Null-check the incoming array ── */
    if (productsArray == nullptr) {
        std::cerr << "[native] ERROR: received null products array.\n";
        return nullptr;
    }

    try {
        /* ── Step 2: Get the array length ── */
        jsize arrayLen = env->GetArrayLength(productsArray);
        std::cout << "[native] Received array of " << arrayLen
                  << " Product object(s).\n\n";

        /* ── Step 3: Create the output jdoubleArray ── */
        jdoubleArray resultArray = env->NewDoubleArray(arrayLen);
        if (resultArray == nullptr)
            throw std::runtime_error("NewDoubleArray allocation failed.");

        /* ── Step 4: Resolve method and field IDs ONCE before the loop ──
         *
         *  We need the class to call GetMethodID / GetFieldID.
         *  Peek at element 0 to get the concrete class, but only if
         *  the array is non-empty.  For an empty array we skip resolution
         *  and return an empty result array immediately.
         */
        if (arrayLen == 0) {
            std::cout << "[native] Array is empty. Returning empty result.\n";
            return resultArray;
        }

        jobject firstElement = env->GetObjectArrayElement(productsArray, 0);
        if (firstElement == nullptr)
            throw std::runtime_error("First element of array is null.");

        jclass productClass = env->GetObjectClass(firstElement);
        env->DeleteLocalRef(firstElement);   // release the peek reference

        if (productClass == nullptr)
            throw std::runtime_error("GetObjectClass returned null.");

        /*
         *  getTotalValue() → signature "()" returns double "D" → "(D)V" would
         *  be wrong; the correct signature for  double getTotalValue()  is:
         *      "()D"   — no parameters, returns double
         */
        jmethodID getTotalValueMID =
            getMethodIDOrThrow(env, productClass, "getTotalValue", "()D");

        /*
         *  Also resolve getName() so we can log each product name natively.
         *  double getName() returns String → "()Ljava/lang/String;"
         */
        jmethodID getNameMID =
            getMethodIDOrThrow(env, productClass,
                               "getName", "()Ljava/lang/String;");

        /*
         *  Resolve "available" field for conditional logging.
         *  boolean → descriptor "Z"
         */
        jfieldID availableFID =
            getFieldIDOrThrow(env, productClass, "available", "Z");

        std::cout << "[native] Method & field IDs resolved. "
                     "Starting iteration...\n\n";

        /* ── Steps 5-8: Iterate the object array ── */

        // Pin the output buffer once; DoubleArrayGuard commits on destruction
        DoubleArrayGuard results(env, resultArray);

        for (jsize i = 0; i < arrayLen; ++i) {

            /* ── 5a: Get the i-th element from the object array ──
             *
             *  GetObjectArrayElement returns a LOCAL reference.
             *  We MUST call DeleteLocalRef at the end of each iteration
             *  to prevent exhausting the JNI local reference table
             *  (default limit is 512–1024 entries).
             */
            jobject productObj =
                env->GetObjectArrayElement(productsArray, i);

            if (productObj == nullptr) {
                std::cerr << "[native] WARNING: element " << i
                          << " is null — skipping.\n";
                results[i] = 0.0;   // store 0 for null elements
                continue;
            }

            /* ── 5b: Read the "available" field from this element ── */
            jboolean available =
                env->GetBooleanField(productObj, availableFID);

            /* ── 5c: Invoke getName() for native-side logging ── */
            jstring nameJStr =
                static_cast<jstring>(
                    env->CallObjectMethod(productObj, getNameMID));

            JStringUTF nameStr(env, nameJStr);  // RAII

            /* ── 5d: Invoke getTotalValue() on the current element ──
             *
             *  This is the core of the iteration:
             *  CallDoubleMethod dispatches to the Java method on
             *  THIS specific array element (productObj).
             */
            jdouble totalValue =
                env->CallDoubleMethod(productObj, getTotalValueMID);

            /* ── Step 6: Check for exceptions thrown by the Java method ── */
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                std::cerr << "[native] Exception in getTotalValue() at index "
                          << i << " — storing 0.\n";
                env->DeleteLocalRef(productObj);
                results[i] = 0.0;
                continue;
            }

            /* ── Step 7: Store the result in the output buffer ── */
            results[i] = totalValue;

            std::cout << "[native] [" << i << "] "
                      << nameStr
                      << (available ? " (available)" : " (unavailable)")
                      << "  →  getTotalValue() = "
                      << totalValue << "\n";

            /* ── Step 8: Delete the local reference for this element ──
             *
             *  Critical for long arrays: without this the JNI local
             *  reference table overflows and the JVM aborts.
             */
            env->DeleteLocalRef(productObj);
        }

        // DoubleArrayGuard destructor commits results[] → resultArray here
        std::cout << "\n[native] Iteration complete. "
                     "Returning result array to Java.\n";

        return resultArray;

    } catch (const std::runtime_error &ex) {
        std::cerr << "[native] EXCEPTION: " << ex.what() << "\n";

        if (!env->ExceptionCheck()) {
            jclass rteClass =
                env->FindClass("java/lang/RuntimeException");
            if (rteClass != nullptr)
                env->ThrowNew(rteClass, ex.what());
        }
        return nullptr;
    }
}