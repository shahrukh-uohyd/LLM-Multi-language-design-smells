package com.app;

import com.app.controller.ApplicationController;
import com.app.controller.ComponentRegistry;

public class NativeController {
    static {
        System.loadLibrary("appcontroller");
    }
    
    private ApplicationController appController;
    
    public NativeController() {
        this.appController = new ApplicationController();
        ComponentRegistry.registerComponent("mainController", appController);
    }
    
    // Methods that will be called from native code
    public void onNativeCommand(String command, String[] parameters) {
        System.out.println("Native command received: " + command);
        for (String param : parameters) {
            System.out.println("  Parameter: " + param);
        }
    }
    
    public void onStatusChange(String status, int code) {
        System.out.println("Status change: " + status + " (code: " + code + ")");
    }
    
    // JNI methods to trigger native operations
    public native void initializeSystem();
    public native void sendControlCommand(String command);
    public native void shutdownSystem();
    public native String querySystemStatus();
    
    // Getter for the application controller
    public ApplicationController getAppController() {
        return appController;
    }
}