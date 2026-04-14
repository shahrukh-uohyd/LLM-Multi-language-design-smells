#include <jni.h>
#include "HardwareConnection.h"

JNIEXPORT void JNICALL
Java_HardwareConnection_resetAfterTimeout(JNIEnv* env, jobject obj) {

    // Get the Java class
    jclass cls = env->GetObjectClass(obj);
    if (cls == nullptr) {
        return;
    }

    // Locate the private field: boolean active
    jfieldID activeField = env->GetFieldID(
        cls,
        "active",
        "Z"   // Z = boolean
    );

    if (activeField == nullptr) {
        env->ExceptionClear();
        return;
    }

    // Reset the flag to its default (false = inactive)
    env->SetBooleanField(obj, activeField, JNI_FALSE);
}
