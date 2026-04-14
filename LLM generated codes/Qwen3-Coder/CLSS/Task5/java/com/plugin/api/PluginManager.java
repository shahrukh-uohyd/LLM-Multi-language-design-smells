package com.plugin.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PluginManager {
    private static final Map<String, PluginInterface> plugins = new HashMap<>();
    
    public static void registerPlugin(String name, PluginInterface plugin) {
        plugins.put(name, plugin);
    }
    
    public static PluginInterface getPlugin(String name) {
        return plugins.get(name);
    }
    
    public static Set<String> getAvailablePlugins() {
        return plugins.keySet();
    }
    
    public static boolean hasPlugin(String name) {
        return plugins.containsKey(name);
    }
    
    public static void unregisterPlugin(String name) {
        plugins.remove(name);
    }
    
    public static void executeAllPlugins() {
        for (PluginInterface plugin : plugins.values()) {
            try {
                plugin.execute();
            } catch (Exception e) {
                System.err.println("Error executing plugin: " + plugin.getName() + " - " + e.getMessage());
            }
        }
    }
}