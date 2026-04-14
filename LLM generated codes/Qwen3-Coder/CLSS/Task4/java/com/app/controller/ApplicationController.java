package com.app.controller;

import java.util.ArrayList;
import java.util.List;

public class ApplicationController {
    private List<String> logMessages = new ArrayList<>();
    private boolean running = false;
    
    public void startApplication() {
        running = true;
        logMessage("Application started");
        System.out.println("Application started");
    }
    
    public void stopApplication() {
        running = false;
        logMessage("Application stopped");
        System.out.println("Application stopped");
    }
    
    public void restartApplication() {
        logMessage("Application restarting...");
        stopApplication();
        try {
            Thread.sleep(1000); // Simulate shutdown delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startApplication();
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void setLogLevel(String level) {
        logMessage("Log level set to: " + level);
        System.out.println("Log level changed to: " + level);
    }
    
    public void performMaintenance() {
        logMessage("Performing maintenance tasks");
        System.out.println("Maintenance tasks executed");
    }
    
    public void logMessage(String message) {
        logMessages.add(System.currentTimeMillis() + ": " + message);
    }
    
    public List<String> getLogMessages() {
        return new ArrayList<>(logMessages);
    }
    
    public String getStatusInfo() {
        return "Status: " + (running ? "RUNNING" : "STOPPED") + 
               ", Log entries: " + logMessages.size();
    }
}