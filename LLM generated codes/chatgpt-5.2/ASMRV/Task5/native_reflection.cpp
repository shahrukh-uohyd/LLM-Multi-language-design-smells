// File: native_reflection.cpp
#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_NativeReflection_getFieldInfo(
        JNIEnv* env,
        jobject /* this */,
        jstring className,
        jstring fieldName) {

    const char* classNameUtf = env->GetStringUTFChars(className, nullptr);
    const char* fieldNameUtf = env->GetStringUTFChars(fieldName, nullptr);

    // Locate the class
    jclass targetClass = env->FindClass(classNameUtf);
    if (targetClass == nullptr) {
        env->ReleaseStringUTFChars(className, classNameUtf);
        env->ReleaseStringUTFChars(fieldName, fieldNameUtf);
        return env->NewStringUTF("Class not found");
    }

    // Attempt to locate the field (instance field, any access level)
    jfieldID fieldId = env->GetFieldID(
        targetClass,
        fieldNameUtf,
        "I"  // try int first
    );

    std::string result;

    if (fieldId != nullptr) {
        result = std::string("Field '") + fieldNameUtf +
                 "' found with type: int";
    } else {
        // Try String field
        fieldId = env->GetFieldID(
            targetClass,
            fieldNameUtf,
            "Ljava/lang/String;"
        );

        if (fieldId != nullptr) {
            result = std::string("Field '") + fieldNameUtf +
                     "' found with type: java.lang.String";
        } else {
            result = std::string("Field '") + fieldNameUtf +
                     "' not found";
        }
    }

    env->ReleaseStringUTFChars(className, classNameUtf);
    env->ReleaseStringUTFChars(fieldName, fieldNameUtf);

    return env->NewStringUTF(result.c_str());
}
