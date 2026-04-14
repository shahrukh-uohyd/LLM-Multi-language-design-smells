package com.plugin;

import com.plugin.api.PluginManager;

public class ExamplePluginUsage {
    public static void main(String[] args) {
        // Register sample plugin
        NativePluginHost.registerSamplePlugin();
        
        NativePluginHost host = new NativePluginHost();
        
        System.out.println("Listing available plugins:");
        String[] plugins = host.listAvailablePlugins();
        for (String plugin : plugins) {
            System.out.println("  - " + plugin);
        }
        
        System.out.println("\nQuerying plugin info:");
        String info = host.queryPluginInfo("SamplePlugin");
        System.out.println(info);
        
        System.out.println("\nExecuting plugin from native code:");
        host.executePlugin("SamplePlugin");
        
        System.out.println("\nExecuting plugin with parameters:");
        String[] params = {"param1", "param2", "param3"};
        host.executePluginWithParams("SamplePlugin", params);
        
        System.out.println("\nDisabling plugin:");
        host.setPluginState("SamplePlugin", false);
        
        System.out.println("\nTrying to execute disabled plugin:");
        host.executePlugin("SamplePlugin");
        
        System.out.println("\nEnabling plugin:");
        host.setPluginState("SamplePlugin", true);
        
        System.out.println("\nProcessing input through plugin:");
        String processed = host.processPluginInput("SamplePlugin", "hello world");
        System.out.println("Result: " + processed);
        
        System.out.println("\nShutting down...");
        host.shutdown();
    }
}