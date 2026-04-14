package com.app.storage;

import com.app.compression.CompressionException;
import com.app.compression.CompressionNative;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link DataStorage} — exercises the full Java→JNI→zlib path.
 *
 * <p>Run with: {@code mvn test} or {@code gradle test} after building the native lib.</p>
 */
class DataStorageTest {

    @TempDir
    Path tempDir;

    private DataStorage storage;
    private DataStorage storageNoCompression;

    @BeforeEach
    void setUp() {
        storage            = new DataStorage(tempDir.resolve("compressed").toString(), true);
        storageNoCompression = new DataStorage(tempDir.resolve("raw").toString(), false);
    }

    // ── compressBound ────────────────────────────────────────────────

    @Test
    void compressBound_returnsPositive() {
        assertTrue(storage.compressBound(1024) > 1024,
            "compressBound should exceed the input size");
    }

    @Test
    void compressBound_zeroInput() {
        assertTrue(storage.compressBound(0) >= 0);
    }

    @Test
    void compressBound_negativeInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> storage.compressBound(-1));
    }

    // ── round-trip: compress → decompress ───────────────────────────

    @Test
    void roundTrip_smallText() {
        byte[] original = "Hello, JNI compression!".getBytes(StandardCharsets.UTF_8);
        byte[] compressed   = storage.compress(original);
        byte[] decompressed = storage.decompress(compressed, original.length);
        assertArrayEquals(original, decompressed);
    }

    @Test
    void roundTrip_largeBinary() {
        byte[] original = new byte[256 * 1024]; // 256 KB
        Arrays.fill(original, (byte) 0xAB);
        byte[] compressed   = storage.compress(original);
        byte[] decompressed = storage.decompress(compressed, original.length);
        assertArrayEquals(original, decompressed);
        // highly compressible data should shrink significantly
        assertTrue(compressed.length < original.length / 10,
            "expected high compression ratio for repeated bytes");
    }

    @Test
    void roundTrip_unknownOriginalSize() {
        byte[] original = "auto-grow buffer test".getBytes(StandardCharsets.UTF_8);
        byte[] compressed   = storage.compress(original);
        byte[] decompressed = storage.decompress(compressed, -1);
        assertArrayEquals(original, decompressed);
    }

    @Test
    void roundTrip_allCompressionLevels() {
        byte[] original = "level test payload".getBytes(StandardCharsets.UTF_8);
        int[] levels = {
            CompressionNative.LEVEL_DEFAULT,
            CompressionNative.LEVEL_NO_COMPRESSION,
            CompressionNative.LEVEL_BEST_SPEED,
            CompressionNative.LEVEL_BEST_COMPRESSION
        };
        for (int level : levels) {
            byte[] compressed   = storage.compress(original, level);
            byte[] decompressed = storage.decompress(compressed, -1);
            assertArrayEquals(original, decompressed,
                "round-trip failed at level " + level);
        }
    }

    // ── store / retrieve / delete / exists ──────────────────────────

    @Test
    void storeAndRetrieve_withCompression() {
        byte[] data = "persistent compressed data".getBytes(StandardCharsets.UTF_8);
        storage.store("my-key", data);
        assertTrue(storage.exists("my-key"));
        assertArrayEquals(data, storage.retrieve("my-key"));
    }

    @Test
    void storeAndRetrieve_withoutCompression() {
        byte[] data = "raw data".getBytes(StandardCharsets.UTF_8);
        storageNoCompression.store("raw-key", data);
        assertArrayEquals(data, storageNoCompression.retrieve("raw-key"));
    }

    @Test
    void delete_removesEntry() {
        storage.store("to-delete", new byte[]{1, 2, 3});
        assertTrue(storage.delete("to-delete"));
        assertFalse(storage.exists("to-delete"));
    }

    @Test
    void retrieve_missingKeyThrows() {
        assertThrows(Exception.class, () -> storage.retrieve("nonexistent"));
    }

    // ── error handling ───────────────────────────────────────────────

    @Test
    void compress_nullDataThrows() {
        assertThrows(IllegalArgumentException.class, () -> storage.compress(null));
    }

    @Test
    void decompress_corruptDataThrows() {
        byte[] garbage = {0x00, 0x01, 0x02, 0x03, 0x04};
        assertThrows(CompressionException.class, () -> storage.decompress(garbage, -1));
    }

    @Test
    void compress_invalidLevelThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> storage.compress(new byte[]{1}, 10));
    }

    // ── zlibVersion ─────────────────────────────────────────────────

    @Test
    void zlibVersion_isNonEmpty() {
        CompressionNative cn = new CompressionNative();
        String version = cn.zlibVersion();
        assertNotNull(version);
        assertFalse(version.isBlank());
        System.out.println("zlib version: " + version);
    }
}