/**
 * Represents all possible states of a hardware connection.
 */
public enum ConnectionStatus {
    CONNECTED,        // Link is up and active
    DISCONNECTED,     // Clean / intentional disconnect
    TIMEOUT,          // Hardware did not respond within the deadline
    ERROR,            // Unrecoverable hardware error
    RESETTING         // Actively being reset back to default
}