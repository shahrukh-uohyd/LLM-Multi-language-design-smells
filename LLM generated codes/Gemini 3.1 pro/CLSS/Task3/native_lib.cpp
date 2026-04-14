#include <jni.h>
#include <iostream>

extern "C"
JNIEXPORT void JNICALL
Java_com_example_NativeTrigger_startNativeProcess(JNIEnv *env, jobject thiz) {
    
    // 1. Locate the Java class using its fully qualified package path.
    // Replace '.' with '/' for the JNI class path.
    jclass operationManagerClass = env->FindClass("com/example/OperationManager");
    
    if (operationManagerClass == nullptr) {
        std::cerr << "Failed to find class com/example/OperationManager" << std::endl;
        return;
    }

    // 2. Get the Method ID for the static method.
    // The signature "(Ljava/lang/String;I)V" means:
    // Takes a String (Ljava/lang/String;) and an int (I), returns void (V).
    jmethodID performOpMethodID = env->GetStaticMethodID(
        operationManagerClass, 
        "performOperation", 
        "(Ljava/lang/String;I)V"
    );

    if (performOpMethodID == nullptr) {
        std::cerr << "Failed to find static method performOperation" << std::endl;
        env->DeleteLocalRef(operationManagerClass);
        return;
    }

    // 3. Prepare the arguments to pass to Java
    jstring jSourceString = env->NewStringUTF("Native C++ Core");
    jint jStatusCode = 200;

    // 4. Invoke the static Java method
    env->CallStaticVoidMethod(
        operationManagerClass, 
        performOpMethodID, 
        jSourceString, 
        jStatusCode
    );

    // 5. Clean up local references
    env->DeleteLocalRef(jSourceString);
    env->DeleteLocalRef(operationManagerClass);
}