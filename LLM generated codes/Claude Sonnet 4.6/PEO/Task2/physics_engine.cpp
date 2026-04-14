#include <jni.h>
#include <cmath>      // std::sqrt, std::pow — or use std::hypot
#include "Entity.h"   // generated via: javac -h . Entity.java

// ---------------------------------------------------------------------------
// Shared helper — pure geometry, no JNI dependency.
// Inline so it compiles away to nothing in release builds.
// ---------------------------------------------------------------------------
static inline bool checkSafetyZone(double posX,    double posY,
                                   double centerX, double centerY,
                                   double radius) {
    // Use std::hypot for numerically stable 2D distance — avoids manual sqrt(dx*dx + dy*dy)
    // which can overflow/underflow for extreme double values.
    double distance = std::hypot(posX - centerX, posY - centerY);
    return distance <= radius;
}


// ---------------------------------------------------------------------------
// APPROACH A (Less Preferred):
// Receives the full Entity jobject, but reads ONLY posX and posY.
// Risk: a future developer editing this function has structural access to
// meshData, texturePath, entityID, etc.
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jboolean JNICALL
Java_Entity_isInSafetyZone(JNIEnv *env, jobject thisObj,
                           jdouble zoneCenterX, jdouble zoneCenterY, jdouble zoneRadius) {

    // --- Step 1: Resolve the class ---
    jclass entityClass = env->GetObjectClass(thisObj);
    if (entityClass == nullptr) {
        return JNI_FALSE;   // Fail safely; do NOT throw or crash
    }

    // --- Step 2: Resolve field IDs for posX and posY ONLY ---
    // JNI type descriptor for 'double' is "D"
    jfieldID fid_posX = env->GetFieldID(entityClass, "posX", "D");
    jfieldID fid_posY = env->GetFieldID(entityClass, "posY", "D");

    if (fid_posX == nullptr || fid_posY == nullptr) {
        return JNI_FALSE;   // Field not found — fail safely
    }

    // --- Step 3: Read ONLY posX and posY — do NOT touch any other field ---
    jdouble posX = env->GetDoubleField(thisObj, fid_posX);
    jdouble posY = env->GetDoubleField(thisObj, fid_posY);

    // --- Step 4: Delegate to the pure-geometry helper ---
    return checkSafetyZone(posX, posY, zoneCenterX, zoneCenterY, zoneRadius)
               ? JNI_TRUE : JNI_FALSE;
}


// ---------------------------------------------------------------------------
// APPROACH B (RECOMMENDED — Principle of Least Privilege):
// Java extracts posX and posY before the JNI call.
// 'thisObj' is the Java 'this' reference — deliberately unused.
// meshData, texturePath, entityID, and posZ are structurally unreachable.
// ---------------------------------------------------------------------------
extern "C"
JNIEXPORT jboolean JNICALL
Java_Entity_isInSafetyZoneSecure(JNIEnv *env, jobject /*thisObj*/,
                                 jdouble posX,       jdouble posY,
                                 jdouble zoneCenterX, jdouble zoneCenterY,
                                 jdouble zoneRadius) {

    // No object introspection needed at all.
    // The geometry helper receives only the four numbers it actually needs.
    return checkSafetyZone(posX, posY, zoneCenterX, zoneCenterY, zoneRadius)
               ? JNI_TRUE : JNI_FALSE;
}