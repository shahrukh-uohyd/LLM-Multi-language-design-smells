#include <jni.h>
#include <unistd.h>

JNIEXPORT jint JNICALL
Java_NativeHardwareInfo_getCpuCoreCount(
        JNIEnv* env,
        jclass) {

    long cores = sysconf(_SC_NPROCESSORS_ONLN);
    if (cores < 1) {
        return 1; // safe fallback
    }
    return (jint)cores;
}

JNIEXPORT jlong JNICALL
Java_NativeHardwareInfo_getTotalMemoryMB(
        JNIEnv* env,
        jclass) {

    long pages = sysconf(_SC_PHYS_PAGES);
    long pageSize = sysconf(_SC_PAGE_SIZE);

    if (pages < 0 || pageSize < 0) {
        return 0;
    }

    long long totalBytes =
        (long long)pages * (long long)pageSize;

    return (jlong)(totalBytes / (1024 * 1024));
}
