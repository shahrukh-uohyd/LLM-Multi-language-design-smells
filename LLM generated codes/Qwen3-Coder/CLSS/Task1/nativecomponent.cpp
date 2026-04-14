#include <jni.h>
#include <thread>
#include <chrono>
#include <string>
#include <atomic>

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

// Helper function to call Java callback
void callJavaCallback(JNIEnv* env, jobject obj, long handlerId, int statusCode, const std::string& description) {
    // Find the Java class
    jclass cls = env->GetObjectClass(obj);
    if (cls == nullptr) {
        return;
    }
    
    // Find the updateStatus method
    jmethodID methodId = env->GetMethodID(cls, "updateStatus", "(JILjava/lang/String;)V");
    if (methodId == nullptr) {
        return;
    }
    
    // Create Java string
    jstring jDescription = env->NewStringUTF(description.c_str());
    if (jDescription == nullptr) {
        return;
    }
    
    // Call the Java method
    env->CallVoidMethod(obj, methodId, handlerId, statusCode, jDescription);
    
    // Clean up local references
    env->DeleteLocalRef(jDescription);
    
    // Check for exceptions
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

// Background calculation function
void performLongRunningCalculation(JNIEnv* env, jobject javaObject, long handlerId) {
    // Keep a global reference to the Java object so we can use it later
    jobject globalRef = env->NewGlobalRef(javaObject);
    
    // Simulate long-running calculation with periodic updates
    for (int i = 0; i <= 100; i += 5) {
        // Perform some calculation work here
        std::this_thread::sleep_for(std::chrono::milliseconds(200)); // Simulate work
        
        // Prepare status update
        int statusCode = i; // Using progress percentage as status code
        std::string description = "Processing step " + std::to_string(i / 5) + "/20";
        
        // Get fresh JNI environment for this thread
        JNIEnv* threadEnv = getEnvForCurrentThread();
        if (threadEnv != nullptr) {
            callJavaCallback(threadEnv, globalRef, handlerId, statusCode, description);
        }
        
        // You can add cancellation logic here if needed
        // For example, check if the handler still exists in Java
    }
    
    // Send completion status
    JNIEnv* threadEnv = getEnvForCurrentThread();
    if (threadEnv != nullptr) {
        callJavaCallback(threadEnv, globalRef, handlerId, 100, "Calculation completed successfully");
    }
    
    // Clean up global reference
    env->DeleteGlobalRef(globalRef);
}

// JNI OnLoad function - called when the library is loaded
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

// JNI function implementation
extern "C" JNIEXPORT void JNICALL Java_NativeComponent_startCalculation
(JNIEnv* env, jobject obj, jlong handlerId) {
    
    // Start the calculation in a separate thread
    std::thread calcThread([env, obj, handlerId]() {
        // Make a local copy of the object reference for the thread
        jobject localRef = env->NewLocalRef(obj);
        performLongRunningCalculation(env, localRef, handlerId);
        env->DeleteLocalRef(localRef);
    });
    
    // Detach the thread from the main execution
    calcThread.detach();
}