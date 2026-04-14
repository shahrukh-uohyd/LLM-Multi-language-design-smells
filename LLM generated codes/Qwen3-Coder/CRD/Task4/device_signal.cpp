// device_signal.cpp
#include "device_signal.h"
#include <iostream>
#include <fstream>
#include <vector>
#include <cmath>
#include <map>
#include <mutex>
#include <complex>
#include <algorithm>
#include <numeric>

// Helper function to convert jstring to std::string
std::string jstringToString(JNIEnv* env, jstring jstr) {
    const char* cstr = env->GetStringUTFChars(jstr, nullptr);
    std::string str(cstr);
    env->ReleaseStringUTFChars(jstr, cstr);
    return str;
}

// Helper function to convert jdoubleArray to std::vector<double>
std::vector<double> jdoubleArrayToVector(JNIEnv* env, jdoubleArray array) {
    jsize length = env->GetArrayLength(array);
    std::vector<double> vec(length);
    
    jdouble* elements = env->GetDoubleArrayElements(array, nullptr);
    std::copy(elements, elements + length, vec.begin());
    env->ReleaseDoubleArrayElements(array, elements, JNI_ABORT);
    
    return vec;
}

// Helper function to convert std::vector<double> to jdoubleArray
jdoubleArray vectorToJDoubleArray(JNIEnv* env, const std::vector<double>& vec) {
    jdoubleArray result = env->NewDoubleArray(vec.size());
    env->SetDoubleArrayRegion(result, 0, vec.size(), 
                             reinterpret_cast<const jdouble*>(vec.data()));
    return result;
}

