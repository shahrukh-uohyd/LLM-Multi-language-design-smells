public class HardwareHandle {
    // Stores the raw C++ pointer or OS file descriptor
    private final long nativePointer;

    public HardwareHandle(long nativePointer) {
        this.nativePointer = nativePointer;
    }

    public long getNativePointer() {
        return nativePointer;
    }

    public boolean isConnected() {
        return nativePointer != 0;
    }
}