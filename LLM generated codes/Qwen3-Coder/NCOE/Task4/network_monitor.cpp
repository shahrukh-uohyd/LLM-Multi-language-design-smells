// network_monitor.cpp
#include <jni.h>
#include <chrono>

extern "C" {

JNIEXPORT void JNICALL Java_NetworkPacket_recordTimestamp(JNIEnv *env, jobject packetObj) {
    if (packetObj == nullptr) {
        return;
    }
    
    // Get the NetworkPacket class
    jclass packetClass = env->GetObjectClass(packetObj);
    
    // Look up the timestamp field
    jfieldID timestampFieldID = env->GetFieldID(packetClass, "timestamp", "J");
    
    // Check if field exists
    if (timestampFieldID == nullptr) {
        // Field not found - throw exception
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exceptionClass, "Could not find 'timestamp' field in NetworkPacket class");
        return;
    }
    
    // Get current system time in milliseconds
    auto currentTime = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
    
    // Set the timestamp field with the current time
    env->SetLongField(packetObj, timestampFieldID, static_cast<jlong>(currentTime));
}

} // extern "C"