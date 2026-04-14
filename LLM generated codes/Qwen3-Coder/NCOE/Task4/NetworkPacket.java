// NetworkPacket.java
public class NetworkPacket {
    private long timestamp;
    private String sourceIP;
    private String destinationIP;
    private int size;
    
    public NetworkPacket(String sourceIP, String destinationIP, int size) {
        this.sourceIP = sourceIP;
        this.destinationIP = destinationIP;
        this.size = size;
        
        // First call site: Set initial timestamp in constructor
        recordTimestamp();
    }
    
    public void refresh() {
        // Second call site: Update timestamp when refreshing packet data
        recordTimestamp();
    }
    
    // Native method declaration
    private native void recordTimestamp();
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getSourceIP() {
        return sourceIP;
    }
    
    public String getDestinationIP() {
        return destinationIP;
    }
    
    public int getSize() {
        return size;
    }
    
    @Override
    public String toString() {
        return "NetworkPacket{sourceIP='" + sourceIP + "', destinationIP='" + destinationIP + 
               "', size=" + size + ", timestamp=" + timestamp + "}";
    }
    
    static {
        System.loadLibrary("network_monitor"); // Load native library
    }
}