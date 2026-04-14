package com.app.init;

import com.app.config.ConfigException;
import com.app.config.ConfigNative;
import com.app.monitor.SystemMonitor;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls application setup and initialization.
 *
 * <p>This class is the <em>sole</em> consumer of {@link ConfigNative}. It is
 * responsible for:</p>
 * <ul>
 *   <li>Loading the configuration file from disk at startup</li>
 *   <li>Validating mandatory configuration entries</li>
 *   <li>Providing strongly-typed accessors for all subsystems</li>
 *   <li>Applying runtime overrides and persisting changes</li>
 *   <li>Reloading the configuration on demand (e.g. SIGHUP)</li>
 *   <li>Coordinating startup health checks against {@link SystemMonitor}</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 *   AppInitializer init = new AppInitializer("/etc/myapp/config.ini");
 *   init.initialize();
 *
 *   String host = init.getServerHost();
 *   int    port = init.getServerPort();
 *   boolean debug = init.isDebugEnabled();
 *
 *   // Later, apply and persist an override:
 *   init.applyOverride("server", "port", "9090");
 * }</pre>
 */
public class AppInitializer {

    private static final Logger LOG = Logger.getLogger(AppInitializer.class.getName());

    // ── mandatory key definitions ─────────────────────────────────────

    private static final String SEC_SERVER  = "server";
    private static final String SEC_APP     = "application";
    private static final String SEC_LOGGING = "logging";
    private static final String SEC_PERF    = "performance";

    private static final String KEY_HOST        = "host";
    private static final String KEY_PORT        = "port";
    private static final String KEY_APP_NAME    = "name";
    private static final String KEY_APP_VERSION = "version";
    private static final String KEY_LOG_LEVEL   = "level";
    private static final String KEY_LOG_PATH    = "path";
    private static final String KEY_DEBUG       = "debug";
    private static final String KEY_MAX_THREADS = "max_threads";
    private static final String KEY_MAX_MEM_MB  = "max_memory_mb";

    // ── POSIX ERE validation patterns ─────────────────────────────────

