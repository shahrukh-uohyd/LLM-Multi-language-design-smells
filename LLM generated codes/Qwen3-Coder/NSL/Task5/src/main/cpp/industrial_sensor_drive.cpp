// File: src/main/cpp/industrial_sensor_driver.cpp
#include <jni.h>
#include <string>
#include <vector>
#include <random>
#include <ctime>
#include <iostream>
#include <thread>
#include <chrono>

// Global variables to simulate sensor state
static bool sensorInitialized = false;
static bool sensorConnected = false;
static std::string lastDriverError = "";

// Helper function to create a Java int array
jintArray createJavaIntArray(JNIEnv *env, const std::vector<int>& data) {
    jsize len = data.size();
    jintArray result = env->NewIntArray(len);
    if (result == nullptr) {
        return nullptr; // OutOfMemoryError thrown
    }
    
    env->SetIntArrayRegion(result, 0, len, data.data());
    return result;
}

// Helper function to create a Java String
jstring createJavaString(JNIEnv *env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

extern "C" {

JNIEXPORT jintArray JNICALL Java_com_example_sensor_IndustrialSensorDriver_readRawData
(JNIEnv *env, jobject obj) {
    if (!sensorConnected) {
        lastDriverError = "Sensor not connected";
        return nullptr;
    }
    
    // Simulate reading raw sensor data
    // In a real implementation, this would interface with actual hardware
    std::vector<int> sensorData;
    
    // Simulate 8 sensor readings (common for industrial sensors)
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 10000); // Values between 0 and 10000
    
    for (int i = 0; i < 8; i++) {
        int value = dis(gen);
        // Add some variation to make it look like real sensor data
        value += i * 100; // Simulate channel differences
        sensorData.push_back(value);
    }
    
    return createJavaIntArray(env, sensorData);
}

JNIEXPORT jstring JNICALL Java_com_example_sensor_IndustrialSensorDriver_getDriverVersion
(JNIEnv *env, jobject obj) {
    return createJavaString(env, "Sensor-IO-X64-Driver v2.3.1");
}

JNIEXPORT jboolean JNICALL Java_com_example_sensor_IndustrialSensorDriver_isSensorConnected
(JNIEnv *env, jobject obj) {
    // Simulate checking sensor connection
    // In a real implementation, this would check actual hardware connection
    if (!sensorInitialized) {
        return JNI_FALSE;
    }
    
    // Simulate occasional connection drops for demonstration
    static int connectionCounter = 0;
    connectionCounter++;
    
    // 95% chance of being connected
    return (connectionCounter % 20 != 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_example_sensor_IndustrialSensorDriver_initializeDriver
(JNIEnv *env, jobject obj) {
    // Simulate driver initialization
    // In a real implementation, this would set up hardware connections
    std::cout << "Initializing industrial sensor driver..." << std::endl;
    
    // Simulate initialization time
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    
    sensorInitialized = true;
    sensorConnected = true;
    lastDriverError = "";
    
    std::cout << "Industrial sensor driver initialized successfully" << std::endl;
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_example_sensor_IndustrialSensorDriver_cleanup
(JNIEnv *env, jobject obj) {
    // Simulate cleanup operations
    std::cout << "Cleaning up industrial sensor driver resources..." << std::endl;
    
    sensorInitialized = false;
    sensorConnected = false;
    lastDriverError = "";
    
    std::cout << "Industrial sensor driver cleanup completed" << std::endl;
}

JNIEXPORT jstring JNICALL Java_com_example_sensor_IndustrialSensorDriver_getLastDriverError
(JNIEnv *env, jobject obj) {
    return createJavaString(env, lastDriverError);
}

} // extern "C"