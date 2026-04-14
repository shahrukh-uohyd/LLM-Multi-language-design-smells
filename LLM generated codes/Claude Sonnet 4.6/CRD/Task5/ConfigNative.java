package com.app.config;

import java.util.Objects;

/**
 * JNI entrypoint for native configuration management operations.
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li><b>Load / Save</b>   — parse and write INI-style config files natively.</li>
 *   <li><b>Get / Set</b>     — read and write individual key-value pairs per section.</li>
 *   <li><b>Delete</b>        — remove a key or an entire section.</li>
 *   <li><b>List keys</b>     — enumerate all keys within a named section.</li>
 *   <li><b>List sections</b> — enumerate all section names in the loaded config.</li>
 *   <li><b>Validate</b>      — verify a key's value against a regex pattern natively.</li>
 *   <li><b>Reload</b>        — atomically re-read the config file from disk.</li>
 *   <li><b>Typed getters</b> — retrieve values as {@code int}, {@code double},
 *       or {@code boolean} with native type conversion and range validation.</li>
 * </ul>
 *
 * <h2>Config file format (INI)</h2>
 * <pre>
 *   [section]
 *   key = value       ; inline comment
 *   # full-line comment
 * </pre>
 *
 * <p>Callers should prefer the higher-level {@link com.app.init.AppInitializer} API.
 * This class is intentionally package-accessible for testability.</p>
 */
public class ConfigNative {

    // ── constants ─────────────────────────────────────────────────────

    /** Section name used for keys that appear before any {@code [section]} header. */
    public static final String DEFAULT_SECTION = "default";

    /** Maximum allowed byte length of a single key or section name. */
    public static final int MAX_KEY_LENGTH     = 256;

    /** Maximum allowed byte length of a single value string. */
    public static final int MAX_VALUE_LENGTH   = 4096;

    // ── library loading ───────────────────────────────────────────────

    static {
        try {
            System.loadLibrary("config_native");
        } catch (UnsatisfiedLinkError e) {
            throw new ExceptionInInitializerError(
                "Failed to load native config library 'config_native': " + e.getMessage());
        }
    }

    // ── native declarations ───────────────────────────────────────────

    /**
     * Loads and parses an INI-style configuration file from disk.
     *
     * <p>Any previously loaded state is discarded. After a successful
     * call, all get/set/validate operations operate on the new data.</p>
     *
     * @param filePath absolute or relative path to the config file
     * @throws IllegalArgumentException if {@code filePath} is null or empty
     * @throws ConfigException          if the file cannot be opened, read, or parsed
     */
    public native void loadConfig(String filePath);

    /**
     * Saves the current in-memory configuration state to disk.
     *
     * <p>The file is written atomically (write to temp file then rename)
     * to prevent data loss on crash during write.</p>
     *
     * @param filePath path to write the config file; may differ from the load path
     * @throws IllegalArgumentException if {@code filePath} is null or empty
     * @throws ConfigException          if the file cannot be written
     */
    public native void saveConfig(String filePath);

    /**
     * Retrieves the string value for {@code key} in {@code section}.
     *
     * @param section     section name (use {@link #DEFAULT_SECTION} for top-level keys)
     * @param key         key name
     * @param defaultValue value to return if the key is absent (may be null)
     * @return            stored value, or {@code defaultValue} if not found
     * @throws IllegalArgumentException if {@code section} or {@code key} is null/empty
     * @throws ConfigException          if a native error occurs during retrieval
     */
    public native String getValue(String section, String key, String defaultValue);

    /**
     * Sets or overwrites the string value for {@code key} in {@code section}.
     *
     * @param section section name
     * @param key     key name
     * @param value   value to store; must not be null
     * @throws IllegalArgumentException if any argument is null or key/section is empty,
     *                                  or if value exceeds {@link #MAX_VALUE_LENGTH}
     * @throws ConfigException          if a native error occurs during storage
     */
    public native void setValue(String section, String key, String value);

    /**
     * Deletes a specific key from a section.
     *
     * @param section section name
     * @param key     key name
     * @return        {@code true} if the key existed and was removed,
     *                {@code false} if the key was not present
     * @throws IllegalArgumentException if {@code section} or {@code key} is null/empty
     * @throws ConfigException          on native error
     */
    public native boolean deleteKey(String section, String key);

    /**
     * Deletes an entire section and all its keys.
     *
     * @param section section name
     * @return        {@code true} if the section existed and was removed
     * @throws IllegalArgumentException if {@code section} is null/empty
     * @throws ConfigException          on native error
     */
    public native boolean deleteSection(String section);

