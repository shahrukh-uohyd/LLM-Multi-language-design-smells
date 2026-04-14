package com.example.plugins;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Thread-safe registry that native C++ code queries to locate plugins by ID.
 * Supports runtime registration and unregistration of plugin instances.
 */
public final class PluginRegistry {

    private static final Map<String, IPlugin> REGISTRY =
            new ConcurrentHashMap<>();

    static {
        // Built-in plugin registrations
        register(new ImageProcessingPlugin());
        register(new NetworkPlugin());
    }

    private PluginRegistry() {}   // Utility class — no instances

    /**
     * Registers a plugin instance.
     * Any previously registered plugin with the same ID is replaced.
     *
     * @param plugin  The plugin to register.
     */
    public static void register(IPlugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin is null");
        REGISTRY.put(plugin.getPluginId(), plugin);
        System.out.println("[PluginRegistry] Registered: " + plugin.getPluginId()
                + " (API v" + plugin.getApiVersion() + ")");
    }

    /**
     * Called by native C++ code to locate a plugin by its unique ID.
     *
     * @param pluginId  The plugin identifier.
     * @return          The matching {@link IPlugin}, or {@code null} if absent.
     */
    public static IPlugin lookupPlugin(String pluginId) {
        IPlugin plugin = REGISTRY.get(pluginId);
        if (plugin == null) {
            System.err.println("[PluginRegistry] Lookup failed: " + pluginId);
        }
        return plugin;
    }

    /**
     * Unregisters a plugin. Called by native code during teardown.
     *
     * @param pluginId  The ID of the plugin to remove.
     */
    public static void unregister(String pluginId) {
        REGISTRY.remove(pluginId);
        System.out.println("[PluginRegistry] Unregistered: " + pluginId);
    }
}