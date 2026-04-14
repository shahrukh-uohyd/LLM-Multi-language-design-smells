// CompressionObserver.java
public interface CompressionObserver {
    void onCompressionEvent(String eventType, byte[] data);
}