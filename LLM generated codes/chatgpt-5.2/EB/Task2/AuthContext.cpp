#include <jni.h>
#include <string>
#include "AuthContext.h"

JNIEXPORT jboolean JNICALL
Java_AuthContext_authenticate(JNIEnv* env, jobject obj) {

    jclass cls = env->GetObjectClass(obj);

    // Locate fields
    jfieldID usernameField =
        env->GetFieldID(cls, "username", "Ljava/lang/String;");
    jfieldID tokenField =
        env->GetFieldID(cls, "token", "Ljava/lang/String;");
    jfieldID privilegeField =
        env->GetFieldID(cls, "privilegeLevel", "I");
    jfieldID activeField =
        env->GetFieldID(cls, "active", "Z");

    if (!usernameField || !tokenField ||
        !privilegeField || !activeField) {
        env->ExceptionClear();
        return JNI_FALSE;
    }

    // Read Java-side authentication data
    jstring jUsername =
        (jstring)env->GetObjectField(obj, usernameField);
    jstring jToken =
        (jstring)env->GetObjectField(obj, tokenField);
    jint privilege =
        env->GetIntField(obj, privilegeField);
    jboolean active =
        env->GetBooleanField(obj, activeField);

    const char* username =
        env->GetStringUTFChars(jUsername, nullptr);
    const char* token =
        env->GetStringUTFChars(jToken, nullptr);

    // ---- Native authentication / integration logic ----
    bool accepted = false;

    if (active &&
        privilege >= 2 &&
        token != nullptr &&
        std::string(token).find("TOKEN-") == 0) {
        accepted = true;
    }

    // ---------------------------------------------------

    // Release resources
    env->ReleaseStringUTFChars(jUsername, username);
    env->ReleaseStringUTFChars(jToken, token);

    return accepted ? JNI_TRUE : JNI_FALSE;
}
