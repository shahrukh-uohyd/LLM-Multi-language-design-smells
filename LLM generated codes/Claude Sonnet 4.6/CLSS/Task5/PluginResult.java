package com.example.plugins;

/**
 * Encapsulates the outcome of a plugin execution, returned to native C++ code.
 * The native layer reads the code and message fields back through JNI.
 */
public final class PluginResult {

    /** Standard result codes understood by the native layer. */
    public static final int CODE_SUCCESS       = 0;
    public static final int CODE_FAILURE       = 1;
    public static final int CODE_NOT_SUPPORTED = 2;
    public static final int CODE_PARTIAL       = 3;

    private final int    code;
    private final String message;
    private final String payload;   // Optional serialised data for native

    private PluginResult(int code, String message, String payload) {
        this.code    = code;
        this.message = message;
        this.payload = payload;
    }

    // --- Factory methods ---

    public static PluginResult success(String message) {
        return new PluginResult(CODE_SUCCESS, message, null);
    }

    public static PluginResult success(String message, String payload) {
        return new PluginResult(CODE_SUCCESS, message, payload);
    }

    public static PluginResult failure(String message) {
        return new PluginResult(CODE_FAILURE, message, null);
    }

    public static PluginResult notSupported(String message) {
        return new PluginResult(CODE_NOT_SUPPORTED, message, null);
    }

    public static PluginResult partial(String message, String payload) {
        return new PluginResult(CODE_PARTIAL, message, payload);
    }

    // --- Accessors called by native code via JNI ---

    public int    getCode()    { return code;    }
    public String getMessage() { return message; }
    public String getPayload() { return payload; }

    @Override
    public String toString() {
        return "PluginResult{code=" + code
             + ", message='" + message + "'"
             + (payload != null ? ", payload='" + payload + "'" : "")
             + "}";
    }
}