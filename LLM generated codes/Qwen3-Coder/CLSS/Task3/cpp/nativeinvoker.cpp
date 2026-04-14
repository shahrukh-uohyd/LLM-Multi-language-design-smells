#include <jni.h>
#include <iostream>
#include <thread>
#include <chrono>

// Global JVM reference for use in background threads
static JavaVM* g_jvm = nullptr;

// Function to get JNI environment for current thread
JNIEnv* getEnvForCurrentThread() {
    JNIEnv* env = nullptr;
    int status = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    
    if (status == JNI_EDETACHED) {
        // Thread is not attached, attach it
        if (g_jvm->AttachCurrentThread(&env, nullptr) != 0) {
            return nullptr;
        }
    } else if (status != JNI_OK) {
        return nullptr;
    }
    
    return env;
}

// Function to call Java operation service
void callJavaOperations(JNIEnv* env, jobject callbackObj) {
    // Find the OperationService class
    jclass operationServiceClass = env->FindClass("com/example/OperationService");
    if (operationServiceClass == nullptr) {
        std::cerr << "Could not find OperationService class" << std::endl;
        return;
    }
    
    // Create an instance of OperationService
    jmethodID constructor = env->GetMethodID(operationServiceClass, "<init>", "()V");
    jobject operationServiceInstance = env->NewObject(operationServiceClass, constructor);
    if (operationServiceInstance == nullptr) {
        std::cerr << "Could not create OperationService instance" << std::endl;
        env->DeleteLocalRef(operationServiceClass);
        return;
    }
    
    // Find and call the performOperation method
    jmethodID performOpMethod = env->GetMethodID(operationServiceClass, "performOperation", 
                                               "(Ljava/lang/String;I)V");
    if (performOpMethod != nullptr) {
        jstring opName = env->NewStringUTF("NativeTriggeredOperation");
        env->CallVoidMethod(operationServiceInstance, performOpMethod, opName, 999);
        env->DeleteLocalRef(opName);
        
        // Check for exceptions
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }
    
    // Call the complex operation method
    jmethodID complexOpMethod = env->GetMethodID(operationServiceClass, "executeComplexOperation",
                                                "(Ljava/lang/String;[D)Ljava/lang/String;");
    if (complexOpMethod != nullptr) {
        // Create a double array
        jdoubleArray values = env->NewDoubleArray(3);
        jdouble vals[] = {1.5, 2.7, 3.8};
        env->SetDoubleArrayRegion(values, 0, 3, vals);
        
        jstring opType = env->NewStringUTF("MathematicalSum");
        jstring result = (jstring)env->CallObjectMethod(operationServiceInstance, complexOpMethod, 
                                                       opType, values);
        
        // Convert result back to C++ string if needed
        if (result != nullptr) {
            const char* resultStr = env->GetStringUTFChars(result, nullptr);
            std::cout << "Complex operation result: " << resultStr << std::endl;
            env->ReleaseStringUTFChars(result, resultStr);
        }
        
        // Clean up local references
        env->DeleteLocalRef(values);
        env->DeleteLocalRef(opType);
        env->DeleteLocalRef(result);
        
        // Check for exceptions
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }
    
    // Call validation method
    jmethodID validateMethod = env->GetMethodID(operationServiceClass, "validateOperation",
                                               "(Ljava/lang/String;)Z");
    if (validateMethod != nullptr) {
        jstring testOp = env->NewStringUTF("TestOperation");
        jboolean isValid = env->CallBooleanMethod(operationServiceInstance, validateMethod, testOp);
        std::cout << "Validation result: " << (isValid ? "Valid" : "Invalid") << std::endl;
        env->DeleteLocalRef(testOp);
        
        // Check for exceptions
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }
    
    // Now call the callback method on the original Java object
    jclass callbackClass = env->GetObjectClass(callbackObj);
    jmethodID callbackMethod = env->GetMethodID(callbackClass, "onOperationComplete", 
                                               "(Ljava/lang/String;)V");
    if (callbackMethod != nullptr) {
        jstring message = env->NewStringUTF("Native operations completed successfully");
        env->CallVoidMethod(callbackObj, callbackMethod, message);
        env->DeleteLocalRef(message);
        
        // Check for exceptions
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }
    
    // Clean up all local references
    env->DeleteLocalRef(operationServiceClass);
    env->DeleteLocalRef(operationServiceInstance);
}

// JNI OnLoad function - called when the library is loaded
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

// JNI function implementation
extern "C" JNIEXPORT void JNICALL Java_com_example_NativeInvoker_triggerOperations
(JNIEnv* env, jobject obj) {
    
    // Keep a global reference to the Java object so we can use it later
    jobject globalRef = env->NewGlobalRef(obj);
    
    // Perform operations in a separate thread
    std::thread([globalRef]() {
        JNIEnv* threadEnv = getEnvForCurrentThread();
        if (threadEnv != nullptr) {
            callJavaOperations(threadEnv, globalRef);
        }
    }).detach();
    
    // Clean up the global reference after the thread completes
    // Note: In a real application, you'd want to clean this up properly
    // when you know the background thread has finished
}