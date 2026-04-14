// File: src/main/cpp/hardware_reader.cpp
#include <jni.h>
#include <string>
#include <map>
#include <vector>
#include <thread>
#include <chrono>
#include <cstdlib>
#include <sstream>
#include <fstream>

#ifdef _WIN32
#include <windows.h>
#include <psapi.h>
#include <iphlpapi.h>
#pragma comment(lib, "IPHLPAPI.lib")
#pragma comment(lib, "psapi.lib")
#else
#include <sys/sysinfo.h>
#include <unistd.h>
#include <ifaddrs.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#endif

// Helper function to create Java String
jstring createJavaString(JNIEnv *env, const std::string& str) {
    return env->NewStringUTF(str.c_str());
}

// Helper function to create Java Long
jobject createJavaLong(JNIEnv *env, jlong value) {
    jclass longClass = env->FindClass("java/lang/Long");
    jmethodID longConstructor = env->GetMethodID(longClass, "<init>", "(J)V");
    return env->NewObject(longClass, longConstructor, value);
}

// Helper function to put a String key-value pair in a Java Map
void putStringInMap(JNIEnv *env, jobject map, const std::string& key, const std::string& value) {
    jstring jkey = createJavaString(env, key.c_str());
    jstring jvalue = createJavaString(env, value.c_str());
    jclass mapClass = env->GetObjectClass(map);
    jmethodID putMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    env->CallObjectMethod(map, putMethod, jkey, jvalue);
    env->DeleteLocalRef(jkey);
    env->DeleteLocalRef(jvalue);
}

// Helper function to put a Long key-value pair in a Java Map
void putLongInMap(JNIEnv *env, jobject map, const std::string& key, jlong value) {
    jstring jkey = createJavaString(env, key.c_str());
    jobject jvalue = createJavaLong(env, value);
    jclass mapClass = env->GetObjectClass(map);
    jmethodID putMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    env->CallObjectMethod(map, putMethod, jkey, jvalue);
    env->DeleteLocalRef(jkey);
    env->DeleteLocalRef(jvalue);
}

// Helper function to put an Integer key-value pair in a Java Map
void putIntInMap(JNIEnv *env, jobject map, const std::string& key, jint value) {
    jstring jkey = createJavaString(env, key.c_str());
    jclass integerClass = env->FindClass("java/lang/Integer");
    jmethodID integerConstructor = env->GetMethodID(integerClass, "<init>", "(I)V");
    jobject jvalue = env->NewObject(integerClass, integerConstructor, value);
    jclass mapClass = env->GetObjectClass(map);
    jmethodID putMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    env->CallObjectMethod(map, putMethod, jkey, jvalue);
    env->DeleteLocalRef(jkey);
    env->DeleteLocalRef(jvalue);
}

