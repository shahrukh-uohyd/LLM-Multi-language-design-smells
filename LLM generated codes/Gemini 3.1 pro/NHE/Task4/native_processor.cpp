#include <jni.h>
#include <iostream>

extern "C"
JNIEXPORT void JNICALL Java_com_example_NativeManager_processObjectField
  (JNIEnv *env, jobject thisObj, jobject targetObj) {

    // Ensure the passed object is not null
    if (targetObj == nullptr) {
        std::cerr << "[C++] Error: Target object is null." << std::endl;
        return;
    }

    // 1. Get the class of the target Java object
    jclass targetClass = env->GetObjectClass(targetObj);
    if (targetClass == nullptr) {
        std::cerr << "[C++] Error: Could not find the class of the target object." << std::endl;
        return;
    }

    // 2. Retrieve the Field ID for a specific field
    // In this example, we look for an int field named "dataValue". 
    // "I" is the JNI signature for the primitive type 'int'.
    jfieldID fieldId = env->GetFieldID(targetClass, "dataValue", "I");
    if (fieldId == nullptr) {
        std::cerr << "[C++] Error: Could not find field 'dataValue'." << std::endl;
        env->DeleteLocalRef(targetClass);
        return; // JNI will automatically throw a NoSuchFieldError in Java
    }

    // 3. Extract the value of the field from the object
    jint retrievedValue = env->GetIntField(targetObj, fieldId);
    std::cout << "[C++] Successfully retrieved field value: " << retrievedValue << std::endl;

    // 4. Retrieve the Method ID for the method we want to invoke
    // We look for a method named "updateData" that takes an int and returns void.
    // "(I)V" is the JNI signature for `void updateData(int)`.
    jmethodID methodId = env->GetMethodID(targetClass, "updateData", "(I)V");
    if (methodId == nullptr) {
        std::cerr << "[C++] Error: Could not find method 'updateData'." << std::endl;
        env->DeleteLocalRef(targetClass);
        return; // JNI will automatically throw a NoSuchMethodError in Java
    }

    // 5. Invoke the method on the same object, passing the retrieved value as the argument
    std::cout << "[C++] Invoking method 'updateData' with the retrieved value..." << std::endl;
    env->CallVoidMethod(targetObj, methodId, retrievedValue);

    // 6. Clean up local references to prevent memory leaks
    env->DeleteLocalRef(targetClass);
}