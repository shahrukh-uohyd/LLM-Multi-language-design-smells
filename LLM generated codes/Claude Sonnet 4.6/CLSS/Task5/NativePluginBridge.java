package com.example.plugins;

import java.util.Map;
import java.util.HashMap;

/**
 * JNI bridge through which native C++ code invokes Java plugin functionality.
 */
public final class NativePluginBridge {

    static {
        System.loadLibrary("nativeplugin"); // libnativeplugin.so / nativeplugin.dll
    }

    /**
     * Asks native code to locate the plugin and execute it.
     *
     * @param pluginId       Target plugin identifier.
     * @param operationName  The operation to perform.
     * @param paramKeys      Parameter keys (paired with paramValues).
     * @param paramValues    Parameter values (paired with paramKeys).
     * @param flags          Control flags bitmask.
     * @return               The integer result code from PluginResult.
     */
    public native int executePlugin(String   pluginId,
                                    String   operationName,
                                    String[] paramKeys,
                                    String[] paramValues,
                                    int      flags);

    // --- Example usage ---
    public static void main(String[] args) {
        NativePluginBridge bridge = new NativePluginBridge();

        // Execute ImageProcessingPlugin via native
        int result1 = bridge.executePlugin(
                "image.processing",
                "RESIZE",
                new String[]{"width", "height"},
                new String[]{"1920", "1080"},
                0x01
        );
        System.out.println("Image result code: " + result1);

        // Execute NetworkPlugin via native
        int result2 = bridge.executePlugin(
                "network.operations",
                "CONNECT",
                new String[]{"host", "port"},
                new String[]{"api.example.com", "443"},
                0x00
        );
        System.out.println("Network result code: " + result2);
    }
}