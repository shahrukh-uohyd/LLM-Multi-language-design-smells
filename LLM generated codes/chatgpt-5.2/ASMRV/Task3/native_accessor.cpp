// File: native_accessor.cpp
#include <jni.h>

extern "C"
JNIEXPORT jint JNICALL
Java_NativeAccessor_getFieldValue(JNIEnv* env, jobject /* this */, jobject holderObj) {

    if (holderObj == nullptr) {
        return 0;  // Defensive check
    }

    // Get the class of the object
    jclass holderClass = env->GetObjectClass(holderObj);
    if (holderClass == nullptr) {
        return 0;
    }

    // Locate the instance field: private int value;
    jfieldID valueField = env->GetFieldID(holderClass, "value", "I");
    if (valueField == nullptr) {
        return 0;
    }

    // Retrieve the integer field value
    jint value = env->GetIntField(holderObj, valueField);

    return value;
}
