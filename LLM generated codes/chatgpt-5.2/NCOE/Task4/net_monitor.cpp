#include <jni.h>
#include <chrono>

/*
 * Class:     com_example_net_NetworkPacket
 * Method:    recordTimestamp
 * Signature: (Lcom/example/net/NetworkPacket;)V
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_example_net_NetworkPacket_recordTimestamp(
        JNIEnv* env,
        jclass,
        jobject packetObj
) {
    if (packetObj == nullptr) {
        return;
    }

    jclass packetClass = env->GetObjectClass(packetObj);
    if (packetClass == nullptr) {
        return;
    }

    // Look up the timestamp field
    jfieldID timestampField = env->GetFieldID(
        packetClass,
        "timestamp",
        "J"   // long
    );

    if (timestampField == nullptr) {
        env->DeleteLocalRef(packetClass);
        return;
    }

    // Get current system time in milliseconds
    auto now = std::chrono::system_clock::now();
    auto millis = std::chrono::duration_cast<std::chrono::milliseconds>(
        now.time_since_epoch()
    ).count();

    // Write timestamp into Java object
    env->SetLongField(
        packetObj,
        timestampField,
        static_cast<jlong>(millis)
    );

    env->DeleteLocalRef(packetClass);
}
