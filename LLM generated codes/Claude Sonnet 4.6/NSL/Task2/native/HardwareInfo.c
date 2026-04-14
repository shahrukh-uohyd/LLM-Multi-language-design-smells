/*
 * JNI implementation – retrieves real hardware/OS information.
 *
 * Platform support:
 *   Linux  – /proc/cpuinfo, /proc/meminfo, sysinfo(2), statvfs(2),
 *             uname(2), gethostname(2), sysconf(3)
 *   macOS  – sysctl(3), host_statistics64, statfs(2), uname(2)
 *   Windows – GetSystemInfo, GlobalMemoryStatusEx, GetDiskFreeSpaceEx,
 *             GetComputerNameA, GetTickCount64
 *
 * No third-party libraries required.
 */

#include "HardwareInfo.h"

#include <string.h>
#include <stdio.h>
#include <stdlib.h>

/* ======================================================================
 * Platform detection
 * ====================================================================== */
#if defined(_WIN32) || defined(_WIN64)
  #define PLATFORM_WINDOWS
#elif defined(__APPLE__) && defined(__MACH__)
  #define PLATFORM_MACOS
#else
  #define PLATFORM_LINUX
#endif

/* ======================================================================
 * Platform includes
 * ====================================================================== */
#ifdef PLATFORM_WINDOWS
  #include <windows.h>
  #include <sysinfoapi.h>
  #include <fileapi.h>

#elif defined(PLATFORM_MACOS)
  #include <sys/types.h>
  #include <sys/sysctl.h>
  #include <sys/mount.h>
  #include <sys/utsname.h>
  #include <mach/mach.h>
  #include <mach/mach_host.h>
  #include <unistd.h>

#else /* PLATFORM_LINUX */
  #include <sys/utsname.h>
  #include <sys/sysinfo.h>
  #include <sys/statvfs.h>
  #include <unistd.h>
#endif

/* ======================================================================
 * Helper: return a jstring, falling back to "N/A" on NULL
 * ====================================================================== */
static jstring safe_string(JNIEnv *env, const char *s) {
    return (*env)->NewStringUTF(env, s ? s : "N/A");
}

/* ======================================================================
 * getCpuModel
 * ====================================================================== */
JNIEXPORT jstring JNICALL
Java_HardwareInfo_getCpuModel(JNIEnv *env, jobject obj) {
    (void)obj;
    char model[256] = "Unknown CPU";

#ifdef PLATFORM_WINDOWS
    HKEY hKey;
    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE,
            "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0",
            0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        DWORD size = sizeof(model);
        RegQueryValueExA(hKey, "ProcessorNameString", NULL, NULL,
                         (LPBYTE)model, &size);
        RegCloseKey(hKey);
    }

#elif defined(PLATFORM_MACOS)
    size_t sz = sizeof(model);
    sysctlbyname("machdep.cpu.brand_string", model, &sz, NULL, 0);

#else /* PLATFORM_LINUX */
    FILE *f = fopen("/proc/cpuinfo", "r");
    if (f) {
        char line[512];
        while (fgets(line, sizeof(line), f)) {
            if (strncmp(line, "model name", 10) == 0) {
                char *colon = strchr(line, ':');
                if (colon) {
                    char *start = colon + 2;       /* skip ": " */
                    size_t len  = strlen(start);
                    if (len > 0 && start[len - 1] == '\n') start[len - 1] = '\0';
                    strncpy(model, start, sizeof(model) - 1);
                }
                break;
            }
        }
        fclose(f);
    }
#endif

    return safe_string(env, model);
}

/* ======================================================================
 * getCpuCores  (physical)
 * ====================================================================== */
JNIEXPORT jint JNICALL
Java_HardwareInfo_getCpuCores(JNIEnv *env, jobject obj) {
    (void)env; (void)obj;

#ifdef PLATFORM_WINDOWS
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    /* NumberOfProcessors is logical; use GetLogicalProcessorInformation
       for physical – simplified to logical here for portability */
    return (jint)si.dwNumberOfProcessors;

#elif defined(PLATFORM_MACOS)
    int cores = 1;
    size_t sz = sizeof(cores);
    sysctlbyname("hw.physicalcpu", &cores, &sz, NULL, 0);
    return (jint)cores;

#else /* PLATFORM_LINUX */
    int cores = 0;
    FILE *f = fopen("/proc/cpuinfo", "r");
    if (f) {
        char line[256];
        while (fgets(line, sizeof(line), f)) {
            if (strncmp(line, "cpu cores", 9) == 0) {
                char *colon = strchr(line, ':');
                if (colon) { cores = atoi(colon + 1); break; }
            }
        }
        fclose(f);
    }
    return (jint)(cores > 0 ? cores : 1);
#endif
}

