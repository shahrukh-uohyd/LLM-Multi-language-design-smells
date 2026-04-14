#include <jni.h>

// --- Mocking the Core C++ Hardware Access Library ---
// In a real application, these would interact with OS-specific APIs 
// (like /sys/class/thermal on Linux or device drivers).

double hw_read_cpu_temp() {
    // Simulated reading: 72.5 degrees Celsius
    return 72.5; 
}

long long hw_fetch_active_interrupts() {
    // Simulated reading: 65,432 interrupts
    return 65432; 
}

double hw_retrieve_power_consumption() {
    // Simulated reading: 380.0 Watts
    return 380.0; 
}
// ----------------------------------------------------

extern "C" {

// 1. JNI Wrapper for CPU Temperature
JNIEXPORT jdouble JNICALL
Java_com_example_diagnostics_SystemDiagnostics_readCpuTemperature(JNIEnv *env, jobject thiz) {
    // Fetch directly from hardware access layer
    double temp = hw_read_cpu_temp();
    return static_cast<jdouble>(temp);
}

// 2. JNI Wrapper for Active Hardware Interrupts
JNIEXPORT jlong JNICALL
Java_com_example_diagnostics_SystemDiagnostics_fetchActiveInterrupts(JNIEnv *env, jobject thiz) {
    // Fetch directly from hardware access layer
    long long interrupts = hw_fetch_active_interrupts();
    return static_cast<jlong>(interrupts);
}

// 3. JNI Wrapper for Power Consumption
JNIEXPORT jdouble JNICALL
Java_com_example_diagnostics_SystemDiagnostics_retrievePowerConsumption(JNIEnv *env, jobject thiz) {
    // Fetch directly from hardware access layer
    double power = hw_retrieve_power_consumption();
    return static_cast<jdouble>(power);
}

}