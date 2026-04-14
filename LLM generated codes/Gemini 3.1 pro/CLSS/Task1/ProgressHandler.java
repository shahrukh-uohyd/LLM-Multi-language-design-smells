package com.example;

// The handler class that will receive callbacks from C++
public class ProgressHandler {
    public void onProgress(int status, String message) {
        System.out.println("Status [" + status + "%]: " + message);
    }
}