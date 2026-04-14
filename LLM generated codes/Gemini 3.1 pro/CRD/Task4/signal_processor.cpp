#include <jni.h>
#include <vector>

// Note: In production, include an optimized math/DSP library like FFTW or Eigen here.

extern "C" {

JNIEXPORT jfloatArray JNICALL Java_SignalProcessor_applyLowPassFilter(JNIEnv *env, jobject thisObj, jfloatArray rawSignal, jfloat smoothingFactor) {
    // 1. Retrieve the length and C++ pointer to the Java array
    jsize length = env->GetArrayLength(rawSignal);
    jfloat *signalElements = env->GetFloatArrayElements(rawSignal, nullptr);

    // 2. Allocate a new float array in the JVM for the processed result
    jfloatArray processedSignal = env->NewFloatArray(length);

    if (processedSignal != nullptr && length > 0) {
        // Use std::vector to safely manage the temporary C++ buffer
        std::vector<jfloat> buffer(length);
        
        /* 
         * ========================================================
         * [Perform ACTUAL signal processing logic here]
         * Here, we implement a simple Exponential Moving Average (EMA)
         * as a mock low-pass filter.
         * ========================================================
         */
        buffer[0] = signalElements[0]; // Initialize first value
        
        for (int i = 1; i < length; ++i) {
            buffer[i] = buffer[i - 1] + smoothingFactor * (signalElements[i] - buffer[i - 1]);
        }

        // 3. Copy the processed C++ buffer back into the JVM's result array
        env->SetFloatArrayRegion(processedSignal, 0, length, buffer.data());
    }

    // 4. Release the input array elements
    // JNI_ABORT is used to tell the JVM we only read the data and didn't modify the original Java array.
    env->ReleaseFloatArrayElements(rawSignal, signalElements, JNI_ABORT);

    // 5. Return the resulting array to Java
    return processedSignal;
}

JNIEXPORT jfloatArray JNICALL Java_SignalProcessor_computeFFT(JNIEnv *env, jobject thisObj, jfloatArray timeDomainSignal) {
    jsize length = env->GetArrayLength(timeDomainSignal);
    jfloat *signalElements = env->GetFloatArrayElements(timeDomainSignal, nullptr);

    /* 
     * ========================================================
     * [Perform ACTUAL FFT logic here using a library like FFTW]
     * For demonstration, we just mock the return data.
     * ========================================================
     */
    
    jsize fftLength = length / 2; // Real FFT results are usually half size
    jfloatArray frequencySpectrum = env->NewFloatArray(fftLength);

    if (frequencySpectrum != nullptr) {
        std::vector<jfloat> mockBuffer(fftLength, 0.0f); // Mock empty frequency data
        env->SetFloatArrayRegion(frequencySpectrum, 0, fftLength, mockBuffer.data());
    }

    env->ReleaseFloatArrayElements(timeDomainSignal, signalElements, JNI_ABORT);
    
    return frequencySpectrum;
}

} // end extern "C"