public class HardwareHandle {
    // Represents the raw pointer to the native C++ object or kernel file descriptor
    private final long nativePtr;

    public HardwareHandle(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    public long getNativePtr() {
        return nativePtr;
    }

    public boolean isValid() {
        return nativePtr != 0;
    }
}