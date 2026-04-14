package com.example;

import java.util.HashMap;
import java.util.Map;

// A registry where plugins are stored and looked up by the native code
public class PluginManager {
    private static final Map<String, IPlugin> plugins = new HashMap<>();

    // Called during app initialization to register available plugins
    public static void registerPlugin(String name, IPlugin plugin) {
        plugins.put(name, plugin);
    }

    // Called by the Native C++ code to retrieve a specific plugin
    public static IPlugin getPlugin(String name) {
        return plugins.get(name);
    }
}