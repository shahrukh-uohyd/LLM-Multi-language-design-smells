package com.hardware.native_utils;

/**
 * Retrieves the unique hardware identifier of the host machine.
 *
 * <p>The exact source of the ID is platform-dependent:</p>
 * <ul>
 *   <li><strong>Linux</strong>  – reads {@code /etc/machine-id} or the
 *       motherboard UUID via {@code dmidecode}.</li>
 *   <li><strong>Windows</strong> – queries the
 *       {@code MachineGuid} registry value or calls
 *       {@code GetAdaptersInfo} for the primary NIC MAC address.</li>
 *   <li><strong>macOS</strong>  – uses {@code IOPlatformSerialNumber}
 *       from {@code IOKit}.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 *   HardwareIdProvider provider = new HardwareIdProvider();
 *   String platform  = provider.getPlatformInfo();
 *   String hardwareId = provider.getHardwareId();
 *   System.out.println(platform + " -> " + hardwareId);
 * }</pre>
 * </p>
 */
public class HardwareIdProvider {

    static {
        // Loads libhardware_id.so (Linux/macOS) or hardware_id.dll (Windows)
        System.loadLibrary("hardware_id");
    }

    /**
     * Retrieves the unique hardware identifier of the host machine.
     *
     * <p>The returned string is a stable, non-null identifier that remains
     * constant across reboots for the same physical hardware (e.g., a UUID
     * such as {@code "550e8400-e29b-41d4-a716-446655440000"}).</p>
     *
     * @return a non-null, non-empty hardware ID string
     * @throws RuntimeException if the native layer cannot determine the ID
     *                          (e.g., insufficient permissions, unsupported platform)
     */
    public native String getHardwareId();

    /**
     * Retrieves a human-readable platform/OS description string from the
     * native layer.
     *
     * <p>Examples: {@code "Linux 6.8.0-x86_64"}, {@code "Windows 11 Pro 22H2"},
     * {@code "macOS 14.4 Sonoma arm64"}.</p>
     *
     * <p>This complements {@link #getHardwareId()} by providing context about
     * the environment in which the ID was collected.</p>
     *
     * @return a non-null platform description string
     */
    public native String getPlatformInfo();

    // -----------------------------------------------------------------------
    // Convenience wrapper
    // -----------------------------------------------------------------------

    /**
     * Returns a formatted summary combining the platform info and hardware ID.
     *
     * @return a string in the form {@code "[platform] | HW-ID: [hardwareId]"}
     */
    public String getFormattedId() {
        return String.format("[%s] | HW-ID: %s", getPlatformInfo(), getHardwareId());
    }
}