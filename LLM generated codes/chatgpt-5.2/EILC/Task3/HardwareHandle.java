public final class HardwareHandle {

    // Opaque pointer to native kernel / driver context
    private final long nativeHandle;

    HardwareHandle(long nativeHandle) {
        if (nativeHandle == 0) {
            throw new IllegalArgumentException("Invalid native hardware handle");
        }
        this.nativeHandle = nativeHandle;
    }

    long getNativeHandle() {
        return nativeHandle;
    }
}
