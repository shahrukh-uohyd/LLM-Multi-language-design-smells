#include <jni.h>
#include <iostream>
#include <thread>
#include <chrono>
#include <vector>
#include <string>

// Global JVM reference
static JavaVM* g_jvm = nullptr;
static jobject g_globalControllerRef = nullptr;

// Function to get JNI environment for current thread
JNIEnv* getEnvForCurrentThread() {
    JNIEnv* env = nullptr;
    int status = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
    
    if (status == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) != 0) {
            return nullptr;
        }
    } else if (status != JNI_OK) {
        return nullptr;
    }
    
    return env;
}

// Helper function to convert Java string array to C++ vector
std::vector<std::string> convertStringArrayToVector(JNIEnv* env, jobjectArray stringArray) {
    std::vector<std::string> result;
    
    if (stringArray == nullptr) {
        return result;
    }
    
    jsize length = env->GetArrayLength(stringArray);
    for (jsize i = 0; i < length; i++) {
        jstring jstr = (jstring)env->GetObjectArrayElement(stringArray, i);
        if (jstr != nullptr) {
            const char* str = env->GetStringUTFChars(jstr, nullptr);
            if (str != nullptr) {
                result.push_back(std::string(str));
                env->ReleaseStringUTFChars(jstr, str);
            }
            env->DeleteLocalRef(jstr);
        }
    }
    
    return result;
}

// Function to find and control Java components
bool controlJavaComponents(JNIEnv* env, const std::string& command, const std::vector<std::string>& params) {
    // Find the ComponentRegistry class
    jclass registryClass = env->FindClass("com/app/controller/ComponentRegistry");
    if (registryClass == nullptr) {
        std::cerr << "Could not find ComponentRegistry class" << std::endl;
        env->ExceptionDescribe();
        env->ExceptionClear();
        return false;
    }
    
    // Find the getComponent method
    jmethodID getComponentMethod = env->GetStaticMethodID(registryClass, "getComponent", 
                                                         "(Ljava/lang/String;)Ljava/lang/Object;");
    if (getComponentMethod == nullptr) {
        std::cerr << "Could not find getComponent method" << std::endl;
        env->DeleteLocalRef(registryClass);
        return false;
    }
    
    // Get the main controller component
    jstring componentName = env->NewStringUTF("mainController");
    jobject controllerObj = env->CallStaticObjectMethod(registryClass, getComponentMethod, componentName);
    env->DeleteLocalRef(componentName);
    
    if (controllerObj == nullptr) {
        std::cerr << "Could not get mainController component" << std::endl;
        env->DeleteLocalRef(registryClass);
        return false;
    }
    
    // Get the ApplicationController class
    jclass appControllerClass = env->GetObjectClass(controllerObj);
    if (appControllerClass == nullptr) {
        std::cerr << "Could not get ApplicationController class" << std::endl;
        env->DeleteLocalRef(controllerObj);
        env->DeleteLocalRef(registryClass);
        return false;
    }
    
    // Execute the requested command
    if (command == "start") {
        jmethodID startMethod = env->GetMethodID(appControllerClass, "startApplication", "()V");
        if (startMethod != nullptr) {
            env->CallVoidMethod(controllerObj, startMethod);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }
    } else if (command == "stop") {
        jmethodID stopMethod = env->GetMethodID(appControllerClass, "stopApplication", "()V");
        if (stopMethod != nullptr) {
            env->CallVoidMethod(controllerObj, stopMethod);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }
    } else if (command == "restart") {
        jmethodID restartMethod = env->GetMethodID(appControllerClass, "restartApplication", "()V");
        if (restartMethod != nullptr) {
            env->CallVoidMethod(controllerObj, restartMethod);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }
    } else if (command == "setLogLevel") {
        if (!params.empty()) {
            jmethodID setLogLevelMethod = env->GetMethodID(appControllerClass, "setLogLevel", 
                                                          "(Ljava/lang/String;)V");
            if (setLogLevelMethod != nullptr) {
                jstring level = env->NewStringUTF(params[0].c_str());
                env->CallVoidMethod(controllerObj, setLogLevelMethod, level);
                env->DeleteLocalRef(level);
                if (env->ExceptionCheck()) {
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                }
            }
        }
    } else if (command == "maintenance") {
        jmethodID maintenanceMethod = env->GetMethodID(appControllerClass, "performMaintenance", "()V");
        if (maintenanceMethod != nullptr) {
            env->CallVoidMethod(controllerObj, maintenanceMethod);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }
    }
    
    // Clean up references
    env->DeleteLocalRef(appControllerClass);
    env->DeleteLocalRef(controllerObj);
    env->DeleteLocalRef(registryClass);
    
    return true;
}

