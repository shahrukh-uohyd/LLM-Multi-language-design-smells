#include <jni.h>
#include <cmath>
#include "Entity.h"

/*
 * boolean isWithinSafetyZone(double centerX, double centerY, double radius)
 */
JNIEXPORT jboolean JNICALL
Java_Entity_isWithinSafetyZone(JNIEnv* env,
                               jobject obj,
                               jdouble centerX,
                               jdouble centerY,
                               jdouble radius) {

    // Get Entity class
    jclass cls = env->GetObjectClass(obj);
    if (cls == nullptr) {
        return JNI_FALSE;
    }

    // Get posX field
    jfieldID posXField = env->GetFieldID(cls, "posX", "D");
    // Get posY field
    jfieldID posYField = env->GetFieldID(cls, "posY", "D");

    if (posXField == nullptr || posYField == nullptr) {
        env->ExceptionClear();
        return JNI_FALSE;
    }

    // Read values
    jdouble posX = env->GetDoubleField(obj, posXField);
    jdouble posY = env->GetDoubleField(obj, posYField);

    // 2D circular boundary check
    double dx = posX - centerX;
    double dy = posY - centerY;
    double distanceSquared = dx * dx + dy * dy;

    return (distanceSquared <= radius * radius)
           ? JNI_TRUE
           : JNI_FALSE;
}
