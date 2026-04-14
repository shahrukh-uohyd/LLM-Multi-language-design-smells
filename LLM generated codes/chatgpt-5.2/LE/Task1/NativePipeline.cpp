#include <jni.h>
#include <vector>
#include <string>
#include <sstream>
#include <numeric>
#include <algorithm>
#include "NativePipeline.h"

/*
 * double[] nativeParse(String rawInput)
 */
JNIEXPORT jdoubleArray JNICALL
Java_NativePipeline_nativeParse(JNIEnv* env,
                                jobject,
                                jstring rawInput) {

    const char* chars = env->GetStringUTFChars(rawInput, nullptr);
    std::string input(chars);
    env->ReleaseStringUTFChars(rawInput, chars);

    std::vector<double> values;
    std::stringstream ss(input);
    std::string token;

    while (std::getline(ss, token, ',')) {
        values.push_back(std::stod(token));
    }

    jdoubleArray result = env->NewDoubleArray(values.size());
    env->SetDoubleArrayRegion(result, 0, values.size(), values.data());
    return result;
}

/*
 * double[] nativeNormalize(double[] parsedData)
 */
JNIEXPORT jdoubleArray JNICALL
Java_NativePipeline_nativeNormalize(JNIEnv* env,
                                    jobject,
                                    jdoubleArray parsedData) {

    jsize len = env->GetArrayLength(parsedData);
    jdouble* data = env->GetDoubleArrayElements(parsedData, nullptr);

    double maxVal = 1.0;
    for (jsize i = 0; i < len; i++) {
        if (data[i] > maxVal) maxVal = data[i];
    }

    std::vector<double> normalized(len);
    for (jsize i = 0; i < len; i++) {
        normalized[i] = data[i] / maxVal;
    }

    env->ReleaseDoubleArrayElements(parsedData, data, JNI_ABORT);

    jdoubleArray result = env->NewDoubleArray(len);
    env->SetDoubleArrayRegion(result, 0, len, normalized.data());
    return result;
}

/*
 * double nativeCompute(double[] normalizedData)
 */
JNIEXPORT jdouble JNICALL
Java_NativePipeline_nativeCompute(JNIEnv* env,
                                  jobject,
                                  jdoubleArray normalizedData) {

    jsize len = env->GetArrayLength(normalizedData);
    jdouble* data = env->GetDoubleArrayElements(normalizedData, nullptr);

    double sum = 0.0;
    for (jsize i = 0; i < len; i++) {
        sum += data[i];
    }

    env->ReleaseDoubleArrayElements(normalizedData, data, JNI_ABORT);
    return sum;
}
