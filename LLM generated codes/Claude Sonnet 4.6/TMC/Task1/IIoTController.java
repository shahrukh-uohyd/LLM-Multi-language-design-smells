/**
 * IIoTController
 *
 * Core JNI bridge for the Industrial IoT Controller.
 * Declares all 8 native methods and directly invokes the two
 * hardware-unit methods (startMotor, performSelfTest).
 */
public class IIoTController {

    // ----------------------------------------------------------------
    // Static initializer — loads the native shared library once.
    // The library file must be named:
    //   libIIoTController.so  (Linux)
    //   IIoTController.dll    (Windows)
    //   libIIoTController.dylib (macOS)
    // ----------------------------------------------------------------
    static {
        System.loadLibrary("IIoTController");
    }

    // ================================================================
    // GROUP 1 — Hardware Unit (called directly by this class)
    // ================================================================

    /**
     * Starts the physical motor attached to this hardware unit.
     *
     * @param motorId    Unique identifier of the motor (0-based index).
     * @param speedRpm   Target speed in revolutions per minute.
     * @return           true if the motor started successfully.
     */
    public native boolean startMotor(int motorId, int speedRpm);

    /**
     * Runs a full self-diagnostic on the hardware unit.
     *
     * @return  A diagnostic status code:
     *          0 = all tests passed,
     *          positive integer = bitmask of failed subsystems.
     */
    public native int performSelfTest();

    // ================================================================
    // GROUP 2 — SecurityModule natives (package-private visibility)
    // ================================================================

    /**
     * Encrypts an outgoing transmission payload using AES-256-GCM
     * (or another algorithm configured in the native layer).
     *
     * @param plaintext  Raw bytes to be encrypted.
     * @param keyAlias   Alias of the key stored in the native key-store.
     * @return           Encrypted ciphertext bytes, or null on failure.
     */
    native byte[] encryptTransmission(byte[] plaintext, String keyAlias);

    /**
     * Verifies an X.509 certificate against the device trust-store.
     *
     * @param certDerBytes  DER-encoded certificate bytes.
     * @return              true if the certificate is valid and trusted.
     */
    native boolean verifyCertificate(byte[] certDerBytes);

    /**
     * Computes a CRC-32 / SHA-based checksum of a payload.
     *
     * @param payload  The raw payload bytes.
     * @return         Hex-encoded checksum string.
     */
    native String calculatePayloadChecksum(byte[] payload);

    // ================================================================
    // GROUP 3 — CloudService natives (package-private visibility)
    // ================================================================

    /**
     * Establishes a connection to a remote relay server.
     *
     * @param relayHost  Fully-qualified hostname or IP of the relay.
     * @param port       TCP port number.
     * @param timeoutMs  Connection timeout in milliseconds.
     * @return           A non-negative connection handle, or -1 on error.
     */
    native int connectToRemoteRelay(String relayHost, int port, int timeoutMs);

    /**
     * Fetches the latest configuration update from the cloud endpoint.
     *
     * @param connectionHandle  Handle returned by {@link #connectToRemoteRelay}.
     * @return                  JSON string containing the configuration,
     *                          or null if no update is available.
     */
    native String fetchConfigUpdate(int connectionHandle);

    /**
     * Appends a structured event record to the cloud log.
     *
     * @param connectionHandle  Handle returned by {@link #connectToRemoteRelay}.
     * @param eventTag          Short category tag, e.g. "FAULT", "INFO".
     * @param eventPayload      UTF-8 JSON payload describing the event.
     * @return                  Server-assigned event ID, or -1 on failure.
     */
    native long logEventToCloud(int connectionHandle, String eventTag, String eventPayload);

    // ================================================================
    // Hardware Unit — direct invocation examples
    // ================================================================

    /**
     * Runs the hardware-unit startup sequence:
     *   1. Self-test
     *   2. Motor start (if self-test passes)
     */
    public void runHardwareStartupSequence(int motorId, int speedRpm) {
        System.out.println("[HW] Running self-test...");
        int diagCode = performSelfTest();

        if (diagCode == 0) {
            System.out.println("[HW] Self-test PASSED. Starting motor " + motorId
                    + " at " + speedRpm + " RPM...");
            boolean started = startMotor(motorId, speedRpm);
            if (started) {
                System.out.println("[HW] Motor " + motorId + " started successfully.");
            } else {
                System.err.println("[HW] ERROR: Motor " + motorId + " failed to start.");
            }
        } else {
            System.err.printf("[HW] Self-test FAILED. Diagnostic code: 0x%X%n", diagCode);
        }
    }

    // ----------------------------------------------------------------
    // Entry point for quick local testing
    // ----------------------------------------------------------------
    public static void main(String[] args) {
        IIoTController controller = new IIoTController();

        // --- Hardware unit ---
        controller.runHardwareStartupSequence(0, 1500);

        // --- Security module ---
        SecurityModule security = new SecurityModule(controller);
        byte[] samplePayload = "SENSOR_DATA:42.7°C".getBytes();
        byte[] encrypted = security.securePayload(samplePayload, "device-key-01");
        System.out.println("[Main] Encrypted payload length: "
                + (encrypted != null ? encrypted.length : "null"));

        // --- Cloud service ---
        CloudService cloud = new CloudService(controller);
        cloud.connect("relay.iiot-cloud.example.com", 8883, 5000);
        cloud.syncAndLog("STARTUP",
                "{\"device\":\"unit-7\",\"firmware\":\"2.4.1\"}");
        cloud.disconnect();
    }
}