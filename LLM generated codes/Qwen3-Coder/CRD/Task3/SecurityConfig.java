// SecurityConfig.java
public class SecurityConfig {
    public static final int DEFAULT_KEY_LENGTH = 256; // bits
    public static final String DEFAULT_ENCRYPTION_ALGORITHM = "AES";
    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";
    public static final int RSA_KEY_SIZE = 2048; // bits
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    
    private boolean enableLogging = true;
    private boolean requireAuthentication = true;
    private int maxRetries = 3;
    private long sessionTimeoutMs = SESSION_TIMEOUT_MINUTES * 60 * 1000L;

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public boolean isRequireAuthentication() {
        return requireAuthentication;
    }

    public void setRequireAuthentication(boolean requireAuthentication) {
        this.requireAuthentication = requireAuthentication;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(long sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }
}