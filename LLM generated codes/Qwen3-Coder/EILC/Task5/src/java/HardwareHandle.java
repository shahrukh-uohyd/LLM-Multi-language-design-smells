// HardwareHandle.java
public class HardwareHandle {
    private long handlePtr;  // Native pointer to hardware interface
    private boolean isValid;
    
    public HardwareHandle(long handlePtr) {
        this.handlePtr = handlePtr;
        this.isValid = (handlePtr != 0);
    }
    
    public long getHandlePtr() {
        return handlePtr;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    // Prevent garbage collection if handle is still needed
    @Override
    protected void finalize() throws Throwable {
        if (isValid) {
            cleanupNativeResources(handlePtr);
        }
        super.finalize();
    }
    
    private native void cleanupNativeResources(long handle);
}