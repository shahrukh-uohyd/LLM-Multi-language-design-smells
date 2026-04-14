package com.example;

public class OperationManager {
    
    // The static method that C++ will locate and invoke
    public static void performOperation(String source, int statusCode) {
        System.out.println("Java Operation Triggered!");
        System.out.println("Source: " + source);
        System.out.println("Status Code: " + statusCode);
        
        // Execute the required Java operations here...
    }
}