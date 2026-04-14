#include <jni.h>
#include <float.h>

JNIEXPORT jdoubleArray JNICALL
Java_NativeDataAnalyzer_analyze(
        JNIEnv* env,
        jclass,
        jdoubleArray input) {

    if (input == NULL) {
        return NULL;
    }

    jsize length = (*env)->GetArrayLength(env, input);
    if (length == 0) {
        return NULL;
    }

    // Access numerical data directly
    jdouble* values =
        (*env)->GetDoubleArrayElements(env, input, NULL);
    if (values == NULL) {
        return NULL;
    }

    double sum = 0.0;
    double min = DBL_MAX;
    double max = -DBL_MAX;

    for (jsize i = 0; i < length; i++) {
        double v = values[i];
        sum += v;
        if (v < min) min = v;
        if (v > max) max = v;
    }

    double avg = sum / length;

    // Prepare result array: {sum, avg, min, max}
    jdouble resultVals[4];
    resultVals[0] = sum;
    resultVals[1] = avg;
    resultVals[2] = min;
    resultVals[3] = max;

    jdoubleArray result =
        (*env)->NewDoubleArray(env, 4);
    if (result != NULL) {
        (*env)->SetDoubleArrayRegion(env, result, 0, 4, resultVals);
    }

    // Release input array without modifying it
    (*env)->ReleaseDoubleArrayElements(env, input, values, JNI_ABORT);

    return result;
}
