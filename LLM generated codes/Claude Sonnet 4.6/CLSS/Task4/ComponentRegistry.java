package com.example.app;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry that the native C++ module queries to locate
 * specific Java components by their string ID.
 */
public class ComponentRegistry {

    private static final Map<String, NativeControllable> REGISTRY = new HashMap<>();

    static {
        // Register all components available to the native layer
        register(new UIController());
        register(new DataProcessor());
    }

    private static void register(NativeControllable component) {
        REGISTRY.put(component.getComponentId(), component);
        System.out.println("[Registry] Registered component: "
                + component.getComponentId());
    }

    /**
     * Called by the native module to look up a component by its ID.
     *
     * @param componentId  The unique ID of the desired component.
     * @return             The matching NativeControllable, or null if not found.
     */
    public static NativeControllable getComponent(String componentId) {
        NativeControllable component = REGISTRY.get(componentId);
        if (component == null) {
            System.err.println("[Registry] Component not found: " + componentId);
        }
        return component;
    }
}