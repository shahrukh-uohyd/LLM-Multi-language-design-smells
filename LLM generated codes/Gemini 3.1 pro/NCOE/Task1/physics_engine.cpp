#include <jni.h>
#include <iostream>

// Global variable to cache the field ID for Entity.velocity
jfieldID cachedVelocityFieldId = nullptr;

extern "C" {

// 1. Initialization method called once by the static block in Java
JNIEXPORT void JNICALL Java_com_example_game_PhysicsEngine_initNativeIDs(JNIEnv *env, jclass clazz) {
    // Look up the Entity class
    // Note: Package paths use forward slashes in JNI
    jclass entityClass = env->FindClass("com/example/game/Entity");
    if (entityClass == nullptr) {
        std::cerr << "Error: Could not find Entity class" << std::endl;
        return; 
    }

    // Look up the field ID for 'velocity'
    // "F" is the JNI signature for a primitive float
    cachedVelocityFieldId = env->GetFieldID(entityClass, "velocity", "F");
    if (cachedVelocityFieldId == nullptr) {
        std::cerr << "Error: Could not find 'velocity' field" << std::endl;
    }

    // It is a JNI best practice to clean up local references when you are done with them
    env->DeleteLocalRef(entityClass);
}

// 2. The High-Frequency 60FPS Update Method
JNIEXPORT void JNICALL Java_com_example_game_PhysicsEngine_updateVelocityNative(
    JNIEnv *env,
    jobject thisObj,      // Refers to the PhysicsEngine Java instance making the call
    jobject entityObj,    // The Entity object passed as an argument
    jfloat newVelocity)   // The new velocity float passed as an argument
{
    // Safety check (usually true if initNativeIDs succeeded)
    if (cachedVelocityFieldId != nullptr && entityObj != nullptr) {
        
        // Directly set the float field using the cached ID.
        // This is extremely fast and safe for a 60 FPS loop.
        env->SetFloatField(entityObj, cachedVelocityFieldId, newVelocity);
        
    }
}

} // extern "C"