#include <jni.h>
#include <jni_md.h>  // Platform-specific JNI headers
#include "Entity.h"
#include <cmath>
#include <limits>

JNIEXPORT jboolean JNICALL Java_Entity_isWithinSafetyZone
  (JNIEnv *env, jobject thisObj, jdouble centerX, jdouble centerY, jdouble radius) {
    
    // Validate input parameters
    if (env == nullptr || thisObj == nullptr) {
        return JNI_FALSE;
    }
    
    // Validate radius is positive
    if (radius < 0.0) {
        return JNI_FALSE;  // Invalid radius
    }
    
    // Check for NaN values
    if (std::isnan(centerX) || std::isnan(centerY) || std::isnan(radius)) {
        return JNI_FALSE;
    }
    
    jclass entityClass = nullptr;
    jfieldID posXFieldID = nullptr;
    jfieldID posYFieldID = nullptr;
    jdouble posX = 0.0;
    jdouble posY = 0.0;
    bool isWithin = false;
    
    try {
        // Get the Entity class
        entityClass = env->GetObjectClass(thisObj);
        if (entityClass == nullptr) {
            // Exception already thrown by GetObjectClass
            return JNI_FALSE;
        }
        
        // Find the field IDs for posX and posY
        posXFieldID = env->GetFieldID(entityClass, "posX", "D");
        posYFieldID = env->GetFieldID(entityClass, "posY", "D");
        
        if (posXFieldID == nullptr || posYFieldID == nullptr) {
            // One or both fields not found - GetFieldID will have thrown an exception
            env->ExceptionDescribe();
            return JNI_FALSE;
        }
        
        // Get the values of posX and posY fields
        posX = env->GetDoubleField(thisObj, posXFieldID);
        posY = env->GetDoubleField(thisObj, posYFieldID);
        
        // Check for potential exceptions during GetDoubleField
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
            return JNI_FALSE;
        }
        
        // Validate position values are not NaN
        if (std::isnan(posX) || std::isnan(posY)) {
            return JNI_FALSE;
        }
        
        // Calculate distance from entity position to center of safety zone
        double dx = posX - centerX;
        double dy = posY - centerY;
        double distanceSquared = dx * dx + dy * dy;
        double radiusSquared = radius * radius;
        
        // Check if the entity is within the safety zone
        // Using squared distances to avoid expensive sqrt operation
        isWithin = distanceSquared <= radiusSquared;
        
    } catch (...) {
        // Handle any unexpected exceptions
        return JNI_FALSE;
    }
    
    return isWithin ? JNI_TRUE : JNI_FALSE;
}