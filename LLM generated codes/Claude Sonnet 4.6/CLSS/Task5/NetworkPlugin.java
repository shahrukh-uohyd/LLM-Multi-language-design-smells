package com.example.plugins;

import java.util.Map;

/**
 * Plugin that handles network operations triggered by native code.
 */
public class NetworkPlugin implements IPlugin {

    private static final String PLUGIN_ID   = "network.operations";
    private static final int    API_VERSION = 1;

    @Override
    public String getPluginId()   { return PLUGIN_ID;   }

    @Override
    public int    getApiVersion() { return API_VERSION; }

    @Override
    public PluginResult execute(PluginContext context) {
        System.out.printf("[NetworkPlugin] Executing: %s%n", context);

        Map<String, String> params = context.getParameters();

        return switch (context.getOperationName()) {
            case "CONNECT"    -> connect(params);
            case "DISCONNECT" -> disconnect(params);
            case "SEND"       -> send(params, context.getFlags());
            default           -> PluginResult.notSupported(
                "Unsupported operation: " + context.getOperationName());
        };
    }

    private PluginResult connect(Map<String, String> params) {
        String host = params.getOrDefault("host", "localhost");
        String port = params.getOrDefault("port", "8080");
        System.out.printf("  → Connecting to %s:%s%n", host, port);
        return PluginResult.success("CONNECT_OK",
                "{\"host\":\"" + host + "\",\"port\":" + port + "}");
    }

    private PluginResult disconnect(Map<String, String> params) {
        System.out.println("  → Disconnecting");
        return PluginResult.success("DISCONNECT_OK");
    }

    private PluginResult send(Map<String, String> params, int flags) {
        String data = params.getOrDefault("data", "");
        System.out.printf("  → Sending data (flags=0x%X): %s%n", flags, data);
        return PluginResult.partial("SEND_PARTIAL",
                "{\"bytesSent\":" + data.length() + "}");
    }
}