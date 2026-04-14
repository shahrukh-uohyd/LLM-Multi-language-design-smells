public class NativeHardwareInfo {

    static {
        System.loadLibrary("hardwareinfo");
    }

    /**
     * @return number of available CPU cores
     */
    public static native int getCpuCoreCount();

    /**
     * @return total physical memory in megabytes
     */
    public static native long getTotalMemoryMB();
}
