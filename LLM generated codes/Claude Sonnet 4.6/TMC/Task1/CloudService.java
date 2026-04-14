/**
 * CloudService
 *
 * Manages the lifecycle of a remote relay connection and provides
 * methods to fetch configuration updates and log events to the cloud.
 */
public class CloudService {

    /** Sentinel value indicating no active connection. */
    private static final int NO_CONNECTION = -1;

    private final IIoTController controller;

    /** Handle returned by connectToRemoteRelay(). */
    private int connectionHandle = NO_CONNECTION;

    /**
     * @param controller  Shared IIoTController instance.
     */
    public CloudService(IIoTController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("IIoTController must not be null.");
        }
        this.controller = controller;
    }

    // ----------------------------------------------------------------
    // Connection lifecycle
    // ----------------------------------------------------------------

    /**
     * Connects to a remote relay server.
     *
     * @param relayHost  Relay hostname or IP.
     * @param port       TCP port.
     * @param timeoutMs  Connection timeout in milliseconds.
     * @return           true if the connection was established.
     */
    public boolean connect(String relayHost, int port, int timeoutMs) {
        if (isConnected()) {
            System.out.println("[Cloud] Already connected (handle="
                    + connectionHandle + "). Skipping connect.");
            return true;
        }
        System.out.printf("[Cloud] Connecting to relay %s:%d (timeout=%d ms)...%n",
                relayHost, port, timeoutMs);

        connectionHandle = controller.connectToRemoteRelay(relayHost, port, timeoutMs);

        if (connectionHandle < 0) {
            System.err.println("[Cloud] Connection FAILED.");
            connectionHandle = NO_CONNECTION;
            return false;
        }
        System.out.println("[Cloud] Connected. Handle=" + connectionHandle);
        return true;
    }

    /**
     * Closes the current relay connection and resets the handle.
     */
    public void disconnect() {
        if (!isConnected()) {
            System.out.println("[Cloud] Not connected. Nothing to disconnect.");
            return;
        }
        // Notify the cloud that the session is ending before releasing the handle.
        logEvent("DISCONNECT", "{\"reason\":\"graceful_shutdown\"}");
        System.out.println("[Cloud] Disconnected (handle=" + connectionHandle + ").");
        connectionHandle = NO_CONNECTION;
    }

    /**
     * @return true if a relay connection is currently active.
     */
    public boolean isConnected() {
        return connectionHandle != NO_CONNECTION;
    }

    // ----------------------------------------------------------------
    // Cloud operations
    // ----------------------------------------------------------------

    /**
     * Fetches the latest configuration JSON from the cloud.
     *
     * @return  Configuration JSON string, or null if none available
     *          or not connected.
     */
    public String fetchLatestConfig() {
        requireConnection("fetchLatestConfig");
        System.out.println("[Cloud] Fetching configuration update...");

        String config = controller.fetchConfigUpdate(connectionHandle);

        if (config == null) {
            System.out.println("[Cloud] No configuration update available.");
        } else {
            System.out.println("[Cloud] Configuration received: " + config);
        }
        return config;
    }

    /**
     * Sends a structured event record to the cloud log.
     *
     * @param eventTag      Short category tag (e.g. "FAULT", "INFO").
     * @param eventPayload  UTF-8 JSON payload.
     * @return              Server-assigned event ID, or -1 on failure.
     */
    public long logEvent(String eventTag, String eventPayload) {
        requireConnection("logEvent");
        System.out.printf("[Cloud] Logging event [%s]: %s%n", eventTag, eventPayload);

        long eventId = controller.logEventToCloud(connectionHandle, eventTag, eventPayload);

        if (eventId < 0) {
            System.err.println("[Cloud] logEventToCloud() failed for tag: " + eventTag);
        } else {
            System.out.println("[Cloud] Event logged. Server event ID: " + eventId);
        }
        return eventId;
    }

    /**
     * Convenience method: fetch a config update, then log an event.
     *
     * @param eventTag      Event category tag.
     * @param eventPayload  Event JSON payload.
     * @return              Server event ID from logEventToCloud().
     */
    public long syncAndLog(String eventTag, String eventPayload) {
        String config = fetchLatestConfig();
        if (config != null) {
            // In a real system you would apply the config here.
            System.out.println("[Cloud] Applying config update...");
        }
        return logEvent(eventTag, eventPayload);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private void requireConnection(String callerName) {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "[Cloud] " + callerName + "() called while not connected. "
                    + "Call connect() first.");
        }
    }
}