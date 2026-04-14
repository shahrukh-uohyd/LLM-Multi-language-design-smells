// ItemNative.cpp
#include <jni.h>

extern "C"
JNIEXPORT jint JNICALL
Java_Item_sumValues(JNIEnv* env, jclass, jobjectArray itemsArray) {

    if (itemsArray == nullptr) {
        return 0;
    }

    // 1. Get array length
    jsize length = env->GetArrayLength(itemsArray);

    jint sum = 0;

    // 2. Find Item class and method ID once (not inside loop)
    jclass itemClass = env->FindClass("Item");
    if (itemClass == nullptr) {
        return 0;
    }

    jmethodID getValueMethod =
        env->GetMethodID(itemClass, "getValue", "()I");
    if (getValueMethod == nullptr) {
        return 0;
    }

    // 3. Iterate over the array
    for (jsize i = 0; i < length; i++) {

        jobject itemObj = env->GetObjectArrayElement(itemsArray, i);
        if (itemObj == nullptr) {
            continue; // skip null elements
        }

        // 4. Call getValue() on the object
        jint value = env->CallIntMethod(itemObj, getValueMethod);
        sum += value;

        // 5. Clean up local reference
        env->DeleteLocalRef(itemObj);
    }

    return sum;
}
