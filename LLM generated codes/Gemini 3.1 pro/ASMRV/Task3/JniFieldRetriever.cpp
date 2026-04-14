#include <jni.h>
#include <iostream>

// In C++, native methods must be wrapped in extern "C" to prevent name mangling
extern "C" {

    JNIEXPORT jint JNICALL Java_JniFieldRetriever_retrieveIntegerValue(JNIEnv *env, jobject thisObj, jobject containerObj) {
        if (containerObj == nullptr) {
            std::cerr << "C++ Error: The provided object is null." << std::endl;
            return -1;
        }

        // 1. Get the class of the passed Java object
        jclass containerClass = env->GetObjectClass(containerObj);
        if (containerClass == nullptr) {
            std::cerr << "C++ Error: Could not get the object's class." << std::endl;
            return -1;
        }

        // 2. Locate the specific instance field within that object
        // The field is named "targetValue" and its type is int (JNI signature "I")
        jfieldID fidTargetValue = env->GetFieldID(containerClass, "targetValue", "I");
        if (fidTargetValue == nullptr) {
            std::cerr << "C++ Error: Could not locate the 'targetValue' field." << std::endl;
            return -1;
        }

        // 3. Retrieve its integer value from the object
        jint retrievedValue = env->GetIntField(containerObj, fidTargetValue);

        std::cout << "C++: Successfully located field and extracted value: " << retrievedValue << std::endl;

        // 4. Return the retrieved value
        return retrievedValue;
    }

}