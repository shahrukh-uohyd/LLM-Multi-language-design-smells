package com.example.plugins;

import java.util.Map;

/**
 * Plugin that handles image-processing operations triggered by native code.
 */
public class ImageProcessingPlugin implements IPlugin {

    private static final String PLUGIN_ID   = "image.processing";
    private static final int    API_VERSION = 2;

    @Override
    public String getPluginId()   { return PLUGIN_ID;   }

    @Override
    public int    getApiVersion() { return API_VERSION; }

    @Override
    public PluginResult execute(PluginContext context) {
        System.out.printf("[ImageProcessingPlugin] Executing: %s%n", context);

        Map<String, String> params = context.getParameters();

        return switch (context.getOperationName()) {
            case "RESIZE"    -> resize(params, context.getFlags());
            case "COMPRESS"  -> compress(params, context.getFlags());
            case "GRAYSCALE" -> grayscale(params, context.getFlags());
            default          -> PluginResult.notSupported(
                "Unsupported operation: " + context.getOperationName());
        };
    }

    private PluginResult resize(Map<String, String> params, int flags) {
        String width  = params.getOrDefault("width",  "0");
        String height = params.getOrDefault("height", "0");
        System.out.printf("  → Resizing to %sx%s (flags=0x%X)%n",
                width, height, flags);
        return PluginResult.success("RESIZE_OK",
                "{\"width\":" + width + ",\"height\":" + height + "}");
    }

    private PluginResult compress(Map<String, String> params, int flags) {
        String quality = params.getOrDefault("quality", "85");
        System.out.printf("  → Compressing at quality=%s%n", quality);
        return PluginResult.success("COMPRESS_OK",
                "{\"quality\":" + quality + "}");
    }

    private PluginResult grayscale(Map<String, String> params, int flags) {
        System.out.println("  → Converting to grayscale");
        return PluginResult.success("GRAYSCALE_OK");
    }
}