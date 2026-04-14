import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class NativeComponent {
    static {
        System.loadLibrary("nativecomponent"); // Load your native library
    }
    
    // JNI method declaration - starts the long-running calculation
    public native void startCalculation(long handlerId);
    
    // This method will be called by the native code to update status
    private void updateStatus(long handlerId, int statusCode, String description) {
        // Retrieve the handler from a static map using handlerId
        StatusHandler handler = getHandlerFromId(handlerId);
        if (handler != null) {
            handler.onStatusUpdate(statusCode, description);
        }
    }
    
    // Store handlers in a static map with unique IDs
    private static final Map<Long, StatusHandler> handlerMap = new ConcurrentHashMap<>();
    private static long nextHandlerId = 1;
    
    // Register a handler and return its ID
    public static synchronized long registerHandler(StatusHandler handler) {
        long id = nextHandlerId++;
        handlerMap.put(id, handler);
        return id;
    }
    
    // Unregister a handler
    public static synchronized void unregisterHandler(long handlerId) {
        handlerMap.remove(handlerId);
    }
    
    // Get handler by ID
    private static StatusHandler getHandlerFromId(long handlerId) {
        return handlerMap.get(handlerId);
    }
}