/* ======================================================================
 * getCpuLogicalProcessors
 * ====================================================================== */
JNIEXPORT jint JNICALL
Java_HardwareInfo_getCpuLogicalProcessors(JNIEnv *env, jobject obj) {
    (void)env; (void)obj;

#ifdef PLATFORM_WINDOWS
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    return (jint)si.dwNumberOfProcessors;

#elif defined(PLATFORM_MACOS)
    int lp = 1;
    size_t sz = sizeof(lp);
    sysctlbyname("hw.logicalcpu", &lp, &sz, NULL, 0);
    return (jint)lp;

#else /* PLATFORM_LINUX */
    long n = sysconf(_SC_NPROCESSORS_ONLN);
    return (jint)(n > 0 ? n : 1);
#endif
}

/* ======================================================================
 * getCpuFrequencyMHz
 * ====================================================================== */
JNIEXPORT jlong JNICALL
Java_HardwareInfo_getCpuFrequencyMHz(JNIEnv *env, jobject obj) {
    (void)env; (void)obj;

#ifdef PLATFORM_WINDOWS
    HKEY hKey;
    DWORD mhz = 0, size = sizeof(mhz);
    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE,
            "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0",
            0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        RegQueryValueExA(hKey, "~MHz", NULL, NULL, (LPBYTE)&mhz, &size);
        RegCloseKey(hKey);
    }
    return (jlong)mhz;

#elif defined(PLATFORM_MACOS)
    long long freq = 0;
    size_t sz = sizeof(freq);
    sysctlbyname("hw.cpufrequency", &freq, &sz, NULL, 0);
    return (jlong)(freq / 1000000LL);

#else /* PLATFORM_LINUX */
    long mhz = 0;
    FILE *f = fopen("/proc/cpuinfo", "r");
    if (f) {
        char line[256];
        while (fgets(line, sizeof(line), f)) {
            if (strncmp(line, "cpu MHz", 7) == 0) {
                char *colon = strchr(line, ':');
                if (colon) { mhz = (long)atof(colon + 1); break; }
            }
        }
        fclose(f);
    }
    return (jlong)mhz;
#endif
}

/* ======================================================================
 * getTotalMemoryBytes
 * ====================================================================== */
JNIEXPORT jlong JNICALL
Java_HardwareInfo_getTotalMemoryBytes(JNIEnv *env, jobject obj) {
    (void)env; (void)obj;

#ifdef PLATFORM_WINDOWS
    MEMORYSTATUSEX ms; ms.dwLength = sizeof(ms);
    GlobalMemoryStatusEx(&ms);
    return (jlong)ms.ullTotalPhys;

#elif defined(PLATFORM_MACOS)
    long long mem = 0;
    size_t sz = sizeof(mem);
    sysctlbyname("hw.memsize", &mem, &sz, NULL, 0);
    return (jlong)mem;

#else /* PLATFORM_LINUX */
    struct sysinfo si;
    sysinfo(&si);
    return (jlong)si.totalram * (jlong)si.mem_unit;
#endif
}

/* ======================================================================
 * getFreeMemoryBytes
 * ====================================================================== */
JNIEXPORT jlong JNICALL
Java_HardwareInfo_getFreeMemoryBytes(JNIEnv *env, jobject obj) {
    (void)env; (void)obj;

#ifdef PLATFORM_WINDOWS
    MEMORYSTATUSEX ms; ms.dwLength = sizeof(ms);
    GlobalMemoryStatusEx(&ms);
    return (jlong)ms.ullAvailPhys;

#elif defined(PLATFORM_MACOS)
    mach_port_t          host   = mach_host_self();
    vm_size_t            pgsize = 0;
    host_page_size(host, &pgsize);
    vm_statistics64_data_t vmstat;
    mach_msg_type_number_t cnt = HOST_VM_INFO64_COUNT;
    host_statistics64(host, HOST_VM_INFO64,
                      (host_info64_t)&vmstat, &cnt);
    return (jlong)(vmstat.free_count + vmstat.inactive_count) * (jlong)pgsize;

#else /* PLATFORM_LINUX */
    struct sysinfo si;
    sysinfo(&si);
    return (jlong)si.freeram * (jlong)si.mem_unit;
#endif
}

/* ======================================================================
 * getOsName
 * ====================================================================== */
JNIEXPORT jstring JNICALL
Java_HardwareInfo_getOsName(JNIEnv *env, jobject obj) {
    (void)obj;

#ifdef PLATFORM_WINDOWS
    return safe_string(env, "Windows");
#else
    struct utsname u;
    uname(&u);
    return safe_string(env, u.sysname);
#endif
}

/* ======================================================================
 * getOsVersion
 * ====================================================================== */
