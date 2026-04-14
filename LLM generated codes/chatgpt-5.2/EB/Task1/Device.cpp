#include <jni.h>
#include <string>
#include "Device.h"

JNIEXPORT jdouble JNICALL
Java_Device_computeHealthScore(JNIEnv* env, jobject obj) {

    jclass deviceClass = env->GetObjectClass(obj);

    // Field IDs
    jfieldID powerField =
        env->GetFieldID(deviceClass, "powerLevel", "I");
    jfieldID tempField =
        env->GetFieldID(deviceClass, "temperature", "D");
    jfieldID activeField =
        env->GetFieldID(deviceClass, "active", "Z");

    if (powerField == nullptr || tempField == nullptr || activeField == nullptr) {
        env->ExceptionClear();
        return 0.0;
    }

    // Retrieve device state
    jint powerLevel = env->GetIntField(obj, powerField);
    jdouble temperature = env->GetDoubleField(obj, tempField);
    jboolean active = env->GetBooleanField(obj, activeField);

    // Native device-related logic
    double score = 0.0;

    if (active) {
        score += powerLevel * 1.2;
        score -= temperature * 0.5;
    } else {
        score = 0.0;
    }

    // Clamp score
    if (score < 0.0) score = 0.0;
    if (score > 100.0) score = 100.0;

    return score;
}
