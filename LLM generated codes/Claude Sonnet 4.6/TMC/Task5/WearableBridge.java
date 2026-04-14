/**
 * WearableBridge
 *
 * Central JNI bridge for the Health-Tracking Wearable application.
 * Declares all 7 native methods across three functional groups and
 * loads the native shared library exactly once.
 *
 * Native library naming conventions:
 *   Linux   → libWearableBridge.so
 *   Windows → WearableBridge.dll
 *   macOS   → libWearableBridge.dylib
 *
 * To regenerate the C++ header after any signature change:
 *   javac -h . WearableBridge.java
 */
public class WearableBridge {

    // ----------------------------------------------------------------
    // Static initializer — library is loaded once for the entire JVM.
    // All three modules share this single load via constructor injection.
    // ----------------------------------------------------------------
    static {
        System.loadLibrary("WearableBridge");
    }

    // ================================================================
    // GROUP 1 — HeartRateMonitor  (public — safety-critical sensor I/O)
    // Direct PPG hardware access and BPM computation via native DSP.
    // ================================================================

    /**
     * Captures a raw PPG (photoplethysmography) sample burst from the
     * optical heart-rate sensor.
     *
     * <p>Each sample is a 16-bit ADC reading; the array length equals
     * {@code sampleCount}. The native layer handles sensor warm-up and
     * LED drive-current calibration automatically.
     *
     * @param sensorId    Index of the optical sensor to read (0-based).
     *                    Most wearables expose a single sensor at index 0.
     * @param sampleCount Number of ADC samples to capture per call.
     *                    Typical values: 25 (1-second burst at 25 Hz),
     *                    or 128 (5-second burst at 25.6 Hz).
     * @return            Array of raw 16-bit PPG samples
     *                    (length == {@code sampleCount}), or {@code null}
     *                    if the sensor is unavailable or powered off.
     */
    public native int[] readPpgSensor(int sensorId, int sampleCount);

    /**
     * Runs the native BPM estimation algorithm on a pre-captured PPG
     * sample buffer.
     *
     * <p>The algorithm applies a band-pass filter (0.5 – 4.0 Hz),
     * peak detection, and inter-beat interval averaging internally.
     *
     * @param ppgSamples    Raw PPG samples returned by
     *                      {@link #readPpgSensor(int, int)}.
     * @param samplingRateHz Sampling frequency used to capture the buffer
     *                      (Hz). Must match the rate configured in the
     *                      sensor driver, typically 25 or 100 Hz.
     * @return              Estimated heart rate in beats-per-minute
     *                      (floating-point for trend smoothing), or
     *                      {@code -1.0f} if the signal quality is too
     *                      poor for a reliable reading.
     */
    public native float calculateBpm(int[] ppgSamples, float samplingRateHz);

    // ================================================================
    // GROUP 2 — SyncService  (package-private — BT stack access only)
    // Bluetooth stack lifecycle, device discovery, and data transfer.
    // ================================================================

    /**
     * Initialises the native Bluetooth stack and allocates internal
     * HCI/GATT resources.
     *
     * <p>Must be called once before any other Bluetooth operation.
     * Calling it a second time without an intervening shutdown is a
     * no-op that returns {@code true}.
     *
     * @param deviceName  Advertised BLE device name (≤ 20 UTF-8 characters
     *                    to fit in a standard advertising payload).
     * @param powerMode   RF transmit-power profile:
     *                      {@link BluetoothPowerMode#LOW_POWER},
     *                      {@link BluetoothPowerMode#BALANCED}, or
     *                      {@link BluetoothPowerMode#HIGH_PERFORMANCE}.
     * @return            true if the stack was initialised (or was already
     *                    running); false if a hardware fault prevents BT
     *                    from starting.
     */
    native boolean initializeBluetoothStack(String deviceName, int powerMode);

    /**
     * Performs a BLE passive scan and returns discovered peripheral devices.
     *
     * <p>The scan runs for {@code scanDurationMs} milliseconds, then
     * returns. Duplicate advertisements from the same MAC address are
     * de-duplicated by the native layer; only the highest-RSSI entry
     * for each device is kept.
     *
     * @param scanDurationMs  Duration of the scan window in milliseconds.
     *                        Values below 100 ms may miss slow advertisers.
     * @param filterByService Optional 128-bit BLE service UUID string to
     *                        filter results (e.g. the Health Thermometer
     *                        Service UUID). Pass {@code null} to return
     *                        all devices.
     * @return                Array of {@link BleDevice} descriptors, sorted
     *                        by descending RSSI; empty array if no devices
     *                        are found; {@code null} on a stack fault.
     */
    native BleDevice[] scanForDevices(int scanDurationMs, String filterByService);

    /**
     * Sends a framed data packet to a connected BLE peripheral over the
     * custom GATT characteristic.
     *
     * <p>The native layer handles MTU negotiation, fragmentation of
     * payloads larger than the negotiated MTU, and acknowledgement
     * retries up to a configurable maximum.
     *
     * @param deviceAddress  BLE MAC address of the target peripheral,
     *                       e.g. {@code "AA:BB:CC:DD:EE:FF"}.
     * @param payload        Raw bytes to transmit. The native layer
     *                       prepends a 4-byte sequence header automatically.
     * @param requireAck     If {@code true}, the method blocks until a
     *                       GATT write-with-response acknowledgement is
     *                       received or the timeout elapses.
     * @return               {@link PacketSendResult} describing the
     *                       transmission outcome (bytes sent, retry count,
     *                       latency).
     */
    native PacketSendResult sendDataPacket(String deviceAddress,
                                           byte[] payload,
                                           boolean requireAck);

