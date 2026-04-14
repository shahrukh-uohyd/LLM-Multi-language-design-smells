package com.example;

public class NativeInvoker {
    static {
        System.loadLibrary("nativeinvoker");
    }
    
    // Method that triggers native code to call Java operations
    public native void triggerOperations();
    
    // Callback method that will be called from native code
    public void onOperationComplete(String result) {
        System.out.println("Native callback received: " + result);
    }
}