extern "C" {

JNIEXPORT jobject JNICALL Java_com_example_hardware_HardwareInfoReader_getCpuInfo
(JNIEnv *env, jobject obj) {
    // Create a HashMap to store CPU info
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapConstructor = env->GetMethodID(hashMapClass, "<init>", "()V");
    jobject cpuInfoMap = env->NewObject(hashMapClass, hashMapConstructor);
    
    // Simulate getting CPU info (in real implementation, this would query system)
    putStringInMap(env, cpuInfoMap, "name", "Intel(R) Core(TM) i7-8700K CPU @ 3.70GHz");
    putIntInMap(env, cpuInfoMap, "cores", 6);
    putStringInMap(env, cpuInfoMap, "architecture", "x86_64");
    putStringInMap(env, cpuInfoMap, "vendor", "GenuineIntel");
    
    return cpuInfoMap;
}

JNIEXPORT jobject JNICALL Java_com_example_hardware_HardwareInfoReader_getMemoryInfo
(JNIEnv *env, jobject obj) {
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapConstructor = env->GetMethodID(hashMapClass, "<init>", "()V");
    jobject memoryInfoMap = env->NewObject(hashMapClass, hashMapConstructor);
    
#ifdef _WIN32
    MEMORYSTATUSEX status;
    status.dwLength = sizeof(status);
    GlobalMemoryStatusEx(&status);
    
    putLongInMap(env, memoryInfoMap, "total", status.ullTotalPhys);
    putLongInMap(env, memoryInfoMap, "available", status.ullAvailPhys);
    putLongInMap(env, memoryInfoMap, "used", status.ullTotalPhys - status.ullAvailPhys);
#else
    struct sysinfo info;
    if (sysinfo(&info) == 0) {
        unsigned long long total = info.totalram * info.mem_unit;
        unsigned long long free = info.freeram * info.mem_unit;
        
        putLongInMap(env, memoryInfoMap, "total", (jlong)total);
        putLongInMap(env, memoryInfoMap, "available", (jlong)free);
        putLongInMap(env, memoryInfoMap, "used", (jlong)(total - free));
    } else {
        // Fallback values
        putLongInMap(env, memoryInfoMap, "total", (jlong)(8ULL * 1024 * 1024 * 1024)); // 8GB
        putLongInMap(env, memoryInfoMap, "available", (jlong)(4ULL * 1024 * 1024 * 1024)); // 4GB
        putLongInMap(env, memoryInfoMap, "used", (jlong)(4ULL * 1024 * 1024 * 1024)); // 4GB
    }
#endif
    
    return memoryInfoMap;
}

JNIEXPORT jobject JNICALL Java_com_example_hardware_HardwareInfoReader_getDiskInfo
(JNIEnv *env, jobject obj) {
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jobject diskList = env->NewObject(arrayListClass, arrayListConstructor);
    
    // Create first disk entry
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapConstructor = env->GetMethodID(hashMapClass, "<init>", "()V");
    jobject disk1Map = env->NewObject(hashMapClass, hashMapConstructor);
    
    putStringInMap(env, disk1Map, "device", "/dev/sda1");
    putStringInMap(env, disk1Map, "mount_point", "/");
    putLongInMap(env, disk1Map, "total_space", (jlong)(256ULL * 1024 * 1024 * 1024)); // 256GB
    putLongInMap(env, disk1Map, "free_space", (jlong)(128ULL * 1024 * 1024 * 1024)); // 128GB
    putStringInMap(env, disk1Map, "type", "ext4");
    
    // Add to list
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    env->CallBooleanMethod(diskList, addMethod, disk1Map);
    
    // Create second disk entry
    jobject disk2Map = env->NewObject(hashMapClass, hashMapConstructor);
    
    putStringInMap(env, disk2Map, "device", "/dev/sdb1");
    putStringInMap(env, disk2Map, "mount_point", "/home");
    putLongInMap(env, disk2Map, "total_space", (jlong)(512ULL * 1024 * 1024 * 1024)); // 512GB
    putLongInMap(env, disk2Map, "free_space", (jlong)(256ULL * 1024 * 1024 * 1024)); // 256GB
    putStringInMap(env, disk2Map, "type", "ext4");
    
    env->CallBooleanMethod(diskList, addMethod, disk2Map);
    
    return diskList;
}

JNIEXPORT jobject JNICALL Java_com_example_hardware_HardwareInfoReader_getNetworkInfo
(JNIEnv *env, jobject obj) {
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jobject networkList = env->NewObject(arrayListClass, arrayListConstructor);
    
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapConstructor = env->GetMethodID(hashMapClass, "<init>", "()V");
    
    // Create first network interface
    jobject net1Map = env->NewObject(hashMapClass, hashMapConstructor);
    
    putStringInMap(env, net1Map, "name", "eth0");
    putStringInMap(env, net1Map, "mac_address", "AA:BB:CC:DD:EE:FF");
    putStringInMap(env, net1Map, "ip_address", "192.168.1.100");
    putIntInMap(env, net1Map, "speed_mbps", 1000);
    
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    env->CallBooleanMethod(networkList, addMethod, net1Map);
    
    // Create second network interface
    jobject net2Map = env->NewObject(hashMapClass, hashMapConstructor);
    
    putStringInMap(env, net2Map, "name", "wlan0");
    putStringInMap(env, net2Map, "mac_address", "11:22:33:44:55:66");
    putStringInMap(env, net2Map, "ip_address", "192.168.1.101");
    putIntInMap(env, net2Map, "speed_mbps", 300);
    
    env->CallBooleanMethod(networkList, addMethod, net2Map);
    
    return networkList;
}

JNIEXPORT jobject JNICALL Java_com_example_hardware_HardwareInfoReader_getGpuInfo
(JNIEnv *env, jobject obj) {
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jobject gpuList = env->NewObject(arrayListClass, arrayListConstructor);
    
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapConstructor = env->GetMethodID(hashMapClass, "<init>", "()V");
    
    // Create first GPU
    jobject gpu1Map = env->NewObject(hashMapClass, hashMapConstructor);
    
    putStringInMap(env, gpu1Map, "name", "NVIDIA GeForce RTX 3080");
    putStringInMap(env, gpu1Map, "vendor", "NVIDIA");
    putIntInMap(env, gpu1Map, "vram_mb", 10240); // 10GB
    
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    env->CallBooleanMethod(gpuList, addMethod, gpu1Map);
    
    return gpuList;
}

} // extern "C"