    private static final String PATTERN_PORT    = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}"
                                                + "|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";
    private static final String PATTERN_HOST    = "^[a-zA-Z0-9._-]{1,253}$";
    private static final String PATTERN_BOOL    = "^(true|false|yes|no|on|off|1|0)$";
    private static final String PATTERN_POSINT  = "^[1-9][0-9]{0,9}$";
    private static final String PATTERN_VERSION = "^[0-9]+\\.[0-9]+\\.[0-9]+$";

    // ── fields ────────────────────────────────────────────────────────

    private final String        configFilePath;
    private final ConfigNative  configNative;
    private final SystemMonitor systemMonitor;
    private volatile boolean    initialized = false;

    // ── constructors ─────────────────────────────────────────────────

    /**
     * Creates an {@code AppInitializer} bound to the given config file path.
     *
     * @param configFilePath path to the INI config file (must not be null or empty)
     */
    public AppInitializer(String configFilePath) {
        this(configFilePath, new ConfigNative(), new SystemMonitor());
    }

    /**
     * Full constructor — package-accessible for testing with injected dependencies.
     */
    AppInitializer(String configFilePath,
                   ConfigNative configNative,
                   SystemMonitor systemMonitor) {
        ConfigNative.checkFilePath(configFilePath);
        this.configFilePath = configFilePath;
        this.configNative   = Objects.requireNonNull(configNative);
        this.systemMonitor  = Objects.requireNonNull(systemMonitor);
    }

    // ── lifecycle ─────────────────────────────────────────────────────

    /**
     * Performs the full initialization sequence:
     * <ol>
     *   <li>Load and parse the config file</li>
     *   <li>Validate all mandatory entries</li>
     *   <li>Run system resource health checks</li>
     * </ol>
     *
     * @throws ConfigException          if the config file is missing, malformed,
     *                                  or a mandatory key fails validation
     * @throws IllegalStateException    if already initialized
     */
    public synchronized void initialize() {
        if (initialized) {
            throw new IllegalStateException("AppInitializer has already been initialized");
        }

        LOG.info("AppInitializer: loading config from " + configFilePath);
        configNative.loadConfig(configFilePath);

        LOG.info("AppInitializer: validating mandatory configuration entries");
        validateMandatoryKeys();

        LOG.info("AppInitializer: running system resource health checks");
        runResourceHealthChecks();

        initialized = true;
        LOG.info(String.format(
            "AppInitializer: initialization complete — app=%s v%s, server=%s:%d, "
          + "logLevel=%s, debug=%b, maxThreads=%d",
            getAppName(), getAppVersion(),
            getServerHost(), getServerPort(),
            getLogLevel(), isDebugEnabled(), getMaxThreads()));
    }

    /**
     * Atomically reloads the configuration from disk, then re-validates.
     *
     * <p>If reloading or validation fails, the previously loaded config remains active.</p>
     *
     * @throws IllegalStateException if not yet initialized
     * @throws ConfigException       if the reload or re-validation fails
     */
    public synchronized void reload() {
        checkInitialized();
        LOG.info("AppInitializer: reloading config from " + configFilePath);
        configNative.reloadConfig();
        validateMandatoryKeys();
        LOG.info("AppInitializer: reload complete — totalKeys=" + configNative.getTotalKeyCount());
    }

    /**
     * Applies a runtime override (sets a key in-memory and persists to disk).
     *
     * @param section section name
     * @param key     key name
     * @param value   new value
     * @throws IllegalStateException    if not yet initialized
     * @throws IllegalArgumentException if any argument is null/empty or value is too long
     * @throws ConfigException          on native error
     */
    public void applyOverride(String section, String key, String value) {
        checkInitialized();
        ConfigNative.checkSection(section);
        ConfigNative.checkKey(key);
        ConfigNative.checkValue(value);

        configNative.setValue(section, key, value);
        configNative.saveConfig(configFilePath);
        LOG.info("AppInitializer: applied override [" + section + "] " + key + "=" + value);
    }

    /**
     * Persists the current in-memory configuration to the loaded file path.
     *
     * @throws IllegalStateException if not yet initialized
     * @throws ConfigException       on write failure
     */
    public void saveConfig() {
        checkInitialized();
        configNative.saveConfig(configFilePath);
        LOG.fine("AppInitializer: config saved to " + configFilePath);
    }

    /**
     * Persists the current in-memory configuration to an alternative path
     * (e.g. for creating a config backup).
     *
     * @param alternativePath destination file path
     * @throws IllegalStateException if not yet initialized
     */
    public void saveConfigAs(String alternativePath) {
        checkInitialized();
        ConfigNative.checkFilePath(alternativePath);
        configNative.saveConfig(alternativePath);
        LOG.fine("AppInitializer: config saved to alternative path: " + alternativePath);
    }

    // ── typed configuration accessors ────────────────────────────────

    /** Returns the server host string (e.g. {@code "localhost"}). */
    public String getServerHost() {
        checkInitialized();
        return configNative.getValue(SEC_SERVER, KEY_HOST, "localhost");
    }

    /** Returns the server port number. */
    public int getServerPort() {
        checkInitialized();
        return configNative.getIntValue(SEC_SERVER, KEY_PORT, 8080);
    }

    /** Returns the application name. */
    public String getAppName() {
        checkInitialized();
        return configNative.getValue(SEC_APP, KEY_APP_NAME, "unknown");
    }

    /** Returns the application version string. */
    public String getAppVersion() {
        checkInitialized();
        return configNative.getValue(SEC_APP, KEY_APP_VERSION, "0.0.0");
    }

    /** Returns the configured log level (e.g. {@code "INFO"}, {@code "DEBUG"}). */
    public String getLogLevel() {
        checkInitialized();
        return configNative.getValue(SEC_LOGGING, KEY_LOG_LEVEL, "INFO");
    }

    /** Returns the log output file path. */
    public String getLogPath() {
        checkInitialized();
        return configNative.getValue(SEC_LOGGING, KEY_LOG_PATH, "/var/log/app.log");
    }

    /** Returns {@code true} if debug mode is enabled. */
    public boolean isDebugEnabled() {
        checkInitialized();
        return configNative.getBoolValue(SEC_APP, KEY_DEBUG, false);
    }

    /** Returns the configured maximum thread pool size. */
    public int getMaxThreads() {
        checkInitialized();
        return configNative.getIntValue(SEC_PERF, KEY_MAX_THREADS, 4);
    }

    /** Returns the configured maximum heap memory in megabytes. */
    public int getMaxMemoryMb() {
        checkInitialized();
        return configNative.getIntValue(SEC_PERF, KEY_MAX_MEM_MB, 512);
    }

    /** Returns {@code true} if this initializer has been successfully initialized. */
    public boolean isInitialized() { return initialized; }

    /** Returns the file path this initializer loaded its config from. */
    public String getConfigFilePath() { return configFilePath; }

    /** Returns the total number of key-value pairs in the loaded config. */
    public int getTotalKeyCount() {
        checkInitialized();
        return configNative.getTotalKeyCount();
    }

    /** Returns all section names present in the loaded config. */
    public String[] getSections() {
        checkInitialized();
        return configNative.listSections();
    }

    /** Returns all key names within the named section. */
    public String[] getKeys(String section) {
        checkInitialized();
        ConfigNative.checkSection(section);
        return configNative.listKeys(section);
    }

    // ── private helpers ───────────────────────────────────────────────

    /**
     * Validates all mandatory config keys and their formats using native regex.
     * Throws {@link ConfigException} on the first validation failure.
     */
    private void validateMandatoryKeys() {
        validateRequired(SEC_SERVER,  KEY_HOST,        PATTERN_HOST,    "server.host");
        validateRequired(SEC_SERVER,  KEY_PORT,        PATTERN_PORT,    "server.port");
        validateRequired(SEC_APP,     KEY_APP_NAME,    ".+",            "application.name");
        validateRequired(SEC_APP,     KEY_APP_VERSION, PATTERN_VERSION, "application.version");
        validateRequired(SEC_LOGGING, KEY_LOG_LEVEL,
                         "^(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)$",       "logging.level");
        validateRequired(SEC_PERF,    KEY_MAX_THREADS, PATTERN_POSINT,  "performance.max_threads");
        validateRequired(SEC_PERF,    KEY_MAX_MEM_MB,  PATTERN_POSINT,  "performance.max_memory_mb");

        // Optional key — only validate format if present
        String debugVal = configNative.getValue(SEC_APP, KEY_DEBUG, null);
        if (debugVal != null && !configNative.validateValue(SEC_APP, KEY_DEBUG, PATTERN_BOOL)) {
            throw new ConfigException(
                "Invalid value for [application] debug — must be true/false/yes/no/on/off/1/0",
                ConfigException.Operation.VALIDATE);
        }
    }

    /**
     * Asserts that a key is present and its value matches {@code pattern}.
     */
    private void validateRequired(String section, String key,
                                   String pattern, String displayName) {
        String val = configNative.getValue(section, key, null);
        if (val == null || val.isEmpty()) {
            throw new ConfigException(
                "Mandatory config key missing: [" + section + "] " + key
                    + " (" + displayName + ")",
                ConfigException.Operation.VALIDATE);
        }
        if (!configNative.validateValue(section, key, pattern)) {
            throw new ConfigException(
                "Config validation failed for [" + section + "] " + key
                    + " (" + displayName + "): value='" + val
                    + "' does not match pattern: " + pattern,
                ConfigException.Operation.VALIDATE);
        }
    }

    /**
     * Checks that available system resources satisfy the configured maximums.
     */
    private void runResourceHealthChecks() {
        long availableMemBytes = systemMonitor.getAvailableMemoryBytes();
        long requiredMemBytes  = (long) getMaxMemoryMb() * 1024 * 1024;

        if (availableMemBytes > 0 && availableMemBytes < requiredMemBytes) {
            LOG.warning(String.format(
                "Available memory (%d MB) is less than configured max_memory_mb (%d MB)",
                availableMemBytes / (1024 * 1024), getMaxMemoryMb()));
        }

        int availCpus  = systemMonitor.getLogicalCpuCount();
        int maxThreads = getMaxThreads();
        if (maxThreads > availCpus * 4) {
            LOG.warning(String.format(
                "max_threads (%d) is more than 4× the available logical CPUs (%d) — "
              + "consider reducing for optimal throughput", maxThreads, availCpus));
        }

        LOG.info(String.format(
            "Resource health check passed — availMem=%d MB, cpus=%d, uptime=%ds",
            availableMemBytes / (1024 * 1024),
            availCpus,
            systemMonitor.getSystemUptimeSeconds()));
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                "AppInitializer has not been initialized — call initialize() first");
        }
    }
}