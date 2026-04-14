#include <jni.h>
#include <iostream>
#include <thread>
#include <chrono>
#include <vector>
#include <string>
#include <map>

// Global JVM reference
static JavaVM* g_jvm = nullptr;
static jobject g_hostObjectRef = nullptr;

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

// Helper function to convert C++ vector to Java string array
jobjectArray convertVectorToStringArray(JNIEnv* env, const std::vector<std::string>& vec) {
    jsize len = static_cast<jsize>(vec.size());
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(len, stringClass, nullptr);
    
    for (jsize i = 0; i < len; i++) {
        jstring str = env->NewStringUTF(vec[i].c_str());
        env->SetObjectArrayElement(array, i, str);
        env->DeleteLocalRef(str);
    }
    
    env->DeleteLocalRef(stringClass);
    return array;
}

// Function to find and execute plugins
bool executePluginOperation(JNIEnv* env, const std::string& pluginName, 
                           const std::string& operation, 
                           const std::vector<std::string>& params = {}) {
    // Find the PluginManager class
    jclass pluginManagerClass = env->FindClass("com/plugin/api/PluginManager");
    if (pluginManagerClass == nullptr) {
        std::cerr << "Could not find PluginManager class" << std::endl;
        env->ExceptionDescribe();
        env->ExceptionClear();
        return false;
    }
    
    // Find the getPlugin method
    jmethodID getPluginMethod = env->GetStaticMethodID(pluginManagerClass, "getPlugin", 
                                                      "(Ljava/lang/String;)Lcom/plugin/api/PluginInterface;");
    if (getPluginMethod == nullptr) {
        std::cerr << "Could not find getPlugin method" << std::endl;
        env->DeleteLocalRef(pluginManagerClass);
        return false;
    }
    
    // Get the plugin object
    jstring jPluginName = env->NewStringUTF(pluginName.c_str());
    jobject pluginObj = env->CallStaticObjectMethod(pluginManagerClass, getPluginMethod, jPluginName);
    env->DeleteLocalRef(jPluginName);
    
    if (pluginObj == nullptr) {
        std::cerr << "Plugin '" << pluginName << "' not found" << std::endl;
        env->DeleteLocalRef(pluginManagerClass);
        return false;
    }
    
    // Get the PluginInterface class
    jclass pluginClass = env->GetObjectClass(pluginObj);
    if (pluginClass == nullptr) {
        std::cerr << "Could not get PluginInterface class" << std::endl;
        env->DeleteLocalRef(pluginObj);
        env->DeleteLocalRef(pluginManagerClass);
        return false;
    }
    
    bool success = true;
    
    // Execute the requested operation
    if (operation == "execute") {
        jmethodID executeMethod = env->GetMethodID(pluginClass, "execute", "()V");
        if (executeMethod != nullptr) {
            env->CallVoidMethod(pluginObj, executeMethod);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                success = false;
            }
        }
    } else if (operation == "executeWithParams") {
        if (!params.empty()) {
            jobjectArray paramArray = convertVectorToStringArray(env, params);
            jmethodID executeWithParamsMethod = env->GetMethodID(pluginClass, "executeWithParams", 
                                                               "([Ljava/lang/String;)V");
            if (executeWithParamsMethod != nullptr) {
                env->CallVoidMethod(pluginObj, executeWithParamsMethod, paramArray);
                if (env->ExceptionCheck()) {
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                    success = false;
                }
            }
            env->DeleteLocalRef(paramArray);
        }
    } else if (operation == "setEnabled") {
        if (!params.empty()) {
            bool enabled = params[0] == "true";
            jmethodID setEnabledMethod = env->GetMethodID(pluginClass, "setEnabled", "(Z)V");
            if (setEnabledMethod != nullptr) {
                env->CallVoidMethod(pluginObj, setEnabledMethod, enabled ? JNI_TRUE : JNI_FALSE);
                if (env->ExceptionCheck()) {
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                    success = false;
                }
            }
        }
    }
    
    // Clean up references
    env->DeleteLocalRef(pluginClass);
    env->DeleteLocalRef(pluginObj);
    env->DeleteLocalRef(pluginManagerClass);
    
    return success;
}

