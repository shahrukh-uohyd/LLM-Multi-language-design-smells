#include <jni.h>

JNIEXPORT jlong JNICALL
Java_com_example_NativeAnalyzer_sumValues(
        JNIEnv* env,
        jclass,
        jobjectArray recordsArray) {

    if (recordsArray == nullptr) {
        return 0;
    }

    jsize length = env->GetArrayLength(recordsArray);

    // Find UserRecord class
    jclass userRecordClass = env->FindClass("com/example/UserRecord");
    if (userRecordClass == nullptr) {
        return 0; // Class not found
    }

    // Get field ID for the integer field
    jfieldID valueField =
        env->GetFieldID(userRecordClass, "value", "I");
    if (valueField == nullptr) {
        return 0; // Field not found
    }

    jlong sum = 0;

    for (jsize i = 0; i < length; i++) {
        jobject record =
            env->GetObjectArrayElement(recordsArray, i);

        if (record != nullptr) {
            jint value = env->GetIntField(record, valueField);
            sum += value;

            // IMPORTANT: prevent local reference table overflow
            env->DeleteLocalRef(record);
        }
    }

    return sum;
}
