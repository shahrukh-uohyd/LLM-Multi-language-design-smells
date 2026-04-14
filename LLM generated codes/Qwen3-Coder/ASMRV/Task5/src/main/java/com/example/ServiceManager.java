package com.example;

import java.util.Date;

public class ServiceManager {
    // Fields
    private static int serviceCount = 0;
    private String serviceName;
    private Date lastAccessed;
    
    // Static block
    static {
        serviceCount = 0;
    }
    
    // Constructor
    public ServiceManager(String serviceName) {
        this.serviceName = serviceName;
        this.lastAccessed = new Date();
        serviceCount++;
    }
    
    // Methods
    public String getServiceName() { return serviceName; }
    public Date getLastAccessed() { return lastAccessed; }
    
    public void updateLastAccessed() {
        this.lastAccessed = new Date();
    }
    
    public static int getServiceCount() { return serviceCount; }
    
    // Package-private method
    void internalServiceOperation() {
        System.out.println("Performing internal service operation");
    }
    
    // Private method
    private boolean validateService() {
        return serviceName != null && !serviceName.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("ServiceManager{serviceName='%s', lastAccessed=%s, serviceCount=%d}", 
                           serviceName, lastAccessed, serviceCount);
    }
}