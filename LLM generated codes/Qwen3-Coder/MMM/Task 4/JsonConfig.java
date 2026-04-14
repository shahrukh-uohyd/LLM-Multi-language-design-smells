package com.example;

import java.util.*;

/**
 * Immutable representation of parsed JSON configuration.
 * Supports nested objects, arrays, and primitive types.
 */
public abstract class JsonConfig {
    
    public static JsonConfig NULL = new JsonNull();
    public static JsonConfig TRUE = new JsonBoolean(true);
    public static JsonConfig FALSE = new JsonBoolean(false);
    
    public abstract boolean isNull();
    public abstract boolean isBoolean();
    public abstract boolean isNumber();
    public abstract boolean isString();
    public abstract boolean isObject();
    public abstract boolean isArray();
    
    public boolean getBoolean() { throw new UnsupportedOperationException(); }
    public int getInt() { throw new UnsupportedOperationException(); }
    public double getDouble() { throw new UnsupportedOperationException(); }
    public String getString() { throw new UnsupportedOperationException(); }
    public Map<String, JsonConfig> getObject() { throw new UnsupportedOperationException(); }
    public List<JsonConfig> getArray() { throw new UnsupportedOperationException(); }
    
    // ===== CONCRETE IMPLEMENTATIONS =====
    
    private static class JsonNull extends JsonConfig {
        @Override public boolean isNull() { return true; }
        @Override public String toString() { return "null"; }
    }
    
    private static class JsonBoolean extends JsonConfig {
        private final boolean value;
        JsonBoolean(boolean value) { this.value = value; }
        @Override public boolean isBoolean() { return true; }
        @Override public boolean getBoolean() { return value; }
        @Override public String toString() { return Boolean.toString(value); }
    }
    
    private static class JsonNumber extends JsonConfig {
        private final double value;
        JsonNumber(double value) { this.value = value; }
        @Override public boolean isNumber() { return true; }
        @Override public int getInt() { return (int)value; }
        @Override public double getDouble() { return value; }
        @Override public String toString() { return Double.toString(value); }
    }
    
    private static class JsonString extends JsonConfig {
        private final String value;
        JsonString(String value) { this.value = value; }
        @Override public boolean isString() { return true; }
        @Override public String getString() { return value; }
        @Override public String toString() { return "\"" + value.replace("\"", "\\\"") + "\""; }
    }
    
    private static class JsonObject extends JsonConfig {
        private final Map<String, JsonConfig> values;
        JsonObject(Map<String, JsonConfig> values) {
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }
        @Override public boolean isObject() { return true; }
        @Override public Map<String, JsonConfig> getObject() { return values; }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder("{");
            for (Map.Entry<String, JsonConfig> entry : values.entrySet()) {
                sb.append(String.format("\"%s\":%s, ", entry.getKey(), entry.getValue()));
            }
            if (!values.isEmpty()) sb.setLength(sb.length() - 2);
            sb.append("}");
            return sb.toString();
        }
    }
    
    private static class JsonArray extends JsonConfig {
        private final List<JsonConfig> values;
        JsonArray(List<JsonConfig> values) {
            this.values = Collections.unmodifiableList(new ArrayList<>(values));
        }
        @Override public boolean isArray() { return true; }
        @Override public List<JsonConfig> getArray() { return values; }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder("[");
            for (JsonConfig value : values) {
                sb.append(value).append(", ");
            }
            if (!values.isEmpty()) sb.setLength(sb.length() - 2);
            sb.append("]");
            return sb.toString();
        }
    }
    
    // ===== FACTORY METHODS =====
    
    static JsonConfig nullValue() { return NULL; }
    static JsonConfig booleanValue(boolean value) { return value ? TRUE : FALSE; }
    static JsonConfig numberValue(double value) { return new JsonNumber(value); }
    static JsonConfig stringValue(String value) { return new JsonString(value); }
    static JsonConfig objectValue(Map<String, JsonConfig> values) { return new JsonObject(values); }
    static JsonConfig arrayValue(List<JsonConfig> values) { return new JsonArray(values); }
}