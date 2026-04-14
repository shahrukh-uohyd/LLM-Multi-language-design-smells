package com.example.app;

public class ServiceController {

    public static void startService(int level) {
        System.out.println("Service started with level: " + level);
    }

    public static void stopService(String reason) {
        System.out.println("Service stopped. Reason: " + reason);
    }
}
