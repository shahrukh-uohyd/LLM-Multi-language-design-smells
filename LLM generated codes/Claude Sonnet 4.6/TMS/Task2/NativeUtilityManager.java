package com.hardware.native_utils;

import java.nio.ByteBuffer;
import sun.nio.ch.DirectBuffer; // for direct buffer address access

/**
 * Central manager that exposes all three native utility features as a
 * unified, easy-to-use API.
 *
 * <p>This class composes {@link FileChecksumCalculator},
 * {@link NativeMemoryCleaner}, and {@link HardwareIdProvider}, initialises
 * them in the correct order, and provides high-level convenience methods.</p>
 */
public class NativeUtilityManager {

    private final FileChecksumCalculator checksumCalculator;
    private final NativeMemoryCleaner    memoryCleaner;
    private final HardwareIdProvider     hardwareIdProvider;

    private boolean initialised = false;

    public NativeUtilityManager() {
        this.checksumCalculator = new FileChecksumCalculator();
        this.memoryCleaner      = new NativeMemoryCleaner();
        this.hardwareIdProvider = new HardwareIdProvider();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Initialises all native utilities.
     * Must be called once before using any other method.
     */
    public void initialise() {
        checksumCalculator.initChecksumEngine();
        initialised = true;
        System.out.println("NativeUtilityManager initialised successfully.");
        System.out.println("Running on: " + hardwareIdProvider.getPlatformInfo());
    }

    // -----------------------------------------------------------------------
    // Checksum
    // -----------------------------------------------------------------------

    /**
     * Computes the checksum of a file at the given path.
     *
     * @param filePath absolute or relative path to the target file
     * @return hexadecimal checksum string
     * @throws IllegalStateException if {@link #initialise()} has not been called
     */
    public String computeFileChecksum(String filePath) {
        ensureInitialised();
        return checksumCalculator.computeChecksum(filePath);
    }

    /**
     * Verifies whether a file's live checksum matches an expected value.
     *
     * @param filePath         path to the file
     * @param expectedChecksum expected hexadecimal checksum
     * @return {@code true} if the file matches; {@code false} otherwise
     */
    public boolean verifyFileChecksum(String filePath, String expectedChecksum) {
        ensureInitialised();
        return checksumCalculator.verifyChecksum(filePath, expectedChecksum);
    }

    // -----------------------------------------------------------------------
    // Memory Clearing
    // -----------------------------------------------------------------------

    /**
     * Securely zeroes the contents of a direct {@link ByteBuffer}.
     *
     * @param buffer a direct ByteBuffer whose memory should be cleared
     * @throws IllegalArgumentException if the buffer is not a direct buffer
     */
    public void clearDirectBuffer(ByteBuffer buffer) {
        ensureInitialised();
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException(
                "Only direct ByteBuffers backed by off-heap memory can be cleared natively.");
        }
        long address = ((DirectBuffer) buffer).address();
        memoryCleaner.secureClear(address, buffer.capacity());
        System.out.println("Direct buffer (" + buffer.capacity() + " bytes) securely cleared.");
    }

    // -----------------------------------------------------------------------
    // Hardware ID
    // -----------------------------------------------------------------------

    /**
     * Returns the unique hardware ID of this machine.
     *
     * @return hardware ID string
     */
    public String getHardwareId() {
        return hardwareIdProvider.getHardwareId();
    }

    /**
     * Returns a formatted platform + hardware ID summary.
     *
     * @return formatted ID string
     */
    public String getFormattedHardwareId() {
        return hardwareIdProvider.getFormattedId();
    }

    // -----------------------------------------------------------------------
    // Entry point (smoke test)
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        NativeUtilityManager manager = new NativeUtilityManager();
        manager.initialise();

        // --- Hardware ID ---
        System.out.println("Hardware ID : " + manager.getHardwareId());
        System.out.println("Full ID     : " + manager.getFormattedHardwareId());

        // --- File Checksum ---
        String testFile = "/var/data/sample.bin";
        String checksum = manager.computeFileChecksum(testFile);
        System.out.println("Checksum    : " + checksum);

        // --- Memory Clear ---
        ByteBuffer secretBuffer = ByteBuffer.allocateDirect(128);
        secretBuffer.put("sensitive-data-placeholder".getBytes());
        manager.clearDirectBuffer(secretBuffer);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void ensureInitialised() {
        if (!initialised) {
            throw new IllegalStateException(
                "NativeUtilityManager has not been initialised. Call initialise() first.");
        }
    }
}