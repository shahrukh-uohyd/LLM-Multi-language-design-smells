#include <jni.h>
#include <chrono>
#include <iostream>

extern "C" {

JNIEXPORT void JNICALL Java_com_example_network_NetworkPacket_recordTimestamp(
    JNIEnv *env, 
    jclass clazz,      // The NetworkPacket class (passed because the method is static)
    jobject packet)    // The NetworkPacket instance passed as an argument
{
    // 1. Safety check
    if (packet == nullptr) {
        return;
    }

    // 2. Get the class of the specific packet object
    jclass packetClass = env->GetObjectClass(packet);
    if (packetClass == nullptr) {
        return;
    }

    // 3. Look up the Field ID for 'timestamp'
    // "J" is the internal JNI signature for a primitive Java long
    jfieldID timestampFieldId = env->GetFieldID(packetClass, "timestamp", "J");
    if (timestampFieldId == nullptr) {
        // Field not found, clean up and exit
        env->DeleteLocalRef(packetClass);
        return; 
    }

    // 4. Get the current system time in milliseconds using C++11 chrono
    auto now = std::chrono::system_clock::now();
    auto now_ms = std::chrono::time_point_cast<std::chrono::milliseconds>(now);
    auto epoch = now_ms.time_since_epoch();
    
    // Cast the C++ time to a JNI jlong (which maps perfectly to Java's long)
    jlong currentTimeMillis = static_cast<jlong>(epoch.count());

    // 5. Update the Java object's timestamp field directly
    env->SetLongField(packet, timestampFieldId, currentTimeMillis);

    // 6. Memory Management: Clean up local reference
    env->DeleteLocalRef(packetClass);
}

} // extern "C"