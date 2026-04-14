#include <jni.h>

JNIEXPORT jbooleanArray JNICALL
Java_com_example_sensors_NativeSensorInspector_inspect(
        JNIEnv* env,
        jclass,
        jobjectArray readings,
        jdoubleArray thresholds) {

    if (readings == nullptr || thresholds == nullptr) {
        return nullptr;
    }

    jsize count = env->GetArrayLength(readings);
    jsize thresholdCount = env->GetArrayLength(thresholds);

    // Allocate result array
    jbooleanArray result = env->NewBooleanArray(count);
    if (result == nullptr) {
        return nullptr;
    }

    jboolean* flags =
        env->GetBooleanArrayElements(result, nullptr);
    jdouble* thresholdVals =
        env->GetDoubleArrayElements(thresholds, nullptr);

    // Lookup class and field IDs once
    jclass readingClass =
        env->FindClass("com/example/sensors/SensorReading");
    if (readingClass == nullptr) goto cleanup;

    jfieldID sensorIdField =
        env->GetFieldID(readingClass, "sensorId", "I");
    jfieldID valueField =
        env->GetFieldID(readingClass, "value", "D");

    if (sensorIdField == nullptr || valueField == nullptr)
        goto cleanup;

    for (jsize i = 0; i < count; i++) {
        jobject reading =
            env->GetObjectArrayElement(readings, i);

        flags[i] = JNI_FALSE;

        if (reading != nullptr) {
            jint sensorId =
                env->GetIntField(reading, sensorIdField);
            jdouble value =
                env->GetDoubleField(reading, valueField);

            if (sensorId >= 0 &&
                sensorId < thresholdCount &&
                value > thresholdVals[sensorId]) {

                flags[i] = JNI_TRUE;
            }

            env->DeleteLocalRef(reading);
        }
    }

cleanup:
    env->ReleaseBooleanArrayElements(result, flags, 0);
    env->ReleaseDoubleArrayElements(thresholds, thresholdVals, JNI_ABORT);

    return result;
}
