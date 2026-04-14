package com.example.net;

public class NetworkPacket {

    // Field written by native code
    public long timestamp;

    static {
        System.loadLibrary("net_monitor");
    }

    // Native method declaration
    private static native void recordTimestamp(NetworkPacket packet);

    // Constructor call site
    public NetworkPacket() {
        // Record creation time
        recordTimestamp(this);
    }

    // Background listener call site
    public void refresh() {
        // Update timestamp when packet is refreshed
        recordTimestamp(this);
    }

    @Override
    public String toString() {
        return "NetworkPacket{timestamp=" + timestamp + "}";
    }
}