// Function to get plugin information
std::string getPluginInfo(JNIEnv* env, const std::string& pluginName) {
    // Find the PluginManager class
    jclass pluginManagerClass = env->FindClass("com/plugin/api/PluginManager");
    if (pluginManagerClass == nullptr) {
        return "Error: PluginManager class not found";
    }
    
    // Get the plugin object
    jmethodID getPluginMethod = env->GetStaticMethodID(pluginManagerClass, "getPlugin", 
                                                      "(Ljava/lang/String;)Lcom/plugin/api/PluginInterface;");
    jstring jPluginName = env->NewStringUTF(pluginName.c_str());
    jobject pluginObj = env->CallStaticObjectMethod(pluginManagerClass, getPluginMethod, jPluginName);
    env->DeleteLocalRef(jPluginName);
    
    if (pluginObj == nullptr) {
        env->DeleteLocalRef(pluginManagerClass);
        return "Plugin '" + pluginName + "' not found";
    }
    
    // Get plugin info methods
    jclass pluginClass = env->GetObjectClass(pluginObj);
    
    std::string info = "Plugin Info:\n";
    
    // Get name
    jmethodID getNameMethod = env->GetMethodID(pluginClass, "getName", "()Ljava/lang/String;");
    if (getNameMethod != nullptr) {
        jstring name = (jstring)env->CallObjectMethod(pluginObj, getNameMethod);
        if (name != nullptr) {
            const char* nameStr = env->GetStringUTFChars(name, nullptr);
            info += "  Name: " + std::string(nameStr) + "\n";
            env->ReleaseStringUTFChars(name, nameStr);
            env->DeleteLocalRef(name);
        }
    }
    
    // Get version
    jmethodID getVersionMethod = env->GetMethodID(pluginClass, "getVersion", "()Ljava/lang/String;");
    if (getVersionMethod != nullptr) {
        jstring version = (jstring)env->CallObjectMethod(pluginObj, getVersionMethod);
        if (version != nullptr) {
            const char* versionStr = env->GetStringUTFChars(version, nullptr);
            info += "  Version: " + std::string(versionStr) + "\n";
            env->ReleaseStringUTFChars(version, versionStr);
            env->DeleteLocalRef(version);
        }
    }
    
    // Get description
    jmethodID getDescriptionMethod = env->GetMethodID(pluginClass, "getDescription", "()Ljava/lang/String;");
    if (getDescriptionMethod != nullptr) {
        jstring description = (jstring)env->CallObjectMethod(pluginObj, getDescriptionMethod);
        if (description != nullptr) {
            const char* descStr = env->GetStringUTFChars(description, nullptr);
            info += "  Description: " + std::string(descStr) + "\n";
            env->ReleaseStringUTFChars(description, descStr);
            env->DeleteLocalRef(description);
        }
    }
    
    // Get enabled status
    jmethodID isEnabledMethod = env->GetMethodID(pluginClass, "isEnabled", "()Z");
    if (isEnabledMethod != nullptr) {
        jboolean enabled = env->CallBooleanMethod(pluginObj, isEnabledMethod);
        info += "  Enabled: " + std::string(enabled ? "Yes" : "No") + "\n";
    }
    
    // Clean up
    env->DeleteLocalRef(pluginClass);
    env->DeleteLocalRef(pluginObj);
    env->DeleteLocalRef(pluginManagerClass);
    
    return info;
}

// Function to call Java callbacks
void callJavaCallback(JNIEnv* env, const std::string& methodName, 
                     const std::string& param1, const std::string& param2 = "") {
    if (g_hostObjectRef == nullptr) {
        return;
    }
    
    jclass hostClass = env->GetObjectClass(g_hostObjectRef);
    if (hostClass == nullptr) {
        return;
    }
    
    std::string signature = "(Ljava/lang/String;";
    if (!param2.empty()) {
        signature += "Ljava/lang/String;";
    }
    signature += ")V";
    
    jmethodID callbackMethod = env->GetMethodID(hostClass, methodName.c_str(), signature.c_str());
    if (callbackMethod != nullptr) {
        jstring jParam1 = env->NewStringUTF(param1.c_str());
        if (!param2.empty()) {
            jstring jParam2 = env->NewStringUTF(param2.c_str());
            env->CallVoidMethod(g_hostObjectRef, callbackMethod, jParam1, jParam2);
            env->DeleteLocalRef(jParam2);
        } else {
            env->CallVoidMethod(g_hostObjectRef, callbackMethod, jParam1);
        }
        env->DeleteLocalRef(jParam1);
        
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }
    
    env->DeleteLocalRef(hostClass);
}

