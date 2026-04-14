public class NetworkPacket {

    public long   timestamp;   // written by native recordTimestamp()
    public String sourceIp;
    public int    payloadSize;

    /**
     * Constructor — first call site for recordTimestamp().
     *
     * The native method stamps the packet at the moment of creation.
     * initNativeCache() is guaranteed to have run before any constructor
     * is called (enforced by the static initialiser in NetworkMonitor).
     */
    public NetworkPacket(String sourceIp, int payloadSize) {
        this.sourceIp    = sourceIp;
        this.payloadSize = payloadSize;

        // ✅ Call site 1: stamp the packet at construction time.
        // Uses the cached jfieldID internally — no lookup cost here.
        NetworkMonitor.recordTimestamp(this);
    }

    /**
     * Refresh the timestamp to reflect when this packet was last seen.
     * Called by the background listener thread whenever the packet is
     * re-observed on the wire.
     */
    public void refresh() {
        // ✅ Call site 2: re-stamp the packet from a background thread.
        // The cached jfieldID was resolved on the main thread during init,
        // so there is no FindClass / classloader hazard here.
        NetworkMonitor.recordTimestamp(this);
    }

    @Override
    public String toString() {
        return String.format(
            "NetworkPacket{sourceIp='%s', payloadSize=%d, timestamp=%d}",
            sourceIp, payloadSize, timestamp);
    }
}