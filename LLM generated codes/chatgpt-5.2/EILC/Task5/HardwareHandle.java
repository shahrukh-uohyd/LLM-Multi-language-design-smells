public final class HardwareHandle {

    /**
     * Native pointer or kernel handle.
     * Must never be interpreted on the Java side.
     */
    private final long nativeHandle;

    HardwareHandle(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    long getNativeHandle() {
        return nativeHandle;
    }
}
