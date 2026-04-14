package com.example.network;

public class NetworkPacket {
    
    // The long field to store the timestamp
    public long timestamp;

    static {
        // Load the compiled C++ library (e.g., libnetworkmonitor.so or networkmonitor.dll)
        System.loadLibrary("networkmonitor");
    }

    // Native method declaration taking the packet explicitly as an argument
    public static native void recordTimestamp(NetworkPacket packet);

    // Call site 1: Inside the constructor
    public NetworkPacket() {
        // Initialize other packet data...
        
        // Stamp the creation time
        recordTimestamp(this);
    }

    // Call site 2: Inside a refresh method
    public void refresh() {
        // Update packet payload or state...
        
        // Update the timestamp to the current time
        recordTimestamp(this);
    }

    // Example background listener simulation
    public static void main(String[] args) throws InterruptedException {
        // Creates the packet (triggers Call Site 1)
        NetworkPacket packet = new NetworkPacket();
        System.out.println("Creation timestamp: " + packet.timestamp);

        // Simulate network delay
        Thread.sleep(1500);

        // Refresh the packet (triggers Call Site 2)
        packet.refresh();
        System.out.println("Refreshed timestamp: " + packet.timestamp);
    }
}