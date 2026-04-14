public class CameraHardware {

    static {
        System.loadLibrary("camera_native");
    }

    // Native declarations for hardware control
    private native void setFocusMode(String mode);
    private native boolean openShutter();

    /**
     * Prepares the camera lens and captures an image by triggering the physical shutter.
     * 
     * @param focusMode The desired focus mode (e.g., "AUTO", "MACRO", "INFINITY").
     * @return true if the shutter was successfully opened and image captured.
     */
    public boolean capturePhoto(String focusMode) {
        System.out.println("Configuring camera hardware...");
        
        // 1. Adjust the physical lens focus via JNI
        setFocusMode(focusMode);
        System.out.println("Focus mode set to: " + focusMode);
        
        // 2. Trigger the hardware shutter
        boolean success = openShutter();
        
        if (success) {
            System.out.println("Shutter opened successfully. Photo captured.");
        } else {
            System.err.println("Hardware Error: Failed to open shutter.");
        }
        
        return success;
    }
}