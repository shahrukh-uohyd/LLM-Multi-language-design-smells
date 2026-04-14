#include <jni.h>

extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_NativeManager_createContainer(JNIEnv *env, jobject thiz, jfloat initialValue) {
    
    // 1. Find the Java class. 
    // Note: Use the fully qualified class name with slashes instead of dots.
    jclass containerClass = env->FindClass("com/example/DataContainer");
    if (containerClass == nullptr) {
        // Class not found. JNI will automatically throw a NoClassDefFoundError in Java.
        return nullptr;
    }

    // 2. Get the Method ID for the constructor.
    // Constructors always have the name "<init>".
    // The signature "(F)V" means it takes a float (F) and returns void (V).
    // (If your constructor took a double, the signature would be "(D)V")
    jmethodID constructorID = env->GetMethodID(containerClass, "<init>", "(F)V");
    if (constructorID == nullptr) {
        // Constructor not found. JNI throws NoSuchMethodError.
        env->DeleteLocalRef(containerClass); // Clean up
        return nullptr;
    }

    // 3. Create a new instance of the class by invoking the constructor
    jobject newContainer = env->NewObject(containerClass, constructorID, initialValue);

    // 4. Clean up the local reference to the class
    // (JNI automatically cleans up local refs when the function returns, 
    // but it's good practice to do it explicitly, especially in larger functions).
    env->DeleteLocalRef(containerClass);

    // 5. Return the newly created object back to Java
    return newContainer;
}