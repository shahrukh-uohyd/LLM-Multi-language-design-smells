package com.app.storage;

import com.app.compression.CompressionException;
import com.app.compression.CompressionNative;

import java.io.*;
import java.nio.file.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages data storage with transparent compression support.
 *
 * <p>This class is the primary consumer of {@link CompressionNative} and is
 * responsible for persisting, retrieving, compressing, and decompressing
 * application data. Other classes across the application may also call
 * {@link #compress} / {@link #decompress} directly for in-memory use.</p>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 *   DataStorage storage = new DataStorage("/var/app/data", true);
 *
 *   // Store compressed
 *   storage.store("user-profile", profileBytes);
 *
 *   // Retrieve and decompress automatically
 *   byte[] profile = storage.retrieve("user-profile");
 *
 *   // In-memory compress/decompress without I/O
 *   byte[] compressed = storage.compress(rawBytes);
 *   byte[] original   = storage.decompress(compressed, rawBytes.length);
 * }</pre>
 */
public class DataStorage {

    private static final Logger LOG = Logger.getLogger(DataStorage.class.getName());

    /** File extension appended to compressed storage files. */
    private static final String COMPRESSED_EXT = ".zlib";

    /** File extension for uncompressed storage files. */
    private static final String RAW_EXT = ".dat";

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------

    private final Path storageRoot;
    private final boolean compressionEnabled;
    private final int defaultCompressionLevel;
    private final CompressionNative compressionNative;

    // ---------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------

    /**
     * Creates a {@code DataStorage} backed by the given directory.
     *
     * @param storageRootPath   filesystem path to the storage directory (created if absent)
     * @param compressionEnabled whether stored data should be compressed automatically
     */
    public DataStorage(String storageRootPath, boolean compressionEnabled) {
        this(storageRootPath, compressionEnabled, CompressionNative.LEVEL_DEFAULT);
    }

    /**
     * Creates a {@code DataStorage} with a specific default compression level.
     *
     * @param storageRootPath       filesystem path to the storage directory
     * @param compressionEnabled    whether stored data should be compressed automatically
     * @param defaultCompressionLevel zlib compression level for all write operations
     */
    public DataStorage(String storageRootPath,
                       boolean compressionEnabled,
                       int defaultCompressionLevel) {
        Objects.requireNonNull(storageRootPath, "storageRootPath must not be null");
        CompressionNative.checkLevel(defaultCompressionLevel);

        this.storageRoot = Paths.get(storageRootPath);
        this.compressionEnabled = compressionEnabled;
        this.defaultCompressionLevel = defaultCompressionLevel;
        this.compressionNative = new CompressionNative();

        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root: " + storageRootPath, e);
        }

