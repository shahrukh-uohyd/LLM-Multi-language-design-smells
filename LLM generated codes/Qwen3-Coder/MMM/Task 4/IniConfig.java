package com.example;

import java.util.*;

/**
 * Immutable representation of parsed INI configuration.
 * Supports multiple sections with key-value pairs and comments.
 */
public final class IniConfig {
    private final Map<String, Section> sections;
    private final Section globalSection; // For keys outside sections
    
    public static class Section {
        private final Map<String, String> values;
        private final Map<String, String> comments; // Key -> comment
        
        Section(Map<String, String> values, Map<String, String> comments) {
            this.values = Collections.unmodifiableMap(new HashMap<>(values));
            this.comments = Collections.unmodifiableMap(new HashMap<>(comments));
        }
        
        public String get(String key) {
            return values.get(key);
        }
        
        public String get(String key, String defaultValue) {
            return values.getOrDefault(key, defaultValue);
        }
        
        public int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(values.get(key));
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        public double getDouble(String key, double defaultValue) {
            try {
                return Double.parseDouble(values.get(key));
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        public boolean getBoolean(String key, boolean defaultValue) {
            String val = values.get(key);
            if (val == null) return defaultValue;
            val = val.trim().toLowerCase();
            return "true".equals(val) || "yes".equals(val) || "1".equals(val);
        }
        
        public String getComment(String key) {
            return comments.get(key);
        }
        
        public Set<String> keySet() {
            return values.keySet();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Section{");
            for (Map.Entry<String, String> entry : values.entrySet()) {
                sb.append(String.format("%s=\"%s\", ", entry.getKey(), entry.getValue()));
            }
            if (!values.isEmpty()) sb.setLength(sb.length() - 2);
            sb.append("}");
            return sb.toString();
        }
    }
    
    IniConfig(Map<String, Section> sections, Section globalSection) {
        this.sections = Collections.unmodifiableMap(new HashMap<>(sections));
        this.globalSection = globalSection;
    }
    
    public Section getSection(String name) {
        return sections.getOrDefault(name, new Section(new HashMap<>(), new HashMap<>()));
    }
    
    public Section getGlobalSection() {
        return globalSection;
    }
    
    public Set<String> sectionNames() {
        return sections.keySet();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("IniConfig{\n");
        if (!globalSection.keySet().isEmpty()) {
            sb.append("  [GLOBAL]: ").append(globalSection).append("\n");
        }
        for (Map.Entry<String, Section> entry : sections.entrySet()) {
            sb.append(String.format("  [%s]: %s\n", entry.getKey(), entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }
}