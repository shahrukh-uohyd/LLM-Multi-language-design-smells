package com.example;

// The Java component that the C++ module will locate and control
public class SystemController {
    
    // Singleton instance
    private static final SystemController instance = new SystemController();

    private SystemController() {
        // Private constructor for singleton
    }

    // 1. Static method for C++ to "locate" the active component
    public static SystemController getInstance() {
        return instance;
    }

    // 2. The functionality that C++ will trigger
    public void executeCommand(String command, int priority) {
        System.out.println("SystemController executing command: '" + command + "' with priority " + priority);
        
        // App-specific logic goes here (e.g., updating UI, writing to DB, routing events)
    }
}