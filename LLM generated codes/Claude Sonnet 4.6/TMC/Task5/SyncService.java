/**
 * SyncService
 *
 * Manages the full Bluetooth lifecycle: stack initialisation, BLE
 * device discovery, and encrypted data-packet transmission.
 * All native BT operations are invoked via {@link WearableBridge}.
 */
public class SyncService {

    /** BLE service UUID for the custom health-data sync profile. */
    public static final String HEALTH_SYNC_SERVICE_UUID =
            "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";

    /** MAC address regex for pre-transmission validation. */
    private static final String MAC_REGEX =
            "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$";

    private final WearableBridge bridge;

    /** true once the native BT stack has been successfully initialised. */
    private boolean stackReady = false;

    /**
     * @param bridge  Shared {@link WearableBridge} instance.
     */
    public SyncService(WearableBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("WearableBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Initialises the native Bluetooth stack with the given device name
     * and power profile.
     *
     * @param deviceName  Advertised BLE name (≤ 20 UTF-8 characters).
     * @param powerMode   One of {@link WearableBridge.BluetoothPowerMode}.
     * @return            true if the stack started successfully.
     */
    public boolean initializeBluetoothStack(String deviceName, int powerMode) {
        if (deviceName == null || deviceName.isBlank()) {
            throw new IllegalArgumentException(
                "Device name must not be null or blank.");
        }
        if (deviceName.length() > 20) {
            throw new IllegalArgumentException(
                "Device name must be ≤ 20 characters for BLE advertising, "
                + "got length " + deviceName.length());
        }

        System.out.printf("[Sync] Initialising BT stack: name='%s', "
                + "powerMode=%s%n", deviceName, powerModeLabel(powerMode));

        // ── Native call ──────────────────────────────────────────────
        boolean ok = bridge.initializeBluetoothStack(deviceName, powerMode);
        // ────────────────────────────────────────────────────────────

        stackReady = ok;
        if (ok) {
            System.out.println("[Sync] ✓ Bluetooth stack initialised.");
        } else {
            System.err.println("[Sync] ✗ ERROR: Bluetooth stack failed to start.");
        }
        return ok;
    }

    /**
     * Performs a BLE scan and returns discovered peripherals, optionally
     * filtered to the custom health-sync service UUID.
     *
     * @param scanDurationMs   Scan window in milliseconds (≥ 100 recommended).
     * @param filterByService  BLE service UUID filter, or {@code null} for all.
     * @return                 Array of {@link WearableBridge.BleDevice} records,
     *                         or an empty array if none found.
     */
    public WearableBridge.BleDevice[] scanForDevices(int scanDurationMs,
                                                     String filterByService) {
        requireStack("scanForDevices");
        if (scanDurationMs < 100) {
            System.err.printf("[Sync] ⚠  Scan duration %d ms is very short "
                    + "— some devices may be missed.%n", scanDurationMs);
        }

        System.out.printf("[Sync] Scanning for BLE devices (%d ms)%s...%n",
                scanDurationMs,
                filterByService != null ? " [filter: " + filterByService + "]" : "");

        // ── Native call ──────────────────────────────────────────────
        WearableBridge.BleDevice[] devices =
                bridge.scanForDevices(scanDurationMs, filterByService);
        // ────────────────────────────────────────────────────────────

        if (devices == null) {
            System.err.println("[Sync] ERROR: scanForDevices() returned null "
                    + "— BT stack fault.");
            return new WearableBridge.BleDevice[0];
        }
        if (devices.length == 0) {
            System.out.println("[Sync] No BLE devices found.");
        } else {
            System.out.println("[Sync] ✓ Found " + devices.length + " device(s):");
            for (int i = 0; i < devices.length; i++) {
                System.out.printf("[Sync]   [%d] %s%n", i + 1, devices[i]);
            }
        }
        return devices;
    }

    /**
     * Transmits a data payload to a connected BLE peripheral.
     *
     * @param deviceAddress  Target MAC address ("AA:BB:CC:DD:EE:FF").
     * @param payload        Raw bytes to send.
     * @param requireAck     Block until GATT write-response received.
     * @return               {@link WearableBridge.PacketSendResult},
     *                       or a synthetic failure result on validation error.
     */
    public WearableBridge.PacketSendResult sendDataPacket(String deviceAddress,
                                                         byte[] payload,
                                                         boolean requireAck) {
        requireStack("sendDataPacket");

        if (deviceAddress == null || !deviceAddress.matches(MAC_REGEX)) {
            return failPacket("Invalid MAC address: '" + deviceAddress + "'.");
        }
        if (payload == null || payload.length == 0) {
            return failPacket("Payload must not be null or empty.");
        }

        System.out.printf("[Sync] Sending %d-byte packet to %s (ack=%b)...%n",
                payload.length, deviceAddress, requireAck);

        // ── Native call ──────────────────────────────────────────────
        WearableBridge.PacketSendResult result =
                bridge.sendDataPacket(deviceAddress, payload, requireAck);
        // ────────────────────────────────────────────────────────────

        if (result == null) {
            return failPacket("sendDataPacket() returned null.");
        }
        if (result.success) {
            System.out.printf("[Sync] ✓ Packet delivered. %s%n", result);
        } else {
            System.err.printf("[Sync] ✗ Delivery failed. %s%n", result);
        }
        return result;
    }

    /**
     * Convenience: scan for devices advertising the health-sync service,
     * then transmit a payload to the first connectable device found.
     *
     * @param payload        Data to transmit.
     * @param scanDurationMs BLE scan window in milliseconds.
     * @return               Send result, or a failure result if no target found.
     */
    public WearableBridge.PacketSendResult discoverAndSync(byte[] payload,
                                                          int scanDurationMs) {
        System.out.println("[Sync] === Discover & Sync Pipeline ===");
        WearableBridge.BleDevice[] devices =
                scanForDevices(scanDurationMs, HEALTH_SYNC_SERVICE_UUID);

        for (WearableBridge.BleDevice device : devices) {
            if (device.connectable) {
                System.out.println("[Sync] Targeting: " + device);
                WearableBridge.PacketSendResult result =
                        sendDataPacket(device.macAddress, payload, true);
                System.out.println("[Sync] === Sync Complete ===");
                return result;
            }
        }
        System.err.println("[Sync] No connectable health-sync device found.");
        System.out.println("[Sync] === Sync Aborted ===");
        return failPacket("No connectable device found after scan.");
    }

    /** @return true if the native Bluetooth stack is currently active. */
    public boolean isStackReady() { return stackReady; }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private void requireStack(String callerName) {
        if (!stackReady) {
            throw new IllegalStateException(
                "[Sync] " + callerName + "() called before Bluetooth stack "
                + "is ready. Call initializeBluetoothStack() first.");
        }
    }

    private static WearableBridge.PacketSendResult failPacket(String msg) {
        System.err.println("[Sync] ✗ " + msg);
        return new WearableBridge.PacketSendResult(false, 0, 0, 0, msg);
    }

    private static String powerModeLabel(int mode) {
        switch (mode) {
            case WearableBridge.BluetoothPowerMode.LOW_POWER:        return "LOW_POWER";
            case WearableBridge.BluetoothPowerMode.BALANCED:         return "BALANCED";
            case WearableBridge.BluetoothPowerMode.HIGH_PERFORMANCE: return "HIGH_PERFORMANCE";
            default: return "UNKNOWN(" + mode + ")";
        }
    }
}