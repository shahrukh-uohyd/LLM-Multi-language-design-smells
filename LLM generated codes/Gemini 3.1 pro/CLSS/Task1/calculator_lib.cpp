#include <jni.h>
#include <string>
#include <thread>
#include <chrono>

// Global pointer to the Java Virtual Machine
JavaVM* g_vm = nullptr;

// This JNI standard method is called automatically when System.loadLibrary() is executed
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

// The background worker function
void RunCalculationInBackground(jobject globalHandler) {
    JNIEnv* env = nullptr;
    
    // 1. Attach the current C++ thread to the JVM to get a valid JNIEnv pointer
    if (g_vm->AttachCurrentThread((void**)&env, nullptr) != JNI_OK) {
        return; 
    }

    // 2. Look up the class and the callback method
    jclass handlerClass = env->GetObjectClass(globalHandler);
    
    // The signature "(ILjava/lang/String;)V" means: takes an Int (I) and a String (Ljava/lang/String;), returns Void (V)
    jmethodID notifyMethod = env->GetMethodID(handlerClass, "onProgress", "(ILjava/lang/String;)V");

    if (notifyMethod != nullptr) {
        // 3. Perform the long-running calculation
        for (int i = 1; i <= 5; ++i) {
            // Simulate heavy lifting...
            std::this_thread::sleep_for(std::chrono::seconds(1));

            int currentStatus = i * 20; // 20%, 40%, etc.
            std::string statusMessage = "Completed stage " + std::to_string(i) + " of the calculation.";

            // 4. Convert C++ std::string to JNI jstring
            jstring jMessage = env->NewStringUTF(statusMessage.c_str());

            // 5. Invoke the Java callback method
            env->CallVoidMethod(globalHandler, notifyMethod, currentStatus, jMessage);

            // 6. Delete the local reference to avoid memory leaks inside the loop
            env->DeleteLocalRef(jMessage);
        }
    }

    // 7. Clean up
    env->DeleteLocalRef(handlerClass);
    env->DeleteGlobalRef(globalHandler); // Release the global ref so Java can garbage collect it
    g_vm->DetachCurrentThread();         // Detach thread before exiting
}

// The JNI wrapper that Java actually calls
extern "C"
JNIEXPORT void JNICALL
Java_com_example_Calculator_startCalculation(JNIEnv *env, jobject thiz, jobject handler) {
    
    // Create a Global Reference to the handler object so it survives after this JNI function returns
    jobject globalHandler = env->NewGlobalRef(handler);

    // Spawn the background calculation thread and detach it
    std::thread calcThread(RunCalculationInBackground, globalHandler);
    calcThread.detach(); 
}