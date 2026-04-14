#include <jni.h>

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_PhysicsEngine_isEntityInSafetyZone(JNIEnv *env, jobject thisObj, jobject entityObj, jdouble centerX, jdouble centerY, jdouble radius) {
    
    // Safety check for null object
    if (entityObj == nullptr) {
        return JNI_FALSE;
    }

    // 1. Get the Entity class
    jclass entityClass = env->GetObjectClass(entityObj);
    if (entityClass == nullptr) {
        return JNI_FALSE;
    }

    // 2. Get Field IDs for posX and posY. "D" is the JNI signature for a Java primitive 'double'
    jfieldID posXField = env->GetFieldID(entityClass, "posX", "D");
    jfieldID posYField = env->GetFieldID(entityClass, "posY", "D");

    // If either field is not found, JNI will throw a NoSuchFieldError in Java
    if (posXField == nullptr || posYField == nullptr) {
        return JNI_FALSE; 
    }

    // 3. Extract the double values from the Entity object
    jdouble posX = env->GetDoubleField(entityObj, posXField);
    jdouble posY = env->GetDoubleField(entityObj, posYField);

    // 4. Calculate 2D distance squared from the center of the safety zone
    // (Using distance squared avoids an expensive and unnecessary square root operation)
    double dx = posX - centerX;
    double dy = posY - centerY;
    double distanceSquared = (dx * dx) + (dy * dy);

    // 5. Check if the entity is within the radius
    if (distanceSquared <= (radius * radius)) {
        return JNI_TRUE;
    }

    return JNI_FALSE;
}