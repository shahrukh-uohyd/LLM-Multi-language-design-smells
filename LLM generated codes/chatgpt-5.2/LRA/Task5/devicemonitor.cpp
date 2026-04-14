#include <jni.h>
#include <unordered_map>
#include <vector>

JNIEXPORT jintArray JNICALL
Java_com_example_monitoring_NativeDeviceMonitor_findDevicesWithRepeatedFailures(
        JNIEnv* env,
        jclass,
        jobjectArray updates,
        jint failureCode,
        jint minFailures) {

    if (updates == nullptr || minFailures <= 0) {
        return nullptr;
    }

    jsize length = env->GetArrayLength(updates);

    // Look up class and field IDs once
    jclass updateClass =
        env->FindClass("com/example/monitoring/DeviceStatusUpdate");
    if (updateClass == nullptr) {
        return nullptr;
    }

    jfieldID deviceIdField =
        env->GetFieldID(updateClass, "deviceId", "I");
    jfieldID statusCodeField =
        env->GetFieldID(updateClass, "statusCode", "I");

    if (deviceIdField == nullptr || statusCodeField == nullptr) {
        return nullptr;
    }

    std::unordered_map<int, int> failureCounts;

    // Count failures per device
    for (jsize i = 0; i < length; i++) {
        jobject update =
            env->GetObjectArrayElement(updates, i);
        if (update == nullptr) {
            continue;
        }

        jint status =
            env->GetIntField(update, statusCodeField);

        if (status == failureCode) {
            jint deviceId =
                env->GetIntField(update, deviceIdField);
            failureCounts[deviceId]++;
        }

        env->DeleteLocalRef(update);
    }

    // Collect devices exceeding threshold
    std::vector<jint> failingDevices;
    for (const auto& entry : failureCounts) {
        if (entry.second >= minFailures) {
            failingDevices.push_back(entry.first);
        }
    }

    // Convert to Java int[]
    jintArray result =
        env->NewIntArray((jsize)failingDevices.size());
    if (result == nullptr) {
        return nullptr;
    }

    env->SetIntArrayRegion(
        result, 0,
        (jsize)failingDevices.size(),
        failingDevices.data());

    return result;
}
