package com.app;

public class ExampleIntegration {
    public static void main(String[] args) {
        NativeController controller = new NativeController();
        
        System.out.println("Initializing system...");
        controller.initializeSystem();
        
        try {
            Thread.sleep(1000); // Wait for initialization
            
            System.out.println("\nQuerying system status...");
            String status = controller.querySystemStatus();
            System.out.println("Current status: " + status);
            
            System.out.println("\nSending control commands...");
            controller.sendControlCommand("setLogLevel");
            controller.sendControlCommand("maintenance");
            
            Thread.sleep(2000); // Wait for operations
            
            System.out.println("\nShutting down system...");
            controller.shutdownSystem();
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}