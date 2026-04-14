package com.example;

public class NativeCore {
    static {
        System.loadLibrary("plugin_runner_lib");
    }

    // Native method that asks C++ to run a specific plugin
    public native void runPluginBehavior(String pluginName);

    public static void main(String[] args) {
        // 1. Register a sample plugin
        PluginManager.registerPlugin("NetworkPlugin", new IPlugin() {
            @Override
            public void execute(String payload) {
                System.out.println("[NetworkPlugin] Executing with payload: " + payload);
            }
        });

        // 2. Trigger the C++ logic
        NativeCore core = new NativeCore();
        core.runPluginBehavior("NetworkPlugin");
    }
}