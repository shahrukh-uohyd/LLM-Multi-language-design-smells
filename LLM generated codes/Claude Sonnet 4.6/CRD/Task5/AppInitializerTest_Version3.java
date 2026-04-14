package com.app.init;

import com.app.config.ConfigException;
import com.app.config.ConfigNative;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link AppInitializer} and {@link ConfigNative}.
 * Exercises the full Java → JNI → C path.
 *
 * Run after building libconfig_native:
 *   mvn test  -Djava.library.path=lib
 *   gradle test
 */
class AppInitializerTest {

    @TempDir Path tempDir;

    private static final String VALID_CONFIG = """
            [server]
            host = localhost
            port = 8080

            [application]
            name    = MyApp
            version = 1.2.3
            debug   = false

            [logging]
            level = INFO
            path  = /var/log/app.log

            [performance]
            max_threads   = 8
            max_memory_mb = 512
            """;

    // ── helpers ───────────────────────────────────────────────────────

    private Path writeConfig(String content) throws IOException {
        Path f = tempDir.resolve("app.ini");
        Files.writeString(f, content);
        return f;
    }

    // ── load and initialize ───────────────────────────────────────────

    @Test
    void initialize_validConfig_succeeds() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        assertDoesNotThrow(init::initialize);
        assertTrue(init.isInitialized());
    }

    @Test
    void initialize_twice_throwsIllegalStateException() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        assertThrows(IllegalStateException.class, init::initialize);
    }

    @Test
    void initialize_missingFile_throwsConfigException() {
        AppInitializer init = new AppInitializer("/nonexistent/config.ini");
        assertThrows(ConfigException.class, init::initialize);
    }

    // ── typed accessors ───────────────────────────────────────────────

    @Test
    void getServerHost_returnsCorrectValue() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        assertEquals("localhost", init.getServerHost());
    }

    @Test
    void getServerPort_returnsCorrectInt() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        assertEquals(8080, init.getServerPort());
    }

    @Test
    void getAppVersion_returnsVersionString() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        assertEquals("1.2.3", init.getAppVersion());
    }

    @Test
    void isDebugEnabled_returnsFalse() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        assertFalse(init.isDebugEnabled());
    }

    @Test
    void getMaxThreads_returnsCorrectValue() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        assertEquals(8, init.getMaxThreads());
    }

    // ── validation failures ───────────────────────────────────────────

    @Test
    void initialize_missingMandatoryKey_throwsConfigException() throws IOException {
        String bad = VALID_CONFIG.replace("host = localhost\n", "");
        Path cfg = writeConfig(bad);
        AppInitializer init = new AppInitializer(cfg.toString());
        ConfigException ex = assertThrows(ConfigException.class, init::initialize);
        assertEquals(ConfigException.Operation.VALIDATE, ex.getOperation());
    }

    @Test
    void initialize_invalidPort_throwsConfigException() throws IOException {
        String bad = VALID_CONFIG.replace("port = 8080", "port = 99999");
        Path cfg = writeConfig(bad);
        AppInitializer init = new AppInitializer(cfg.toString());
        assertThrows(ConfigException.class, init::initialize);
    }

    @Test
    void initialize_invalidVersion_throwsConfigException() throws IOException {
        String bad = VALID_CONFIG.replace("version = 1.2.3", "version = not-a-version");
        Path cfg = writeConfig(bad);
        AppInitializer init = new AppInitializer(cfg.toString());
        assertThrows(ConfigException.class, init::initialize);
    }

    // ── override and save ─────────────────────────────────────────────

    @Test
    void applyOverride_persistsToDisk() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();

        init.applyOverride("server", "port", "9090");
        assertEquals(9090, init.getServerPort());

        // Reload in a fresh instance to confirm disk persistence
        AppInitializer init2 = new AppInitializer(cfg.toString());
        init2.initialize();
        assertEquals(9090, init2.getServerPort());
    }

    @Test
    void saveConfigAs_writesAlternativePath() throws IOException {
        Path cfg  = writeConfig(VALID_CONFIG);
        Path copy = tempDir.resolve("backup.ini");
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        init.saveConfigAs(copy.toString());
        assertTrue(Files.exists(copy));
        assertTrue(Files.size(copy) > 0);
    }

    // ── reload ────────────────────────────────────────────────────────

    @Test
    void reload_picksUpDiskChanges() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        assertEquals(8080, init.getServerPort());

        // Modify file on disk
        String updated = VALID_CONFIG.replace("port = 8080", "port = 7070");
        Files.writeString(cfg, updated);

        init.reload();
        assertEquals(7070, init.getServerPort());
    }

    @Test
    void reload_beforeInitialize_throwsIllegalStateException() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        assertThrows(IllegalStateException.class, init::reload);
    }

    // ── section / key listing ─────────────────────────────────────────

    @Test
    void getSections_returnsAllSections() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        String[] sections = init.getSections();
        assertNotNull(sections);
        assertTrue(sections.length >= 4);
    }

    @Test
    void getKeys_returnsKeysForSection() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        String[] keys = init.getKeys("server");
        assertNotNull(keys);
        assertEquals(2, keys.length); // host, port
    }

    @Test
    void getTotalKeyCount_isPositive() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        AppInitializer init = new AppInitializer(cfg.toString());
        init.initialize();
        assertTrue(init.getTotalKeyCount() >= 9);
    }

    // ── ConfigNative direct tests ─────────────────────────────────────

    @Test
    void configNative_setAndGet_roundTrip() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        ConfigNative cn = new ConfigNative();
        cn.loadConfig(cfg.toString());

        cn.setValue("server", "host", "192.168.1.1");
        assertEquals("192.168.1.1", cn.getValue("server", "host", null));
    }

    @Test
    void configNative_deleteKey_returnsTrue() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        ConfigNative cn = new ConfigNative();
        cn.loadConfig(cfg.toString());

        assertTrue(cn.deleteKey("server", "port"));
        assertNull(cn.getValue("server", "port", null));
    }

    @Test
    void configNative_deleteSection_removesAllKeys() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        ConfigNative cn = new ConfigNative();
        cn.loadConfig(cfg.toString());

        assertTrue(cn.deleteSection("logging"));
        assertEquals(0, cn.listKeys("logging").length);
    }

    @Test
    void configNative_validateValue_matchesPattern() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        ConfigNative cn = new ConfigNative();
        cn.loadConfig(cfg.toString());

        assertTrue(cn.validateValue("server",  "port",  "^[0-9]+$"));
        assertFalse(cn.validateValue("server", "host",  "^[0-9]+$")); // "localhost" is not digits
    }

    @Test
    void configNative_getIntValue_parsesCorrectly() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        ConfigNative cn = new ConfigNative();
        cn.loadConfig(cfg.toString());
        assertEquals(8080, cn.getIntValue("server", "port", -1));
    }

    @Test
    void configNative_getBoolValue_parsesCorrectly() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        ConfigNative cn = new ConfigNative();
        cn.loadConfig(cfg.toString());
        assertFalse(cn.getBoolValue("application", "debug", true));
    }

    @Test
    void configNative_getLoadedFilePath_returnsPath() throws IOException {
        Path cfg = writeConfig(VALID_CONFIG);
        ConfigNative cn = new ConfigNative();
        cn.loadConfig(cfg.toString());
        assertEquals(cfg.toString(), cn.getLoadedFilePath());
    }

    @Test
    void configNative_nullFilePath_throwsIAE() {
        ConfigNative cn = new ConfigNative();
        assertThrows(NullPointerException.class, () -> cn.loadConfig(null));
    }
}