    // ================================================================
    // GROUP 3 — DataPrivacyModule  (package-private — crypto only)
    // Key generation and authenticated health-data encryption.
    // ================================================================

    /**
     * Generates a cryptographically secure symmetric key and stores it
     * in the native hardware-backed key store (TEE or SE where available).
     *
     * <p>The returned key handle is an opaque reference — the raw key
     * material never crosses the JNI boundary. Pass the handle to
     * {@link #encryptHealthData(byte[], String, byte[])} to use it.
     *
     * @param keyAlgorithm  Algorithm identifier: {@code "AES-256-GCM"},
     *                      {@code "CHACHA20-POLY1305"}, etc.
     * @param keyAlias      Unique alias for the key in the native store.
     *                      Re-using an existing alias rotates the key.
     * @return              Opaque key-handle string for use with encryption
     *                      calls, or {@code null} if key generation failed
     *                      (e.g. no TEE available and secure generation
     *                      is required).
     */
    native String generateSecureKey(String keyAlgorithm, String keyAlias);

    /**
     * Encrypts health data using the key identified by {@code keyHandle}
     * and returns authenticated ciphertext.
     *
     * <p>The native layer prepends a randomly generated IV/nonce to the
     * returned byte array so that the caller does not need to manage it
     * separately. The format is:
     * <pre>
     *   [ IV/nonce (12 bytes) | ciphertext | auth tag (16 bytes) ]
     * </pre>
     *
     * @param plaintext    Raw health-record bytes to encrypt.
     * @param keyHandle    Opaque key handle returned by
     *                     {@link #generateSecureKey(String, String)}.
     * @param aadBytes     Additional Authenticated Data (AAD) bound to
     *                     the ciphertext (e.g. patient ID, timestamp).
     *                     Pass an empty array to skip AAD.
     * @return             Authenticated ciphertext (IV + ciphertext + tag),
     *                     or {@code null} if encryption failed.
     */
    native byte[] encryptHealthData(byte[] plaintext,
                                    String keyHandle,
                                    byte[] aadBytes);

    // ================================================================
    // Shared data types — populated by the native layer.
    // ================================================================

    /**
     * Bluetooth RF transmit-power profiles for
     * {@link #initializeBluetoothStack(String, int)}.
     * Integer values must match the native {@code BtPowerMode} enum.
     */
    public static final class BluetoothPowerMode {
        private BluetoothPowerMode() { /* constants only */ }

        /** Minimise radio duty cycle — longest battery life. */
        public static final int LOW_POWER       = 0;
        /** Trade-off between range and power consumption. */
        public static final int BALANCED        = 1;
        /** Maximum TX power — best range, highest battery drain. */
        public static final int HIGH_PERFORMANCE = 2;
    }

    /**
     * Describes a BLE peripheral discovered by
     * {@link #scanForDevices(int, String)}.
     */
    public static class BleDevice {
        /** BLE MAC address, e.g. {@code "AA:BB:CC:DD:EE:FF"}. */
        public final String macAddress;
        /** Advertised device name, or an empty string if not advertised. */
        public final String name;
        /** Received signal strength in dBm (negative; closer to 0 = stronger). */
        public final int    rssiDbm;
        /** true if the device advertises connectable undirected events. */
        public final boolean connectable;

        public BleDevice(String macAddress, String name,
                         int rssiDbm, boolean connectable) {
            this.macAddress  = macAddress;
            this.name        = name;
            this.rssiDbm     = rssiDbm;
            this.connectable = connectable;
        }

        @Override
        public String toString() {
            return String.format(
                "BleDevice{mac='%s', name='%s', rssi=%d dBm, connectable=%b}",
                macAddress, name, rssiDbm, connectable);
        }
    }

    /**
     * Outcome descriptor returned by
     * {@link #sendDataPacket(String, byte[], boolean)}.
     */
    public static class PacketSendResult {
        /** true if the packet was transmitted (and acknowledged, if requested). */
        public final boolean success;
        /** Total bytes accepted by the GATT layer (after fragmentation). */
        public final int     bytesSent;
        /** Number of automatic retries performed (0 on first-attempt success). */
        public final int     retryCount;
        /** Round-trip latency in milliseconds (meaningful only when ack requested). */
        public final int     latencyMs;
        /** Human-readable status from the native BT stack. */
        public final String  statusMessage;

        public PacketSendResult(boolean success,  int bytesSent,
                                int retryCount,   int latencyMs,
                                String statusMessage) {
            this.success       = success;
            this.bytesSent     = bytesSent;
            this.retryCount    = retryCount;
            this.latencyMs     = latencyMs;
            this.statusMessage = statusMessage;
        }

        @Override
        public String toString() {
            return String.format(
                "PacketSendResult{ok=%b, bytes=%d, retries=%d, "
                + "latency=%d ms, msg='%s'}",
                success, bytesSent, retryCount, latencyMs, statusMessage);
        }
    }
}