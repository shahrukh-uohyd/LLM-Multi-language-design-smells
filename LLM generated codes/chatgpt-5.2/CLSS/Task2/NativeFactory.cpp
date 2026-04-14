#include <jni.h>
#include "NativeFactory.h"

JNIEXPORT jobject JNICALL
Java_NativeFactory_createDataContainer(JNIEnv* env,
                                       jobject,
                                       jdouble value) {

    // Find the Java class
    jclass dataClass = env->FindClass("DataContainer");
    if (dataClass == nullptr) {
        return nullptr; // Class not found
    }

    // Find the constructor: DataContainer(double)
    jmethodID ctor = env->GetMethodID(
        dataClass,
        "<init>",
        "(D)V"
    );
    if (ctor == nullptr) {
        return nullptr; // Constructor not found
    }

    // Create a new Java object
    jobject dataObject = env->NewObject(
        dataClass,
        ctor,
        value
    );

    return dataObject;
}
