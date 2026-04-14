// SampleNative.cpp
#include <jni.h>

extern "C"
JNIEXPORT void JNICALL
Java_Sample_nativeInspectAndInvoke(JNIEnv* env, jobject obj) {

    // 1. Get the object's class
    jclass sampleClass = env->GetObjectClass(obj);
    if (sampleClass == nullptr) {
        return; // class lookup failed
    }

    // 2. Get the field ID for "counter"
    jfieldID counterFieldId =
        env->GetFieldID(sampleClass, "counter", "I");
    if (counterFieldId == nullptr) {
        return; // field not found
    }

    // 3. Retrieve the field value
    jint counterValue = env->GetIntField(obj, counterFieldId);

    // 4. Get the method ID for "updateCounter(int)"
    jmethodID updateMethodId =
        env->GetMethodID(sampleClass, "updateCounter", "(I)V");
    if (updateMethodId == nullptr) {
        return; // method not found
    }

    // 5. Call the method on the same object,
    //    passing the retrieved field value
    env->CallVoidMethod(obj, updateMethodId, counterValue);
}
