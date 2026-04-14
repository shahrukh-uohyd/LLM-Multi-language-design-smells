package com.example.plugins;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable execution context passed from native C++ code to a plugin.
 * Carries the operation name, a parameter map, and a bitmask of
 * control flags originating from the native layer.
 */
public final class PluginContext {

    private final String              operationName;
    private final Map<String, String> parameters;
    private final int                 flags;

    public PluginContext(String operationName,
                         Map<String, String> parameters,
                         int flags)
    {
        this.operationName = operationName;
        this.parameters    = Collections.unmodifiableMap(
                                 new HashMap<>(parameters));
        this.flags         = flags;
    }

    public String              getOperationName() { return operationName; }
    public Map<String, String> getParameters()    { return parameters;    }
    public int                 getFlags()          { return flags;         }

    @Override
    public String toString() {
        return "PluginContext{op='" + operationName
             + "', params=" + parameters
             + ", flags=0x" + Integer.toHexString(flags) + "}";
    }
}