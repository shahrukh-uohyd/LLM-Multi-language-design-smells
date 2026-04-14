package com.plugin.impl;

import com.plugin.api.PluginInterface;

public class SamplePlugin implements PluginInterface {
    private String name = "SamplePlugin";
    private String version = "1.0.0";
    private boolean enabled = true;
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public void initialize() {
        System.out.println("SamplePlugin initialized");
    }
    
    @Override
    public void execute() {
        if (enabled) {
            System.out.println("SamplePlugin executing...");
            // Simulate some work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            System.out.println("SamplePlugin is disabled, skipping execution");
        }
    }
    
    @Override
    public void executeWithParams(String[] parameters) {
        if (enabled) {
            System.out.println("SamplePlugin executing with parameters:");
            for (String param : parameters) {
                System.out.println("  - " + param);
            }
        }
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        System.out.println("SamplePlugin " + (enabled ? "enabled" : "disabled"));
    }
    
    @Override
    public String getDescription() {
        return "A sample plugin implementation for demonstration purposes";
    }
    
    @Override
    public String processInput(String input) {
        return "Processed: " + input.toUpperCase();
    }
}