// Device control implementations
JNIEXPORT jboolean JNICALL Java_DeviceController_connectToDevice(JNIEnv *env, jobject obj, jstring deviceId) {
    std::string id = jstringToString(env, deviceId);
    
    // Simulate device connection - in real implementation, this would communicate with actual hardware
    static std::map<std::string, bool> connections;
    static std::mutex connection_mutex;
    
    std::lock_guard<std::mutex> lock(connection_mutex);
    connections[id] = true;
    
    std::cout << "Connected to device: " << id << std::endl;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_DeviceController_disconnectDevice(JNIEnv *env, jobject obj, jstring deviceId) {
    std::string id = jstringToString(env, deviceId);
    
    static std::map<std::string, bool> connections;
    static std::mutex connection_mutex;
    
    std::lock_guard<std::mutex> lock(connection_mutex);
    connections.erase(id);
    
    std::cout << "Disconnected from device: " << id << std::endl;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_DeviceController_sendCommand(JNIEnv *env, jobject obj, jstring deviceId, jstring command) {
    std::string id = jstringToString(env, deviceId);
    std::string cmd = jstringToString(env, command);
    
    // Simulate sending command to device
    std::cout << "Sent command to " << id << ": " << cmd << std::endl;
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_DeviceController_readResponse(JNIEnv *env, jobject obj, jstring deviceId) {
    std::string id = jstringToString(env, deviceId);
    
    // Simulate reading response from device
    std::string response = "ACK_" + id + "_" + std::to_string(time(nullptr));
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT jboolean JNICALL Java_DeviceController_configureDevice(JNIEnv *env, jobject obj, jstring deviceId, jstring config) {
    std::string id = jstringToString(env, deviceId);
    std::string cfg = jstringToString(env, config);
    
    // Simulate configuring device
    std::cout << "Configured device " << id << " with: " << cfg << std::endl;
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_DeviceController_isDeviceConnected(JNIEnv *env, jobject obj, jstring deviceId) {
    std::string id = jstringToString(env, deviceId);
    
    static std::map<std::string, bool> connections;
    static std::mutex connection_mutex;
    
    std::lock_guard<std::mutex> lock(connection_mutex);
    auto it = connections.find(id);
    return (it != connections.end() && it->second) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_DeviceController_getDeviceStatus(JNIEnv *env, jobject obj, jstring deviceId) {
    std::string id = jstringToString(env, deviceId);
    
    if (Java_DeviceController_isDeviceConnected(env, obj, deviceId)) {
        return env->NewStringUTF("CONNECTED");
    } else {
        return env->NewStringUTF("DISCONNECTED");
    }
}

JNIEXPORT jboolean JNICALL Java_DeviceController_resetDevice(JNIEnv *env, jobject obj, jstring deviceId) {
    std::string id = jstringToString(env, deviceId);
    
    // Simulate resetting device
    std::cout << "Reset device: " << id << std::endl;
    return JNI_TRUE;
}

// Signal processing implementations
JNIEXPORT jdoubleArray JNICALL Java_DeviceController_filterSignal(JNIEnv *env, jobject obj, jdoubleArray signal, jstring filterType, jdouble parameter) {
    std::string type = jstringToString(env, filterType);
    std::vector<double> inputSignal = jdoubleArrayToVector(env, signal);
    
    std::vector<double> outputSignal(inputSignal.size());
    
    if (type == "LOWPASS") {
        // Simple moving average filter
        int windowSize = static_cast<int>(parameter * inputSignal.size());
        if (windowSize < 1) windowSize = 1;
        
        for (size_t i = 0; i < inputSignal.size(); ++i) {
            double sum = 0.0;
            int count = 0;
            for (int j = -windowSize/2; j <= windowSize/2; ++j) {
                int idx = static_cast<int>(i) + j;
                if (idx >= 0 && idx < static_cast<int>(inputSignal.size())) {
                    sum += inputSignal[idx];
                    count++;
                }
            }
            outputSignal[i] = sum / count;
        }
    } else if (type == "HIGHPASS") {
        // High-pass filter using difference
        for (size_t i = 1; i < inputSignal.size(); ++i) {
            outputSignal[i] = inputSignal[i] - inputSignal[i-1];
        }
        if (!inputSignal.empty()) {
            outputSignal[0] = inputSignal[0];
        }
    } else if (type == "BANDPASS") {
        // Simple bandpass simulation
        for (size_t i = 0; i < inputSignal.size(); ++i) {
            outputSignal[i] = inputSignal[i] * std::sin(2.0 * M_PI * parameter * i / inputSignal.size());
        }
    }
    
    return vectorToJDoubleArray(env, outputSignal);
}

JNIEXPORT jdoubleArray JNICALL Java_DeviceController_fftTransform(JNIEnv *env, jobject obj, jdoubleArray signal) {
    std::vector<double> inputSignal = jdoubleArrayToVector(env, signal);
    
    // Pad signal to next power of 2 for efficient FFT
    size_t n = inputSignal.size();
    size_t paddedSize = 1;
    while (paddedSize < n) {
        paddedSize <<= 1;
    }
    
    std::vector<std::complex<double>> paddedSignal(paddedSize);
    for (size_t i = 0; i < n; ++i) {
        paddedSignal[i] = std::complex<double>(inputSignal[i], 0.0);
    }
    
    // Perform FFT (simplified implementation - in practice, use FFTW or similar)
    // This is a basic implementation for demonstration
    std::vector<std::complex<double>> result(paddedSize);
    
    for (size_t k = 0; k < paddedSize; ++k) {
        std::complex<double> sum(0.0, 0.0);
        for (size_t n_idx = 0; n_idx < paddedSize; ++n_idx) {
            double angle = -2.0 * M_PI * k * n_idx / paddedSize;
            std::complex<double> w(std::cos(angle), std::sin(angle));
            sum += paddedSignal[n_idx] * w;
        }
        result[k] = sum;
    }
    
    // Return magnitude of complex FFT result
    std::vector<double> magnitudes(paddedSize);
    for (size_t i = 0; i < paddedSize; ++i) {
        magnitudes[i] = std::abs(result[i]);
    }
    
    return vectorToJDoubleArray(env, magnitudes);
}

JNIEXPORT jdoubleArray JNICALL Java_DeviceController_correlateSignals(JNIEnv *env, jobject obj, jdoubleArray signal1, jdoubleArray signal2) {
    std::vector<double> sig1 = jdoubleArrayToVector(env, signal1);
    std::vector<double> sig2 = jdoubleArrayToVector(env, signal2);
    
    size_t len1 = sig1.size();
    size_t len2 = sig2.size();
    size_t resultLen = len1 + len2 - 1;
    
    std::vector<double> correlation(resultLen, 0.0);
    
    for (size_t i = 0; i < resultLen; ++i) {
        for (size_t j = 0; j < len1 && (i - j) < len2; ++j) {
            if (i >= j) {
                correlation[i] += sig1[j] * sig2[i - j];
            }
        }
    }
    
    return vectorToJDoubleArray(env, correlation);
}

JNIEXPORT jdouble JNICALL Java_DeviceController_findPeakValue(JNIEnv *env, jobject obj, jdoubleArray signal) {
    std::vector<double> inputSignal = jdoubleArrayToVector(env, signal);
    
    if (inputSignal.empty()) {
        return 0.0;
    }
    
    return *std::max_element(inputSignal.begin(), inputSignal.end());
}

JNIEXPORT jint JNICALL Java_DeviceController_findPeakIndex(JNIEnv *env, jobject obj, jdoubleArray signal) {
    std::vector<double> inputSignal = jdoubleArrayToVector(env, signal);
    
    if (inputSignal.empty()) {
        return -1;
    }
    
    auto maxElement = std::max_element(inputSignal.begin(), inputSignal.end());
    return static_cast<jint>(std::distance(inputSignal.begin(), maxElement));
}

JNIEXPORT jdoubleArray JNICALL Java_DeviceController_applyWindow(JNIEnv *env, jobject obj, jdoubleArray signal, jstring windowType) {
    std::string type = jstringToString(env, windowType);
    std::vector<double> inputSignal = jdoubleArrayToVector(env, signal);
    
    std::vector<double> windowedSignal(inputSignal.size());
    
    if (type == "HAMMING") {
        for (size_t i = 0; i < inputSignal.size(); ++i) {
            double alpha = 0.54;
            double beta = 0.46;
            double w = alpha - beta * std::cos(2.0 * M_PI * i / (inputSignal.size() - 1));
            windowedSignal[i] = inputSignal[i] * w;
        }
    } else if (type == "HANNING") {
        for (size_t i = 0; i < inputSignal.size(); ++i) {
            double w = 0.5 * (1.0 - std::cos(2.0 * M_PI * i / (inputSignal.size() - 1)));
            windowedSignal[i] = inputSignal[i] * w;
        }
    } else if (type == "BLACKMAN") {
        for (size_t i = 0; i < inputSignal.size(); ++i) {
            double alpha = 0.16;
            double a0 = (1.0 - alpha) / 2.0;
            double a1 = 0.5;
            double a2 = alpha / 2.0;
            double w = a0 - a1 * std::cos(2.0 * M_PI * i / (inputSignal.size() - 1)) + 
                      a2 * std::cos(4.0 * M_PI * i / (inputSignal.size() - 1));
            windowedSignal[i] = inputSignal[i] * w;
        }
    }
    
    return vectorToJDoubleArray(env, windowedSignal);
}

JNIEXPORT jdoubleArray JNICALL Java_DeviceController_convolveSignals(JNIEnv *env, jobject obj, jdoubleArray signal1, jdoubleArray signal2) {
    std::vector<double> sig1 = jdoubleArrayToVector(env, signal1);
    std::vector<double> sig2 = jdoubleArrayToVector(env, signal2);
    
    size_t len1 = sig1.size();
    size_t len2 = sig2.size();
    size_t resultLen = len1 + len2 - 1;
    
    std::vector<double> convolution(resultLen, 0.0);
    
    for (size_t i = 0; i < resultLen; ++i) {
        for (size_t j = 0; j <= i && j < len1 && (i - j) < len2; ++j) {
            convolution[i] += sig1[j] * sig2[i - j];
        }
    }
    
    return vectorToJDoubleArray(env, convolution);
}

JNIEXPORT jdouble JNICALL Java_DeviceController_calculateRMS(JNIEnv *env, jobject obj, jdoubleArray signal) {
    std::vector<double> inputSignal = jdoubleArrayToVector(env, signal);
    
    if (inputSignal.empty()) {
        return 0.0;
    }
    
    double sumSquares = 0.0;
    for (double value : inputSignal) {
        sumSquares += value * value;
    }
    
    return std::sqrt(sumSquares / inputSignal.size());
}

JNIEXPORT jdoubleArray JNICALL Java_DeviceController_downsampleSignal(JNIEnv *env, jobject obj, jdoubleArray signal, jint factor) {
    if (factor <= 0) {
        factor = 1;
    }
    
    std::vector<double> inputSignal = jdoubleArrayToVector(env, signal);
    std::vector<double> downsampledSignal;
    
    for (size_t i = 0; i < inputSignal.size(); i += factor) {
        downsampledSignal.push_back(inputSignal[i]);
    }
    
    return vectorToJDoubleArray(env, downsampledSignal);
}