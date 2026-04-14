package com.example.app;

/**
 * JNI bridge exposing all native entry points to the Java application.
 */
public class NativeModuleBridge {

    static {
        System.loadLibrary("nativemodule"); // libnativemodule.so / nativemodule.dll
    }

    /**
     * Locate a registered component and trigger it from native code.
     *
     * @param componentId  Registry ID of the target Java component.
     * @param params       Parameter string forwarded to component.trigger().
     * @param flags        Flags bitmask forwarded to component.trigger().
     * @return             Result string from the component, or null on failure.
     */
    public native String triggerComponent(String componentId,
                                          String params,
                                          int flags);

    /**
     * Signals an app lifecycle event from native code.
     *
     * @param event   Lifecycle event tag.
     * @param reason  Reason description from native.
     */
    public native void signalLifecycleEvent(String event, String reason);


    // --- Example usage ---
    public static void main(String[] args) {
        NativeModuleBridge bridge = new NativeModuleBridge();

        // Trigger the UIController via native
        String uiResult = bridge.triggerComponent(
                "UIController", "SHOW_DIALOG:title=NativeAlert", 0x03);
        System.out.println("UI Result: " + uiResult);

        // Trigger the DataProcessor via native
        String dataResult = bridge.triggerComponent(
                "DataProcessor", "TRANSFORM:format=JSON", 0x05);
        System.out.println("Data Result: " + dataResult);

        // Signal a lifecycle event from native
        bridge.signalLifecycleEvent("PAUSE", "Native low-memory pressure detected");
    }
}