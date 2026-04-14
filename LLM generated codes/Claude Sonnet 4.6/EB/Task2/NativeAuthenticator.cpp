#include <jni.h>
#include <string>
#include <iostream>
#include <sstream>
#include <chrono>
#include <unordered_map>
#include <stdexcept>

// ════════════════════════════════════════════════════════════════════════════
//  Simulated credential store  (replace with a real secure store)
// ════════════════════════════════════════════════════════════════════════════
static const std::unordered_map<std::string, std::pair<std::string,std::string>>
USER_DB = {
    // username  →  { hashed_password (plain for demo),  role }
    { "alice",   { "s3cr3t!",  "ADMIN" } },
    { "bob",     { "p@ssw0rd", "USER"  } },
    { "guest",   { "guest",    "GUEST" } },
};

// Max consecutive failures before the account is locked
static const int MAX_FAILED_ATTEMPTS = 3;

// Session TTL in milliseconds (1 hour)
static const long long SESSION_TTL_MS = 3600LL * 1000LL;

// ════════════════════════════════════════════════════════════════════════════
//  JNI helpers
// ════════════════════════════════════════════════════════════════════════════

// Throw a Java RuntimeException from C++
static void throwJavaException(JNIEnv* env, const char* msg) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls) env->ThrowNew(cls, msg);
}

// Convert jstring → std::string  (returns "" if jstr is null)
static std::string jstringToStd(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* cstr = env->GetStringUTFChars(jstr, nullptr);
    std::string result(cstr);
    env->ReleaseStringUTFChars(jstr, cstr);
    return result;
}

// Convert std::string → jstring
static jstring stdToJstring(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

// ════════════════════════════════════════════════════════════════════════════
//  Struct that mirrors the Java-side objects for easy use in C++
// ════════════════════════════════════════════════════════════════════════════
struct CredentialsData {
    std::string username;
    std::string password;
    std::string clientIp;
};

struct NativeAuthState {
    CredentialsData creds;
    int             failedAttempts;
};

// ════════════════════════════════════════════════════════════════════════════
//  Read credentials and state from the Java UserAuthenticator object
// ════════════════════════════════���═══════════════════════════════════════════
static NativeAuthState readAuthState(JNIEnv* env, jobject authObj) {
    NativeAuthState state{};

    // ── UserAuthenticator ────────────────────────────────────────────────
    jclass authClass = env->GetObjectClass(authObj);
    if (!authClass) throw std::runtime_error("Cannot find UserAuthenticator class");

    // getFailedAttempts()
    jmethodID getFailedAttempts =
        env->GetMethodID(authClass, "getFailedAttempts", "()I");
    if (!getFailedAttempts) throw std::runtime_error("Cannot find getFailedAttempts()");
    state.failedAttempts = env->CallIntMethod(authObj, getFailedAttempts);

    // getCredentials()  →  AuthCredentials object
    jmethodID getCredentials =
        env->GetMethodID(authClass, "getCredentials", "()LAuthCredentials;");
    if (!getCredentials) throw std::runtime_error("Cannot find getCredentials()");

    jobject credsObj = env->CallObjectMethod(authObj, getCredentials);
    if (!credsObj) throw std::runtime_error("getCredentials() returned null");

    // ── AuthCredentials ──────────────────────────────────────────────────
    jclass credsClass = env->GetObjectClass(credsObj);
    if (!credsClass) throw std::runtime_error("Cannot find AuthCredentials class");

    auto readStr = [&](const char* method) -> std::string {
        jmethodID mid = env->GetMethodID(credsClass, method, "()Ljava/lang/String;");
        if (!mid) throw std::runtime_error(std::string("Cannot find ") + method);
        return jstringToStd(env, (jstring)env->CallObjectMethod(credsObj, mid));
    };

    state.creds.username = readStr("getUsername");
    state.creds.password = readStr("getPassword");
    state.creds.clientIp = readStr("getClientIp");

    return state;
}

// ════════════════════════════════════════════════════════════════════════════
//  Populate the embedded AuthSession object on the Java side
// ════════════════════════════════════════════════════════════════════════════
static void populateSession(JNIEnv*            env,
                            jobject            authObj,
                            const std::string& token,
                            const std::string& role,
                            long long          expiresAt,
                            bool               active)
{
    // Retrieve the session object from Java
    jclass authClass = env->GetObjectClass(authObj);
    jmethodID getSession =
        env->GetMethodID(authClass, "getSession", "()LAuthSession;");
    if (!getSession) throw std::runtime_error("Cannot find getSession()");

    jobject sessionObj = env->CallObjectMethod(authObj, getSession);
    if (!sessionObj) throw std::runtime_error("getSession() returned null");

    jclass sessionClass = env->GetObjectClass(sessionObj);

    // Helper lambda: call a void setter
    auto callStrSetter = [&](const char* method, const std::string& value) {
        jmethodID mid = env->GetMethodID(sessionClass, method,
                                         "(Ljava/lang/String;)V");
        if (!mid) throw std::runtime_error(std::string("Cannot find ") + method);
        env->CallVoidMethod(sessionObj, mid, stdToJstring(env, value));
    };

    callStrSetter("setSessionToken", token);
    callStrSetter("setRole",         role);

    // setExpiresAt(long)
    jmethodID setExpires =
        env->GetMethodID(sessionClass, "setExpiresAt", "(J)V");
    if (!setExpires) throw std::runtime_error("Cannot find setExpiresAt()");
    env->CallVoidMethod(sessionObj, setExpires, (jlong)expiresAt);

    // setActive(boolean)
    jmethodID setActive =
        env->GetMethodID(sessionClass, "setActive", "(Z)V");
    if (!setActive) throw std::runtime_error("Cannot find setActive()");
    env->CallVoidMethod(sessionObj, setActive, active ? JNI_TRUE : JNI_FALSE);
}

// ═══════════════════════════════════════════════════════════════════════════��
//  Utility: current time in milliseconds since epoch
// ════════════════════════════════════════════════════════════════════════════
static long long nowMs() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(
               system_clock::now().time_since_epoch()).count();
}

