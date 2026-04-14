#include <jni.h>

// Optional but strongly recommended: cache field IDs
static jfieldID velocityFieldID = nullptr;

/*
 * Class:     com_example_game_PhysicsNative
 * Method:    updateVelocityNative
 * Signature: (Lcom/example/game/Entity;F)V
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_example_game_PhysicsNative_updateVelocityNative(
        JNIEnv* env,
        jclass /* clazz */,
        jobject entityObj,
        jfloat newVelocity
) {
    if (entityObj == nullptr) {
        return; // defensive: avoid JVM crash
    }

    // Cache field ID on first call
    if (velocityFieldID == nullptr) {
        jclass entityClass = env->GetObjectClass(entityObj);
        if (entityClass == nullptr) {
            return;
        }

        velocityFieldID = env->GetFieldID(
            entityClass,
            "velocity",
            "F" // float
        );

        // Local ref cleanup
        env->DeleteLocalRef(entityClass);

        if (velocityFieldID == nullptr) {
            // Field not found → JVM will already have thrown NoSuchFieldError
            return;
        }
    }

    // Set the float field
    env->SetFloatField(entityObj, velocityFieldID, newVelocity);
}
