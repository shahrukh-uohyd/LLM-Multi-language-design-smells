package com.example;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * High-performance configuration parser using JNI for format-agnostic parsing.
 * Supports INI, JSON-like objects, and CSV formats with type inference.
 */
public class ConfigParser {
    
    // Load native library with comprehensive fallback strategy
    static {
        boolean loaded = false;
        String[] libNames = {
            "config_parser",
            "libconfig_parser.so",
            "config_parser.dll",
            "libconfig_parser.dylib"
        };
        
        for (String lib : libNames) {
            try {
                String cleanName = lib
                    .replace("lib", "")
                    .replace(".so", "")
                    .replace(".dylib", "")
                    .replace(".dll", "");
                System.loadLibrary(cleanName);
                loaded = true;
                break;
            } catch (UnsatisfiedLinkError e1) {
                try {
                    System.load("./lib/" + lib);
                    loaded = true;
                    break;
                } catch (UnsatisfiedLinkError e2) {
                    // Continue trying other options
                }
            }
        }
        
        if (!loaded) {
            throw new RuntimeException(
                "Failed to load native library 'config_parser'. Ensure it's in java.library.path\n" +
                "Current path: " + System.getProperty("java.library.path")
            );
        }
    }

    // ===== NATIVE METHODS =====
    
    /**
     * Parse INI configuration format with sections and key-value pairs.
     * Format:
     *   # Comment
     *   [section]
     *   key = value
     *   key2 = "quoted value"
     *
     * @param configRaw Raw configuration string (UTF-8)
     * @return Parsed IniConfig object
     * @throws ConfigParseException on syntax errors
     */
    public native IniConfig parseIni(byte[] configRaw) throws ConfigParseException;
    
    /**
     * Parse JSON-like configuration with nested objects and arrays.
     * Supports: strings, numbers, booleans, null, objects {}, arrays []
     *
     * @param configRaw Raw configuration string (UTF-8)
     * @return Parsed JsonConfig object
     * @throws ConfigParseException on syntax errors
     */
    public native JsonConfig parseJson(byte[] configRaw) throws ConfigParseException;
    
    /**
     * Parse CSV/TSV tabular data with optional header row and type inference.
     * Automatically detects delimiters (comma, tab, semicolon) and data types.
     *
     * @param configRaw Raw CSV data (UTF-8)
     * @param hasHeader Whether first row contains column names
     * @return Parsed CsvTable object
     * @throws ConfigParseException on syntax errors
     */
    public native CsvTable parseCsv(byte[] configRaw, boolean hasHeader) throws ConfigParseException;
    
    /**
     * Validate configuration syntax without full parsing (fast check).
     *
     * @param configRaw Raw configuration string (UTF-8)
     * @param format "ini", "json", or "csv"
     * @return true if syntax is valid
     */
    public native boolean validateSyntax(byte[] configRaw, String format);

    // ===== CONVENIENCE METHODS =====
    
    public IniConfig parseIni(String config) throws ConfigParseException {
        return parseIni(config.getBytes(StandardCharsets.UTF_8));
    }
    
    public JsonConfig parseJson(String config) throws ConfigParseException {
        return parseJson(config.getBytes(StandardCharsets.UTF_8));
    }
    
    public CsvTable parseCsv(String config, boolean hasHeader) throws ConfigParseException {
        return parseCsv(config.getBytes(StandardCharsets.UTF_8), hasHeader);
    }
    
    public boolean validateIni(String config) {
        return validateSyntax(config.getBytes(StandardCharsets.UTF_8), "ini");
    }
    
    public boolean validateJson(String config) {
        return validateSyntax(config.getBytes(StandardCharsets.UTF_8), "json");
    }
    
    public boolean validateCsv(String config) {
        return validateSyntax(config.getBytes(StandardCharsets.UTF_8), "csv");
    }
}

/**
 * Custom exception for configuration parsing errors
 */
@SuppressWarnings("serial")
class ConfigParseException extends Exception {
    public final int lineNumber;
    public final int columnNumber;
    public final String context;
    
    public ConfigParseException(String message, int lineNumber, int columnNumber, String context) {
        super(String.format("%s at line %d, column %d: %s", message, lineNumber, columnNumber, context));
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.context = context;
    }
    
    public ConfigParseException(String message) {
        super(message);
        this.lineNumber = -1;
        this.columnNumber = -1;
        this.context = null;
    }
}