public class SystemProbe {
    private final HardwareHandle hardwareHandle;

    static {
        // Load the native C++ library (e.g., libhwmonitor.so or hwmonitor.dll)
        System.loadLibrary("hwmonitor");
    }

    public SystemProbe(HardwareHandle hardwareHandle) {
        this.hardwareHandle = hardwareHandle;
    }

    /**
     * Executes a deep scan utilizing the hardware handle to retrieve all 6 metrics.
     * 
     * @return HardwareMetrics object containing the queried data.
     * @throws IllegalStateException if the kernel handle is disconnected.
     */
    public HardwareMetrics performDeepScan() {
        if (hardwareHandle == null || !hardwareHandle.isConnected()) {
            throw new IllegalStateException("Cannot perform scan: HardwareHandle is disconnected.");
        }

        // Extract the native pointer in Java and pass it as a primitive 'long'.
        // This is much faster than passing the HardwareHandle object to C++ 
        // and forcing the native layer to look up the pointer field.
        long kernelPointer = hardwareHandle.getNativePointer();
        
        return nativePerformDeepScan(kernelPointer);
    }

    /**
     * Native declaration: Takes the raw handle pointer, queries the kernel for
     * the 6 specific metrics, and constructs a HardwareMetrics object.
     * 
     * @param handlePtr The raw memory address linking to the kernel driver.
     * @return Populated HardwareMetrics object.
     */
    private native HardwareMetrics nativePerformDeepScan(long handlePtr);
}