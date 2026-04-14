package com.example;

public class Calculator {
    static {
        // Load the compiled C++ native library
        System.loadLibrary("calculator_lib"); 
    }

    // Native method that takes our handler object as an argument
    public native void startCalculation(ProgressHandler handler);

    public static void main(String[] args) {
        Calculator calc = new Calculator();
        ProgressHandler handler = new ProgressHandler();
        
        System.out.println("Starting native calculation...");
        calc.startCalculation(handler);
        
        // Keep the main thread alive long enough to see the background C++ threads finish
        try { Thread.sleep(6000); } catch (InterruptedException e) {}
    }
}