        LOG.info(String.format(
            "DataStorage initialised — root=%s, compression=%b, level=%d, zlib=%s",
            storageRoot, compressionEnabled, defaultCompressionLevel,
            compressionNative.zlibVersion()));
    }

    // ---------------------------------------------------------------
    // Public storage API
    // ---------------------------------------------------------------

    /**
     * Persists {@code data} under the given {@code key}.
     * If compression is enabled the data is compressed before writing to disk.
     *
     * @param key  logical name for the stored entry; must be a valid filename component
     * @param data bytes to store; must not be null or empty
     * @throws UncheckedIOException  on any I/O failure
     * @throws CompressionException  if compression fails
     */
    public void store(String key, byte[] data) {
        validateKey(key);
        CompressionNative.checkData(data, "data");

        byte[] toWrite;
        String extension;

        if (compressionEnabled) {
            toWrite = compress(data);
            extension = COMPRESSED_EXT;
            LOG.fine(String.format("store key=%s original=%d compressed=%d (%.1f%%)",
                key, data.length, toWrite.length,
                (1.0 - (double) toWrite.length / data.length) * 100));
        } else {
            toWrite = data;
            extension = RAW_EXT;
        }

        Path target = storageRoot.resolve(key + extension);
        try {
            Files.write(target, toWrite,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write storage entry: " + key, e);
        }
    }

    /**
     * Retrieves and returns the data stored under {@code key}.
     * Decompresses automatically if the stored file is compressed.
     *
     * @param key logical name of the stored entry
     * @return raw (decompressed) bytes
     * @throws NoSuchFileException   (wrapped in {@link UncheckedIOException}) if key not found
     * @throws CompressionException  if decompression fails
     */
    public byte[] retrieve(String key) {
        validateKey(key);

        Path compressed = storageRoot.resolve(key + COMPRESSED_EXT);
        Path raw        = storageRoot.resolve(key + RAW_EXT);

        try {
            if (Files.exists(compressed)) {
                byte[] compressedBytes = Files.readAllBytes(compressed);
                LOG.fine("retrieve key=" + key + " mode=compressed size=" + compressedBytes.length);
                return decompress(compressedBytes, -1);
            } else if (Files.exists(raw)) {
                LOG.fine("retrieve key=" + key + " mode=raw");
                return Files.readAllBytes(raw);
            } else {
                throw new UncheckedIOException(
                    new FileNotFoundException("No storage entry found for key: " + key));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read storage entry: " + key, e);
        }
    }

    /**
     * Deletes the stored entry for {@code key} (either extension).
     *
     * @param key logical name of the stored entry
     * @return {@code true} if at least one file was deleted, {@code false} if key was absent
     */
    public boolean delete(String key) {
        validateKey(key);
        boolean deleted = false;
        for (String ext : new String[]{COMPRESSED_EXT, RAW_EXT}) {
            try {
                deleted |= Files.deleteIfExists(storageRoot.resolve(key + ext));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to delete storage entry: " + key, e);
            }
        }
        return deleted;
    }

    /**
     * Returns {@code true} if an entry exists for {@code key}.
     */
    public boolean exists(String key) {
        validateKey(key);
        return Files.exists(storageRoot.resolve(key + COMPRESSED_EXT))
            || Files.exists(storageRoot.resolve(key + RAW_EXT));
    }

    // ---------------------------------------------------------------
    // Public in-memory compression API (used by other classes too)
    // ---------------------------------------------------------------

    /**
     * Compresses {@code data} using the storage's default compression level.
     *
     * @param data raw bytes to compress
     * @return compressed bytes
     * @throws CompressionException on failure
     */
    public byte[] compress(byte[] data) {
        return compress(data, defaultCompressionLevel);
    }

    /**
     * Compresses {@code data} at the specified compression level.
     *
     * @param data             raw bytes to compress
     * @param compressionLevel zlib level in [-1, 9]
     * @return compressed bytes
     * @throws CompressionException on failure
     */
    public byte[] compress(byte[] data, int compressionLevel) {
        CompressionNative.checkData(data, "data");
        CompressionNative.checkLevel(compressionLevel);
        return compressionNative.compress(data, compressionLevel);
    }

    /**
     * Decompresses {@code compressedData}.
     *
     * @param compressedData zlib-compressed bytes
     * @param originalSize   expected decompressed size, or {@code -1} if unknown
     * @return decompressed bytes
     * @throws CompressionException if the data is corrupt or decompression fails
     */
    public byte[] decompress(byte[] compressedData, int originalSize) {
        CompressionNative.checkData(compressedData, "compressedData");
        return compressionNative.decompress(compressedData, originalSize);
    }

    /**
     * Returns an upper bound on the compressed size for {@code inputSize} bytes.
     * Useful for callers who want to pre-allocate buffers.
     */
    public long compressBound(long inputSize) {
        if (inputSize < 0) throw new IllegalArgumentException("inputSize must be >= 0");
        return compressionNative.compressBound(inputSize);
    }

    // ---------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------

    /** Returns the filesystem root this storage instance writes to. */
    public Path getStorageRoot() { return storageRoot; }

    /** Returns {@code true} if automatic compression is enabled. */
    public boolean isCompressionEnabled() { return compressionEnabled; }

    /** Returns the default zlib compression level used by this instance. */
    public int getDefaultCompressionLevel() { return defaultCompressionLevel; }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private static void validateKey(String key) {
        Objects.requireNonNull(key, "key must not be null");
        if (key.isEmpty()) throw new IllegalArgumentException("key must not be empty");
        if (key.contains("/") || key.contains("\\") || key.contains("..")) {
            throw new IllegalArgumentException("key must not contain path separators: " + key);
        }
    }
}