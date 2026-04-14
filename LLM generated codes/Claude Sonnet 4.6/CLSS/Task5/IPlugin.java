package com.example.plugins;

/**
 * Contract that every plugin must fulfill.
 * Native C++ code invokes {@link #execute} through JNI using this interface's
 * method ID, allowing the JVM to dispatch polymorphically to any implementation.
 */
public interface IPlugin {

    /**
     * Returns the unique identifier for this plugin.
     * Used by native code to locate the correct plugin in the registry.
     */
    String getPluginId();

    /**
     * Returns the API version this plugin targets.
     * Native code may use this for compatibility gating.
     */
    int getApiVersion();

    /**
     * Executes the plugin's core functionality.
     *
     * @param context  Execution context populated by native C++ code.
     * @return         A {@link PluginResult} reporting the outcome.
     */
    PluginResult execute(PluginContext context);
}