// Function to call back to Java controller
void callJavaCallback(JNIEnv* env, jobject controllerObj, const std::string& command, 
                     const std::vector<std::string>& params) {
    jclass controllerClass = env->GetObjectClass(controllerObj);
    if (controllerClass == nullptr) {
        return;
    }
    
    // Convert parameters to Java string array
    jsize paramCount = static_cast<jsize>(params.size());
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray paramArray = env->NewObjectArray(paramCount, stringClass, nullptr);
    
    for (jsize i = 0; i < paramCount; i++) {
        jstring jParam = env->NewStringUTF(params[i].c_str());
        env->SetObjectArrayElement(paramArray, i, jParam);
        env->DeleteLocalRef(jParam);
    }
    
    // Find and call the onNativeCommand method
    jmethodID callbackMethod = env->GetMethodID(controllerClass, "onNativeCommand", 
                                              "(Ljava/lang/String;[Ljava/lang/String;)V");
    if (callbackMethod != nullptr) {
        jstring jCommand = env->NewStringUTF(command.c_str());
        env->CallVoidMethod(controllerObj, callbackMethod, jCommand, paramArray);
        env->DeleteLocalRef(jCommand);
        
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }
    
    // Clean up
    env->DeleteLocalRef(paramArray);
    env->DeleteLocalRef(stringClass);
    env->DeleteLocalRef(controllerClass);
}

// JNI OnLoad function
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

// JNI function implementations
extern "C" JNIEXPORT void JNICALL Java_com_app_NativeController_initializeSystem
(JNIEnv* env, jobject obj) {
    
    // Store global reference to the controller object
    if (g_globalControllerRef == nullptr) {
        g_globalControllerRef = env->NewGlobalRef(obj);
    }
    
    // Initialize the system
    std::vector<std::string> params = {"INITIALIZING"};
    controlJavaComponents(env, "start", params);
    
    // Call back to Java
    callJavaCallback(env, obj, "initialize", params);
}

extern "C" JNIEXPORT void JNICALL Java_com_app_NativeController_sendControlCommand
(JNIEnv* env, jobject obj, jstring command) {
    
    if (command == nullptr) {
        return;
    }
    
    const char* cmdStr = env->GetStringUTFChars(command, nullptr);
    if (cmdStr == nullptr) {
        return;
    }
    
    std::string cmd(cmdStr);
    env->ReleaseStringUTFChars(command, cmdStr);
    
    // Parse command and execute
    std::vector<std::string> params;
    controlJavaComponents(env, cmd, params);
    
    // Call back to Java
    callJavaCallback(env, obj, "control_command", params);
}

extern "C" JNIEXPORT void JNICALL Java_com_app_NativeController_shutdownSystem
(JNIEnv* env, jobject obj) {
    
    std::vector<std::string> params = {"SHUTTING_DOWN"};
    controlJavaComponents(env, "stop", params);
    
    // Call back to Java
    callJavaCallback(env, obj, "shutdown", params);
    
    // Clean up global reference
    if (g_globalControllerRef != nullptr) {
        env->DeleteGlobalRef(g_globalControllerRef);
        g_globalControllerRef = nullptr;
    }
}

extern "C" JNIEXPORT jstring JNICALL Java_com_app_NativeController_querySystemStatus
(JNIEnv* env, jobject obj) {
    
    // Find the ComponentRegistry class
    jclass registryClass = env->FindClass("com/app/controller/ComponentRegistry");
    if (registryClass == nullptr) {
        return env->NewStringUTF("Error: Could not access registry");
    }
    
    // Get the main controller component
    jmethodID getComponentMethod = env->GetStaticMethodID(registryClass, "getComponent", 
                                                         "(Ljava/lang/String;)Ljava/lang/Object;");
    jstring componentName = env->NewStringUTF("mainController");
    jobject controllerObj = env->CallStaticObjectMethod(registryClass, getComponentMethod, componentName);
    env->DeleteLocalRef(componentName);
    
    if (controllerObj == nullptr) {
        env->DeleteLocalRef(registryClass);
        return env->NewStringUTF("Error: Controller not found");
    }
    
    // Get the getStatusInfo method
    jclass appControllerClass = env->GetObjectClass(controllerObj);
    jmethodID getStatusMethod = env->GetMethodID(appControllerClass, "getStatusInfo", "()Ljava/lang/String;");
    
    jstring status = nullptr;
    if (getStatusMethod != nullptr) {
        status = (jstring)env->CallObjectMethod(controllerObj, getStatusMethod);
    }
    
    // Clean up
    env->DeleteLocalRef(appControllerClass);
    env->DeleteLocalRef(controllerObj);
    env->DeleteLocalRef(registryClass);
    
    if (status != nullptr) {
        return status;
    } else {
        return env->NewStringUTF("Unknown status");
    }
}