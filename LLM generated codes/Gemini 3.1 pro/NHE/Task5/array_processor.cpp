#include <jni.h>
#include <iostream>

extern "C"
JNIEXPORT void JNICALL Java_com_example_NativeManager_processObjectArray
  (JNIEnv *env, jobject thisObj, jobjectArray objArray) {

    // 1. Ensure the array is not null
    if (objArray == nullptr) {
        std::cerr << "[C++] Error: Provided array is null." << std::endl;
        return;
    }

    // 2. Get the length of the array
    jsize length = env->GetArrayLength(objArray);
    if (length == 0) {
        std::cout << "[C++] Array is empty." << std::endl;
        return;
    }

    // 3. Cache the Method ID before the loop for performance.
    // To do this, we grab the first element to resolve its class.
    jobject firstElement = env->GetObjectArrayElement(objArray, 0);
    if (firstElement == nullptr) {
        std::cerr << "[C++] Error: First element is null." << std::endl;
        return;
    }

    jclass elementClass = env->GetObjectClass(firstElement);
    
    // We assume the method is `int getValue()`. 
    // "()I" is the JNI signature for a method taking no arguments and returning an int.
    jmethodID getValueMethodId = env->GetMethodID(elementClass, "getValue", "()I");
    
    // Clean up local references used just for setup
    env->DeleteLocalRef(firstElement);
    
    if (getValueMethodId == nullptr) {
        std::cerr << "[C++] Error: Could not find method 'getValue'." << std::endl;
        env->DeleteLocalRef(elementClass);
        return; // JNI will throw a NoSuchMethodError
    }

    std::cout << "[C++] Iterating through array of " << length << " objects..." << std::endl;

    // 4. Iterate through the array
    for (jsize i = 0; i < length; i++) {
        
        // Retrieve the object at index i
        jobject element = env->GetObjectArrayElement(objArray, i);
        if (element == nullptr) continue;

        // 5. Invoke the method on the current element
        jint retrievedValue = env->CallIntMethod(element, getValueMethodId);

        // Process the retrieved value (printing it out for this example)
        std::cout << " - Element at index " << i << " returned value: " << retrievedValue << std::endl;

        // 6. CRITICAL: Delete the local reference to the element!
        // The JNI local reference table has a limited capacity (usually 512).
        // Failing to delete this inside a loop will cause a memory leak and a JNI crash on large arrays.
        env->DeleteLocalRef(element);
    }

    // Clean up the class reference
    env->DeleteLocalRef(elementClass);
    
    std::cout << "[C++] Array processing complete." << std::endl;
}