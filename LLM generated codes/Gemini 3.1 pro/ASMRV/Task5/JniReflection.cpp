#include <jni.h>
#include <iostream>

extern "C" {

    JNIEXPORT jstring JNICALL Java_JniReflection_getMemberInfo(
            JNIEnv *env, jobject thisObj, jstring className, jstring memberName, jstring signature, jboolean isMethod) {
        
        // Convert Java strings to C strings
        const char *cClassName = env->GetStringUTFChars(className, nullptr);
        const char *cMemberName = env->GetStringUTFChars(memberName, nullptr);
        const char *cSignature = env->GetStringUTFChars(signature, nullptr);
        
        // 1. Locate the Target Class
        jclass targetClass = env->FindClass(cClassName);
        if (targetClass == nullptr) {
            env->ExceptionClear(); // Clear the ClassNotFoundException
            return env->NewStringUTF("Error: Class not found.");
        }

        jobject reflectedMember = nullptr;

        // 2. Locate the specific Class Member (Method or Field)
        if (isMethod == JNI_TRUE) {
            jmethodID mid = env->GetMethodID(targetClass, cMemberName, cSignature);
            if (mid != nullptr) {
                // Convert JNI internal method ID to a java.lang.reflect.Method object
                reflectedMember = env->ToReflectedMethod(targetClass, mid, JNI_FALSE);
            }
        } else {
            jfieldID fid = env->GetFieldID(targetClass, cMemberName, cSignature);
            if (fid != nullptr) {
                // Convert JNI internal field ID to a java.lang.reflect.Field object
                reflectedMember = env->ToReflectedField(targetClass, fid, JNI_FALSE);
            }
        }

        // Clean up any exceptions if the member was not found
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }

        // Release C strings
        env->ReleaseStringUTFChars(className, cClassName);
        env->ReleaseStringUTFChars(memberName, cMemberName);
        env->ReleaseStringUTFChars(signature, cSignature);

        if (reflectedMember == nullptr) {
            return env->NewStringUTF("Error: Member not found.");
        }

        // 3. Extract information about it by calling toString() on the java.lang.reflect Object
        jclass memberClass = env->GetObjectClass(reflectedMember);
        jmethodID toStringMid = env->GetMethodID(memberClass, "toString", "()Ljava/lang/String;");
        
        jstring resultString = (jstring) env->CallObjectMethod(reflectedMember, toStringMid);

        // 4. Return the information to Java
        return resultString;
    }

}