// ════════════════════════════════════════════════════════════════════════════
//  Utility: generate a simple session token  (use a CSPRNG in production)
// ════════════════════════════════════════════════════════════════════════════
static std::string generateToken(const std::string& username) {
    long long ts = nowMs();
    std::ostringstream oss;
    oss << "TOK_" << username << "_" << ts;
    return oss.str();
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: UserAuthenticator.authenticate()
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jboolean JNICALL
Java_UserAuthenticator_authenticate(JNIEnv* env, jobject authObj) {
    try {
        NativeAuthState state = readAuthState(env, authObj);

        // ── Check if account is locked ───────────────────────────────────
        if (state.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            std::cout << "[NATIVE] Account locked after "
                      << state.failedAttempts << " failed attempts. "
                      << "IP: " << state.creds.clientIp << "\n";
            return JNI_FALSE;
        }

        // ── Look up in the credential store ─────────────────────────────
        auto it = USER_DB.find(state.creds.username);
        bool valid = (it != USER_DB.end() &&
                      it->second.first == state.creds.password);

        if (!valid) {
            // Increment failed attempts on the Java side
            int newCount = state.failedAttempts + 1;
            jclass authClass = env->GetObjectClass(authObj);
            jmethodID setFailed =
                env->GetMethodID(authClass, "setFailedAttempts", "(I)V");
            if (setFailed)
                env->CallVoidMethod(authObj, setFailed, (jint)newCount);

            std::cout << "[NATIVE] Authentication FAILED for user '"
                      << state.creds.username
                      << "'. Attempt " << newCount << "/" << MAX_FAILED_ATTEMPTS
                      << ". IP: " << state.creds.clientIp << "\n";
            return JNI_FALSE;
        }

        // ── Successful auth: build and populate the session ──────────────
        std::string role      = it->second.second;
        std::string token     = generateToken(state.creds.username);
        long long   expiresAt = nowMs() + SESSION_TTL_MS;

        populateSession(env, authObj, token, role, expiresAt, true);

        std::cout << "[NATIVE] Authentication SUCCEEDED for user '"
                  << state.creds.username
                  << "' (role=" << role
                  << ", ip=" << state.creds.clientIp << ")\n";

        return JNI_TRUE;

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return JNI_FALSE;
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: UserAuthenticator.validateToken(String token)
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jboolean JNICALL
Java_UserAuthenticator_validateToken(JNIEnv* env, jobject authObj, jstring jToken) {
    try {
        std::string tokenToCheck = jstringToStd(env, jToken);
        if (tokenToCheck.empty()) return JNI_FALSE;

        // Retrieve current session from Java
        jclass authClass = env->GetObjectClass(authObj);
        jmethodID getSession =
            env->GetMethodID(authClass, "getSession", "()LAuthSession;");
        if (!getSession) throw std::runtime_error("Cannot find getSession()");

        jobject sessionObj = env->CallObjectMethod(authObj, getSession);
        if (!sessionObj) return JNI_FALSE;

        jclass sessionClass = env->GetObjectClass(sessionObj);

        // getSessionToken()
        jmethodID getToken =
            env->GetMethodID(sessionClass, "getSessionToken",
                             "()Ljava/lang/String;");
        std::string storedToken =
            jstringToStd(env, (jstring)env->CallObjectMethod(sessionObj, getToken));

        // isActive()
        jmethodID isActive =
            env->GetMethodID(sessionClass, "isActive", "()Z");
        bool active =
            (env->CallBooleanMethod(sessionObj, isActive) == JNI_TRUE);

        // getExpiresAt()
        jmethodID getExpires =
            env->GetMethodID(sessionClass, "getExpiresAt", "()J");
        long long expiresAt =
            (long long)env->CallLongMethod(sessionObj, getExpires);

        bool tokenMatch  = (storedToken == tokenToCheck);
        bool notExpired  = (nowMs() < expiresAt);
        bool valid       = active && tokenMatch && notExpired;

        std::cout << "[NATIVE] Token validation: "
                  << "match=" << tokenMatch
                  << " active=" << active
                  << " notExpired=" << notExpired
                  << " → " << (valid ? "VALID" : "INVALID") << "\n";

        return valid ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return JNI_FALSE;
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: UserAuthenticator.revokeSession()
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT void JNICALL
Java_UserAuthenticator_revokeSession(JNIEnv* env, jobject authObj) {
    try {
        // Mark the session as inactive; clear the token
        populateSession(env, authObj, /*token=*/"", /*role=*/"",
                        /*expiresAt=*/0LL, /*active=*/false);

        std::cout << "[NATIVE] Session revoked (logout).\n";

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  JNI: UserAuthenticator.getAuthReport()
// ════════════════════════════════════════════════════════════════════════════
extern "C"
JNIEXPORT jstring JNICALL
Java_UserAuthenticator_getAuthReport(JNIEnv* env, jobject authObj) {
    try {
        NativeAuthState state = readAuthState(env, authObj);

        // ── Read session data ────────────────────────────────────────────
        jclass authClass = env->GetObjectClass(authObj);
        jmethodID getSession =
            env->GetMethodID(authClass, "getSession", "()LAuthSession;");
        jobject sessionObj = env->CallObjectMethod(authObj, getSession);

        std::string token, role;
        long long   expiresAt = 0;
        bool        active    = false;

        if (sessionObj) {
            jclass sc = env->GetObjectClass(sessionObj);

            auto readSessionStr = [&](const char* method) -> std::string {
                jmethodID mid =
                    env->GetMethodID(sc, method, "()Ljava/lang/String;");
                return mid
                    ? jstringToStd(env,
                          (jstring)env->CallObjectMethod(sessionObj, mid))
                    : "<error>";
            };

            token     = readSessionStr("getSessionToken");
            role      = readSessionStr("getRole");

            jmethodID getExp =
                env->GetMethodID(sc, "getExpiresAt", "()J");
            if (getExp)
                expiresAt = (long long)env->CallLongMethod(sessionObj, getExp);

            jmethodID isAct =
                env->GetMethodID(sc, "isActive", "()Z");
            if (isAct)
                active = (env->CallBooleanMethod(sessionObj, isAct) == JNI_TRUE);
        }

        // ── Build the report string ──────────────────────────────────────
        bool locked = (state.failedAttempts >= MAX_FAILED_ATTEMPTS);

        std::ostringstream oss;
        oss << "[AUTH REPORT]\n"
            << "  Username        : " << state.creds.username         << "\n"
            << "  Client IP       : " << state.creds.clientIp         << "\n"
            << "  Failed Attempts : " << state.failedAttempts
                                      << " / " << MAX_FAILED_ATTEMPTS  << "\n"
            << "  Account Locked  : " << (locked ? "YES" : "NO")      << "\n"
            << "  Session Active  : " << (active ? "YES" : "NO")      << "\n";

        if (active) {
            oss << "  Role            : " << role                      << "\n"
                << "  Session Token   : " << token                     << "\n"
                << "  Expires At (ms) : " << expiresAt                 << "\n"
                << "  TTL Remaining   : "
                << (expiresAt - nowMs()) / 1000LL << " seconds\n";
        }

        return stdToJstring(env, oss.str());

    } catch (const std::exception& ex) {
        throwJavaException(env, ex.what());
        return nullptr;
    }
}