package com.example.plugins;

/**
 * Event bus used by native C++ code to broadcast plugin execution outcomes
 * to interested Java-side observers.
 */
public final class PluginEventBus {

    private PluginEventBus() {}   // Utility class — no instances

    /**
     * Called by native code after a plugin execution completes.
     *
     * @param pluginId    The ID of the plugin that ran.
     * @param resultCode  The integer result code from {@link PluginResult}.
     * @param message     The result message.
     */
    public static void publishEvent(String pluginId,
                                     int    resultCode,
                                     String message)
    {
        System.out.printf("[PluginEventBus] Plugin='%s' | code=%d | msg='%s'%n",
                pluginId, resultCode, message);
        // Real implementation: notify registered Java-side listeners
    }
}