JNIEXPORT jstring JNICALL
Java_HardwareInfo_getOsVersion(JNIEnv *env, jobject obj) {
    (void)obj;

#ifdef PLATFORM_WINDOWS
    /* Read from registry for a friendlier string */
    HKEY hKey;
    char ver[128] = "Unknown";
    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE,
            "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
            0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        DWORD sz = sizeof(ver);
        RegQueryValueExA(hKey, "CurrentBuildNumber", NULL, NULL,
                         (LPBYTE)ver, &sz);
        RegCloseKey(hKey);
    }
    return safe_string(env, ver);

#else
    struct utsname u;
    uname(&u);
    char buf[512];
    snprintf(buf, sizeof(buf), "%s %s", u.release, u.version);
    return safe_string(env, buf);
#endif
}

/* ======================================================================
 * getOsArchitecture
 * ====================================================================== */
JNIEXPORT jstring JNICALL
Java_HardwareInfo_getOsArchitecture(JNIEnv *env, jobject obj) {
    (void)obj;

#ifdef PLATFORM_WINDOWS
    SYSTEM_INFO si;
    GetNativeSystemInfo(&si);
    switch (si.wProcessorArchitecture) {
        case PROCESSOR_ARCHITECTURE_AMD64: return safe_string(env, "x86_64");
        case PROCESSOR_ARCHITECTURE_ARM:   return safe_string(env, "ARM");
        case PROCESSOR_ARCHITECTURE_ARM64: return safe_string(env, "aarch64");
        case PROCESSOR_ARCHITECTURE_INTEL: return safe_string(env, "x86");
        default:                           return safe_string(env, "Unknown");
    }
#else
    struct utsname u;
    uname(&u);
    return safe_string(env, u.machine);
#endif
}

/* ======================================================================
 * getTotalDiskBytes  (root / C:\ partition)
 * ====================================================================== */
JNIEXPORT jlong JNICALL
Java_HardwareInfo_getTotalDiskBytes(JNIEnv *env, jobject obj) {
    (void)env; (void)obj;

#ifdef PLATFORM_WINDOWS
    ULARGE_INTEGER total, free, avail;
    GetDiskFreeSpaceExA("C:\\", &avail, &total, &free);
    return (jlong)total.QuadPart;

#elif defined(PLATFORM_MACOS)
    struct statfs sf;
    statfs("/", &sf);
    return (jlong)sf.f_blocks * (jlong)sf.f_bsize;

#else /* PLATFORM_LINUX */
    struct statvfs sv;
    statvfs("/", &sv);
    return (jlong)sv.f_blocks * (jlong)sv.f_frsize;
#endif
}

/* ======================================================================
 * getFreeDiskBytes
 * ====================================================================== */
JNIEXPORT jlong JNICALL
Java_HardwareInfo_getFreeDiskBytes(JNIEnv *env, jobject obj) {
    (void)env; (void)obj;

#ifdef PLATFORM_WINDOWS
    ULARGE_INTEGER total, free, avail;
    GetDiskFreeSpaceExA("C:\\", &avail, &total, &free);
    return (jlong)avail.QuadPart;           /* avail = usable by caller */

#elif defined(PLATFORM_MACOS)
    struct statfs sf;
    statfs("/", &sf);
    return (jlong)sf.f_bavail * (jlong)sf.f_bsize;

#else /* PLATFORM_LINUX */
    struct statvfs sv;
    statvfs("/", &sv);
    return (jlong)sv.f_bavail * (jlong)sv.f_frsize;
#endif
}

/* ======================================================================
 * getHostname
 * ====================================================================== */
JNIEXPORT jstring JNICALL
Java_HardwareInfo_getHostname(JNIEnv *env, jobject obj) {
    (void)obj;
    char name[256] = "unknown";

#ifdef PLATFORM_WINDOWS
    DWORD sz = sizeof(name);
    GetComputerNameA(name, &sz);
#else
    gethostname(name, sizeof(name) - 1);
#endif

    return safe_string(env, name);
}

/* ======================================================================
 * getUptimeSeconds
 * ====================================================================== */
JNIEXPORT jlong JNICALL
Java_HardwareInfo_getUptimeSeconds(JNIEnv *env, jobject obj) {
    (void)env; (void)obj;

#ifdef PLATFORM_WINDOWS
    return (jlong)(GetTickCount64() / 1000ULL);

#elif defined(PLATFORM_MACOS)
    struct timeval boottime;
    size_t sz = sizeof(boottime);
    sysctlbyname("kern.boottime", &boottime, &sz, NULL, 0);
    time_t now;
    time(&now);
    return (jlong)(now - boottime.tv_sec);

#else /* PLATFORM_LINUX */
    struct sysinfo si;
    sysinfo(&si);
    return (jlong)si.uptime;
#endif
}