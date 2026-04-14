public class SystemProbe {
    private final HardwareHandle hardwareHandle;

    static {
        // Load the native C++ library that interacts with the kernel
        System.loadLibrary("hwmonitor");
    }

    public SystemProbe(HardwareHandle hardwareHandle) {
        this.hardwareHandle = hardwareHandle;
    }

    /**
     * Performs a deep scan by querying the kernel for 6 specific hardware metrics.
     * 
     * @return DeepScanResult containing temperatures, fan speeds, and voltage.
     * @throws IllegalStateException if the hardware handle is invalid.
     */
    public DeepScanResult performDeepScan() {
        // 1. Validate the handle before crossing the JNI bridge
        if (hardwareHandle == null || !hardwareHandle.isValid()) {
            throw new IllegalStateException("Cannot perform scan: HardwareHandle is disconnected or invalid.");
        }

        // 2. Extract the native pointer and pass it to the JNI method.
        // This single JNI call gathers all 6 metrics simultaneously to maximize performance.
        return executeNativeDeepScan(hardwareHandle.getNativePtr());
    }

    /**
     * Native declaration: Takes the kernel handle pointer, reads the hardware sensors,
     * and constructs a DeepScanResult object containing all 6 metrics.
     * 
     * @param handlePtr The raw memory address / file descriptor from HardwareHandle.
     * @return A newly allocated DeepScanResult object populated by C++.
     */
    private native DeepScanResult executeNativeDeepScan(long handlePtr);
}