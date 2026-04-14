// physics_engine.cpp
#include <jni.h>
#include <android/log.h> // Optional: for logging on Android
#include <iostream>

// Global field ID cache to avoid repeated field lookups (critical for performance)
static jfieldID g_velocityFieldID = nullptr;

extern "C" {

JNIEXPORT void JNICALL Java_Entity_updateVelocityNative(JNIEnv *env, jobject entityObj, jfloat newVelocity) {
    // If we haven't cached the field ID yet, do it now
    if (g_velocityFieldID == nullptr) {
        jclass entityClass = env->GetObjectClass(entityObj);
        
        // Find the velocity field in the Entity class
        g_velocityFieldID = env->GetFieldID(entityClass, "velocity", "F");
        
        // Check for errors
        if (g_velocityFieldID == nullptr) {
            // Field not found - log error or throw exception
            jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
            env->ThrowNew(exceptionClass, "Could not find 'velocity' field in Entity class");
            return;
        }
    }
    
    // Update the velocity field with the new value
    env->SetFloatField(entityObj, g_velocityFieldID, newVelocity);
}

} // extern "C"

// Optional: Function to initialize the native library properly
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    
    // Pre-cache the field ID during initialization for better performance
    jclass entityClass = env->FindClass("Entity");
    if (entityClass != nullptr) {
        g_velocityFieldID = env->GetFieldID(entityClass, "velocity", "F");
    }
    
    return JNI_VERSION_1_6;
}

// Optional: Function to clean up when unloading
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    // Reset global field ID
    g_velocityFieldID = nullptr;
}