public class AuthResult {
    private boolean success;
    private String message;
    private String token;
    private long timestamp;

    public AuthResult(boolean success, String message, String token) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("AuthResult{success=%s, message='%s', token='%s', timestamp=%d}",
                success, message, token, timestamp);
    }
}