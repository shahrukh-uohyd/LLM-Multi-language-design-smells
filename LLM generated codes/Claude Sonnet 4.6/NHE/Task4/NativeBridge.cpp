#include <jni.h>
#include <iostream>
#include <stdexcept>
#include <string>

/* ═══════════════════════════════════════════════════════════════════
 *  Utility RAII wrapper — ensures GetStringUTFChars is always paired
 *  with ReleaseStringUTFChars, even if an exception is thrown.
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

    /* Implicit conversion to const char* for easy use in printf/cout */
    const char *get()          const { return m_cstr ? m_cstr : "(null)"; }
    operator const char *()    const { return get(); }

    /* Non-copyable */
    JStringUTF(const JStringUTF &)            = delete;
    JStringUTF &operator=(const JStringUTF &) = delete;

private:
    JNIEnv     *m_env;
    jstring     m_jstr;
    const char *m_cstr;
};

/* ═══════════════════════════════════════════════════════════════════
 *  Utility: retrieve a jfieldID and throw a descriptive C++ exception
 *  if the field cannot be resolved (avoids repetitive error handling).
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
 *  Utility: retrieve a jmethodID and throw if not found.
 * ══════���════════════════════════════════════════════════════════════ */
static jmethodID getMethodIDOrThrow(JNIEnv     *env,
                                     jclass      cls,
                                     const char *name,
                                     const char *sig)
{
    jmethodID mid = env->GetMethodID(cls, name, sig);
    if (mid == nullptr) {
        env->ExceptionClear();
        throw std::runtime_error(
            std::string("Method not found: ") + name + sig);
    }
    return mid;
}

/* ═══════════════════════════════════════════════════════════════════
 *  JNI Entry Point
 *
 *  Implements: public native double computeAndApplyInterest(Account account);
 *
 *  Steps:
 *    1.  Obtain jclass from the Account jobject.
 *    2a. Resolve field ID for "balance"      (double / "D").
 *    2b. Resolve field ID for "interestRate" (double / "D").
 *    3.  Read both double values.
 *    4.  Compute interest = balance × interestRate entirely in C++.
 *    5.  Resolve method ID for applyInterest(double).
 *    6.  Call account.applyInterest(interest) from C++,
 *        passing the value derived from the field read in step 3.
 *    7.  Check for exceptions thrown by the Java method.
 *    8.  Return the computed interest to Java as a jdouble.
 * ═══════════════════════════════════════════════════════════════════ */
extern "C"
JNIEXPORT jdouble JNICALL
Java_NativeBridge_computeAndApplyInterest(JNIEnv *env,
                                          jobject  /*thisObj*/,
                                          jobject  accountObj)
{
    /* ── Guard: null object ── */
    if (accountObj == nullptr) {
        std::cerr << "[native] ERROR: received null Account object.\n";
        return -1.0;
    }

    try {
        /* ── Step 1: Get the Class of the Account object ── */
        jclass accountClass = env->GetObjectClass(accountObj);
        if (accountClass == nullptr)
            throw std::runtime_error("GetObjectClass returned null.");

        std::cout << "[native] Class obtained. Resolving fields...\n";

        /* ── Step 2a: Resolve "balance" field ID ──
         *
         *  JNI type descriptor for double → "D"
         */
        jfieldID balanceFID =
            getFieldIDOrThrow(env, accountClass, "balance", "D");

        /* ── Step 2b: Resolve "interestRate" field ID ── */
        jfieldID interestRateFID =
            getFieldIDOrThrow(env, accountClass, "interestRate", "D");

        /* ── (Optional) also read "owner" String for logging ── */
        jfieldID ownerFID =
            getFieldIDOrThrow(env, accountClass,
                              "owner", "Ljava/lang/String;");

        /* ── Step 3: Read the field values ── */
        jdouble balance =
            env->GetDoubleField(accountObj, balanceFID);

        jdouble interestRate =
            env->GetDoubleField(accountObj, interestRateFID);

        jstring ownerJStr =
            static_cast<jstring>(
                env->GetObjectField(accountObj, ownerFID));

        JStringUTF ownerStr(env, ownerJStr);   // RAII — auto-released

        std::cout << "[native] Fields read:\n"
                  << "           owner        = " << ownerStr        << "\n"
                  << "           balance       = " << balance         << "\n"
                  << "           interestRate  = " << interestRate    << "\n";

        /* ── Step 4: Compute interest in C++ ── */
        jdouble interest = balance * interestRate;

        std::cout << "[native] Computed interest = "
                  << balance << " x " << interestRate
                  << " = " << interest << "\n";

        /* ── Step 5: Resolve the method ID for applyInterest(double) ──
         *
         *  JNI method signature for void applyInterest(double):
         *    "(D)V"   →  takes one double (D), returns void (V)
         */
        jmethodID applyInterestMID =
            getMethodIDOrThrow(env, accountClass,
                               "applyInterest", "(D)V");

        std::cout << "[native] Calling account.applyInterest("
                  << interest << ")...\n";

        /* ── Step 6: Call account.applyInterest(interest) from C++ ──
         *
         *  The VALUE passed as the argument (interest) was derived
         *  entirely from fields read natively in steps 2–4.
         */
        env->CallVoidMethod(accountObj, applyInterestMID, interest);

        /* ── Step 7: Check for exception thrown by the Java method ── */
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            throw std::runtime_error("applyInterest() threw a Java exception.");
        }

        std::cout << "[native] applyInterest() completed successfully.\n";

        /* ── Step 8: Return computed interest to Java ── */
        return interest;

    } catch (const std::runtime_error &ex) {
        std::cerr << "[native] EXCEPTION: " << ex.what() << "\n";

        /*
         * If no Java exception is already pending, throw one so Java
         * can also observe the error via a checked exception.
         */
        if (!env->ExceptionCheck()) {
            jclass rteClass =
                env->FindClass("java/lang/RuntimeException");
            if (rteClass != nullptr)
                env->ThrowNew(rteClass, ex.what());
        }
        return -1.0;
    }
}