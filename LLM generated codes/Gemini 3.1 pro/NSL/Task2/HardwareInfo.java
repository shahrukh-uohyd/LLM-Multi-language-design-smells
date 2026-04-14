public class HardwareInfo {

    // Load the native C/C++ library during class initialization
    static {
        // The library will be named libhwinfo.so (Linux), libhwinfo.dylib (Mac), or hwinfo.dll (Windows)
        System.loadLibrary("hwinfo");
    }

    /**
     * Native method to retrieve the number of logical CPU cores.
     * @return The number of CPU cores.
     */
    public native int getCpuCores();

    /**
     * Native method to retrieve the total physical memory (RAM).
     * @return Total RAM in bytes.
     */
    public native long getTotalRam();

    public static void main(String[] args) {
        HardwareInfo hwInfo = new HardwareInfo();

        System.out.println("Querying hardware information via JNI...\n");

        // 1. Get CPU Cores
        int cores = hwInfo.getCpuCores();
        System.out.println("Hardware CPU Cores : " + cores);

        // 2. Get Total RAM and process it
        long ramBytes = hwInfo.getTotalRam();
        
        // Convert bytes to Gigabytes (GB) for readability
        double ramGB = (double) ramBytes / (1024 * 1024 * 1024);
        
        System.out.printf("Total Physical RAM : %d bytes (%.2f GB)%n", ramBytes, ramGB);
    }
}