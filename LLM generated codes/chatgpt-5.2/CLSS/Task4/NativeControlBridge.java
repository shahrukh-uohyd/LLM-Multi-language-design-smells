public class NativeControlBridge {

    static {
        System.loadLibrary("nativecontrol");
    }

    // Native method that triggers Java components
    public native void executeNativeControl();

    public static void main(String[] args) {
        NativeControlBridge bridge = new NativeControlBridge();
        bridge.executeNativeControl();
    }
}