    /**
     * Returns all key names within {@code section}, in file order.
     *
     * @param section section name
     * @return        array of key name strings; empty array if section has no keys
     * @throws IllegalArgumentException if {@code section} is null/empty
     * @throws ConfigException          on native error
     */
    public native String[] listKeys(String section);

    /**
     * Returns all section names present in the loaded config, in file order.
     *
     * @return array of section name strings; empty array if no sections exist
     * @throws ConfigException on native error
     */
    public native String[] listSections();

    /**
     * Validates the value stored at {@code section}/{@code key} against
     * a POSIX extended regular expression {@code pattern}.
     *
     * @param section section name
     * @param key     key name
     * @param pattern POSIX ERE pattern string (e.g. {@code "^[0-9]{1,5}$"})
     * @return        {@code true} if the stored value matches {@code pattern},
     *                {@code false} if it does not match or the key is absent
     * @throws IllegalArgumentException if any argument is null/empty
     * @throws ConfigException          if the regex cannot be compiled or a native error occurs
     */
    public native boolean validateValue(String section, String key, String pattern);

    /**
     * Atomically reloads the configuration from the file path supplied to
     * the most recent {@link #loadConfig} call.
     *
     * <p>If the reload fails, the previously loaded state is preserved.</p>
     *
     * @throws ConfigException if no file has been loaded yet, or if the reload fails
     */
    public native void reloadConfig();

    /**
     * Retrieves a value as a 32-bit integer.
     *
     * @param section      section name
     * @param key          key name
     * @param defaultValue value returned if the key is absent
     * @return             parsed integer value or {@code defaultValue}
     * @throws IllegalArgumentException if section/key is null/empty
     * @throws ConfigException          if the stored value cannot be parsed as an integer
     */
    public native int getIntValue(String section, String key, int defaultValue);

    /**
     * Retrieves a value as a 64-bit IEEE 754 double.
     *
     * @param section      section name
     * @param key          key name
     * @param defaultValue value returned if the key is absent
     * @return             parsed double value or {@code defaultValue}
     * @throws IllegalArgumentException if section/key is null/empty
     * @throws ConfigException          if the stored value cannot be parsed as a double
     */
    public native double getDoubleValue(String section, String key, double defaultValue);

    /**
     * Retrieves a value as a boolean.
     *
     * <p>Accepts (case-insensitive): {@code true/false}, {@code yes/no},
     * {@code on/off}, {@code 1/0}.</p>
     *
     * @param section      section name
     * @param key          key name
     * @param defaultValue value returned if the key is absent
     * @return             parsed boolean value or {@code defaultValue}
     * @throws IllegalArgumentException if section/key is null/empty
     * @throws ConfigException          if the stored value is not a recognised boolean string
     */
    public native boolean getBoolValue(String section, String key, boolean defaultValue);

    /**
     * Returns the number of keys currently held across all sections.
     */
    public native int getTotalKeyCount();

    /**
     * Returns the file path passed to the most recent {@link #loadConfig} call,
     * or {@code null} if no file has been loaded.
     */
    public native String getLoadedFilePath();

    // ── Java-side validation helpers ──────────────────────────────────

    /** Validates that a section name is non-null and non-empty. */
    static void checkSection(String section) {
        Objects.requireNonNull(section, "section must not be null");
        if (section.isEmpty()) throw new IllegalArgumentException("section must not be empty");
        if (section.length() > MAX_KEY_LENGTH)
            throw new IllegalArgumentException("section name exceeds MAX_KEY_LENGTH");
    }

    /** Validates that a key name is non-null and non-empty. */
    static void checkKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        if (key.isEmpty()) throw new IllegalArgumentException("key must not be empty");
        if (key.length() > MAX_KEY_LENGTH)
            throw new IllegalArgumentException("key name exceeds MAX_KEY_LENGTH");
    }

    /** Validates that a value string is non-null and within length bounds. */
    static void checkValue(String value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.length() > MAX_VALUE_LENGTH)
            throw new IllegalArgumentException("value exceeds MAX_VALUE_LENGTH");
    }

    /** Validates that a file path is non-null and non-empty. */
    static void checkFilePath(String path) {
        Objects.requireNonNull(path, "filePath must not be null");
        if (path.isEmpty()) throw new IllegalArgumentException("filePath must not be empty");
    }
}