// JNI OnLoad function
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

// JNI function implementations
extern "C" JNIEXPORT jboolean JNICALL Java_com_plugin_NativePluginHost_loadPlugin
(JNIEnv* env, jobject obj, jstring pluginName, jstring className) {
    
    // Store the host object reference
    if (g_hostObjectRef == nullptr) {
        g_hostObjectRef = env->NewGlobalRef(obj);
    }
    
    const char* pluginNameStr = env->GetStringUTFChars(pluginName, nullptr);
    const char* classNameStr = env->GetStringUTFChars(className, nullptr);
    
    if (pluginNameStr == nullptr || classNameStr == nullptr) {
        if (pluginNameStr) env->ReleaseStringUTFChars(pluginName, pluginNameStr);
        if (classNameStr) env->ReleaseStringUTFChars(className, classNameStr);
        return JNI_FALSE;
    }
    
    // In a real implementation, you would dynamically load the class
    // For this example, we'll assume plugins are already registered
    
    bool success = true; // Assume success for this demo
    
    // Call back to Java
    callJavaCallback(env, "onPluginLoaded", pluginNameStr, success ? "true" : "false");
    
    env->ReleaseStringUTFChars(pluginName, pluginNameStr);
    env->ReleaseStringUTFChars(className, classNameStr);
    
    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_plugin_NativePluginHost_executePlugin
(JNIEnv* env, jobject obj, jstring pluginName) {
    
    const char* pluginNameStr = env->GetStringUTFChars(pluginName, nullptr);
    if (pluginNameStr == nullptr) {
        return JNI_FALSE;
    }
    
    bool success = executePluginOperation(env, pluginNameStr, "execute");
    
    // Call back to Java
    callJavaCallback(env, "onPluginExecuted", pluginNameStr, success ? "Success" : "Failed");
    
    env->ReleaseStringUTFChars(pluginName, pluginNameStr);
    
    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_plugin_NativePluginHost_executePluginWithParams
(JNIEnv* env, jobject obj, jstring pluginName, jobjectArray parameters) {
    
    const char* pluginNameStr = env->GetStringUTFChars(pluginName, nullptr);
    if (pluginNameStr == nullptr) {
        return JNI_FALSE;
    }
    
    std::vector<std::string> params = convertStringArrayToVector(env, parameters);
    bool success = executePluginOperation(env, pluginNameStr, "executeWithParams", params);
    
    // Call back to Java
    callJavaCallback(env, "onPluginExecuted", pluginNameStr, success ? "Success" : "Failed");
    
    env->ReleaseStringUTFChars(pluginName, pluginNameStr);
    
    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL Java_com_plugin_NativePluginHost_queryPluginInfo
(JNIEnv* env, jobject obj, jstring pluginName) {
    
    const char* pluginNameStr = env->GetStringUTFChars(pluginName, nullptr);
    if (pluginNameStr == nullptr) {
        return env->NewStringUTF("Error: Invalid plugin name");
    }
    
    std::string info = getPluginInfo(env, pluginNameStr);
    
    env->ReleaseStringUTFChars(pluginName, pluginNameStr);
    
    return env->NewStringUTF(info.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_plugin_NativePluginHost_setPluginState
(JNIEnv* env, jobject obj, jstring pluginName, jboolean enabled) {
    
    const char* pluginNameStr = env->GetStringUTFChars(pluginName, nullptr);
    if (pluginNameStr == nullptr) {
        return JNI_FALSE;
    }
    
    std::vector<std::string> params = {enabled ? "true" : "false"};
    bool success = executePluginOperation(env, pluginNameStr, "setEnabled", params);
    
    env->ReleaseStringUTFChars(pluginName, pluginNameStr);
    
    return success ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL Java_com_plugin_NativePluginHost_processPluginInput
(JNIEnv* env, jobject obj, jstring pluginName, jstring input) {
    
    const char* pluginNameStr = env->GetStringUTFChars(pluginName, nullptr);
    const char* inputStr = env->GetStringUTFChars(input, nullptr);
    
    if (pluginNameStr == nullptr || inputStr == nullptr) {
        if (pluginNameStr) env->ReleaseStringUTFChars(pluginName, pluginNameStr);
        if (inputStr) env->ReleaseStringUTFChars(input, inputStr);
        return env->NewStringUTF("");
    }
    
    // Find and call the processInput method on the plugin
    jclass pluginManagerClass = env->FindClass("com/plugin/api/PluginManager");
    jmethodID getPluginMethod = env->GetStaticMethodID(pluginManagerClass, "getPlugin", 
                                                      "(Ljava/lang/String;)Lcom/plugin/api/PluginInterface;");
    jstring jPluginName = env->NewStringUTF(pluginNameStr);
    jobject pluginObj = env->CallStaticObjectMethod(pluginManagerClass, getPluginMethod, jPluginName);
    env->DeleteLocalRef(jPluginName);
    
    jstring result = nullptr;
    if (pluginObj != nullptr) {
        jclass pluginClass = env->GetObjectClass(pluginObj);
        jmethodID processInputMethod = env->GetMethodID(pluginClass, "processInput", "(Ljava/lang/String;)Ljava/lang/String;");
        
        if (processInputMethod != nullptr) {
            jstring jInput = env->NewStringUTF(inputStr);
            result = (jstring)env->CallObjectMethod(pluginObj, processInputMethod, jInput);
            env->DeleteLocalRef(jInput);
        }
        
        env->DeleteLocalRef(pluginClass);
        env->DeleteLocalRef(pluginObj);
    }
    
    env->DeleteLocalRef(pluginManagerClass);
    
    if (result == nullptr) {
        result = env->NewStringUTF("");
    }
    
    env->ReleaseStringUTFChars(pluginName, pluginNameStr);
    env->ReleaseStringUTFChars(input, inputStr);
    
    return result;
}

extern "C" JNIEXPORT jobjectArray JNICALL Java_com_plugin_NativePluginHost_listAvailablePlugins
(JNIEnv* env, jobject obj) {
    
    // Find the PluginManager class
    jclass pluginManagerClass = env->FindClass("com/plugin/api/PluginManager");
    if (pluginManagerClass == nullptr) {
        return nullptr;
    }
    
    // Get the getAvailablePlugins method
    jmethodID getAvailablePluginsMethod = env->GetStaticMethodID(pluginManagerClass, "getAvailablePlugins", 
                                                                "()Ljava/util/Set;");
    jobject pluginSet = env->CallStaticObjectMethod(pluginManagerClass, getAvailablePluginsMethod);
    if (pluginSet == nullptr) {
        env->DeleteLocalRef(pluginManagerClass);
        return nullptr;
    }
    
    // Convert Set to Array
    jclass setClass = env->FindClass("java/util/Set");
    jmethodID toArrayMethod = env->GetMethodID(setClass, "toArray", "()[Ljava/lang/Object;");
    jobjectArray objectArray = (jobjectArray)env->CallObjectMethod(pluginSet, toArrayMethod);
    
    // Cast to String array
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray stringArray = env->NewObjectArray(
        env->GetArrayLength(objectArray), 
        stringClass, 
        env->NewStringUTF("")
    );
    
    for (int i = 0; i < env->GetArrayLength(objectArray); i++) {
        jstring pluginName = (jstring)env->GetObjectArrayElement(objectArray, i);
        env->SetObjectArrayElement(stringArray, i, pluginName);
        env->DeleteLocalRef(pluginName);
    }
    
    // Clean up
    env->DeleteLocalRef(objectArray);
    env->DeleteLocalRef(setClass);
    env->DeleteLocalRef(pluginSet);
    env->DeleteLocalRef(pluginManagerClass);
    
    return stringArray;
}

extern "C" JNIEXPORT void JNICALL Java_com_plugin_NativePluginHost_shutdown
(JNIEnv* env, jobject obj) {
    
    // Clean up global reference
    if (g_hostObjectRef != nullptr) {
        env->DeleteGlobalRef(g_hostObjectRef);
        g_hostObjectRef = nullptr;
    }
}