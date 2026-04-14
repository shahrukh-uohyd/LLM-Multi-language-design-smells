package com.example.operations;

/**
 * JNI bridge that exposes native entry points to trigger Java operations.
 */
public class NativeOperationBridge {

    static {
        System.loadLibrary("nativeops"); // libnativeops.so / nativeops.dll
    }

    /**
     * Triggers an instance-level Java operation from native code.
     *
     * @param handler       The OperationHandler instance the native code will call into.
     * @param operationType String operation tag forwarded to performOperation().
     * @param priority      Integer priority forwarded to performOperation().
     */
    public native void triggerInstanceOperation(OperationHandler handler,
                                                String operationType,
                                                int priority);

    /**
     * Triggers a static Java method from native code — no handler instance needed.
     *
     * @param eventCode  Numeric event code forwarded to handleNativeEvent().
     * @param payload    Payload string forwarded to handleNativeEvent().
     */
    public native void triggerStaticEvent(int eventCode, String payload);


    // --- Example usage ---
    public static void main(String[] args) {
        NativeOperationBridge bridge = new NativeOperationBridge();
        OperationHandler handler = new OperationHandler("PrimaryHandler");

        // Trigger an instance method from native
        bridge.triggerInstanceOperation(handler, "SYNC", 5);

        // Trigger a static method from native
        bridge.triggerStaticEvent(42, "sensor_threshold_exceeded");
    }
}