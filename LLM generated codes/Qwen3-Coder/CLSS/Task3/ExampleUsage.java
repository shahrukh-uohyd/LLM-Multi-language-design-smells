package com.example;

public class ExampleUsage {
    public static void main(String[] args) {
        NativeInvoker invoker = new NativeInvoker();
        
        System.out.println("Starting native-triggered operations...");
        invoker.triggerOperations();
        
        // Give time for the operations to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("Main thread continuing...");
    }
}