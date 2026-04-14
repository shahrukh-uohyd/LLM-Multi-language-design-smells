package com.plugin.api;

public interface PluginInterface {
    String getName();
    String getVersion();
    void initialize();
    void execute();
    void executeWithParams(String[] parameters);
    boolean isEnabled();
    void setEnabled(boolean enabled);
    String getDescription();
    String processInput(String input);
}