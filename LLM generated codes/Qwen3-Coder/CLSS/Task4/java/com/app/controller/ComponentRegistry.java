package com.app.controller;

import java.util.HashMap;
import java.util.Map;

public class ComponentRegistry {
    private static final Map<String, Object> components = new HashMap<>();
    
    public static void registerComponent(String name, Object component) {
        components.put(name, component);
    }
    
    public static Object getComponent(String name) {
        return components.get(name);
    }
    
    public static boolean hasComponent(String name) {
        return components.containsKey(name);
    }
    
    public static void removeComponent(String name) {
        components.remove(name);
    }
}