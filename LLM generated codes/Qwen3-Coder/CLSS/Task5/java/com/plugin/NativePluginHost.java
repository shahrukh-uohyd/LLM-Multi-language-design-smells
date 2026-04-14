package com.plugin;

import com.plugin.api.PluginInterface;
import com.plugin.api.PluginManager;

public class NativePluginHost {
    static {
        System.loadLibrary("pluginhost");
    }
    
    // JNI methods for native code to control plugins
    public native boolean loadPlugin(String pluginName, String className);
    public native boolean executePlugin(String pluginName);
    public native boolean executePluginWithParams(String pluginName, String[] parameters);
    public native String queryPluginInfo(String pluginName);
    public native boolean setPluginState(String pluginName, boolean enabled);
    public native String processPluginInput(String pluginName, String input);
    public native String[] listAvailablePlugins();
    public native void shutdown();
    
    // Callback methods that will be called from native code
    public void onPluginLoaded(String pluginName, boolean success) {
        System.out.println("Plugin " + pluginName + " loading " + (success ? "successful" : "failed"));
    }
    
    public void onPluginExecuted(String pluginName, String result) {
        System.out.println("Plugin " + pluginName + " executed: " + result);
    }
    
    public void onPluginError(String pluginName, String errorMessage) {
        System.err.println("Plugin " + pluginName + " error: " + errorMessage);
    }
    
    // Static method to register plugins from Java side
    public static void registerSamplePlugin() {
        PluginManager.registerPlugin("SamplePlugin", new com.plugin.impl.SamplePlugin());
    }
}