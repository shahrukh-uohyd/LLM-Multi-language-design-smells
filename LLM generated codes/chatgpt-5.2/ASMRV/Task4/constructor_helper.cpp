// File: constructor_helper.cpp
#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jobject JNICALL
Java_NativeConstructorHelper_prepareConstructorData(
        JNIEnv* env, jobject /* this */, jint seed) {

    // Locate ConstructorData class
    jclass dataClass = env->FindClass("ConstructorData");
    if (dataClass == nullptr) {
        return nullptr;
    }

    // Locate ConstructorData(int, String)
    jmethodID ctor = env->GetMethodID(
        dataClass,
        "<init>",
        "(ILjava/lang/String;)V"
    );
    if (ctor == nullptr) {
        return nullptr;
    }

    // Prepare constructor arguments
    jint id = seed * 100;  // derived value
    std::string name = "user_" + std::to_string(seed);

    jstring username =
        env->NewStringUTF(name.c_str());

    // Create and return ConstructorData object
    jobject result =
        env->NewObject(dataClass, ctor, id, username);

    return result;
}
