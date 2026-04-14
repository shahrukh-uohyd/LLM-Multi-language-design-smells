/**
 * WearableMain
 *
 * End-to-end demonstration of all three wearable subsystems sharing
 * a single {@link WearableBridge} instance through a complete
 * health-monitoring and sync session.
 *
 * Session sequence
 * ────────────────
 *  1. HeartRateMonitor — single reading  →  averaged reading
 *  2. DataPrivacyModule — key generation  →  health-data encryption
 *  3. SyncService — BT stack init  →  device scan  →  data transmission
 */
public class WearableMain {

    public static void main(String[] args) {

        // One bridge — one native library load for the entire session.
        WearableBridge bridge = new WearableBridge();

        // Instantiate all three subsystems with the shared bridge.
        HeartRateMonitor  hrm     = new HeartRateMonitor(bridge);
        DataPrivacyModule privacy = new DataPrivacyModule(bridge);
        SyncService       sync    = new SyncService(bridge);

        banner("Health Wearable — Session Start");

        // ── 1. Heart Rate Monitoring ─────────────────────────────────
        banner("Phase 1 · Heart Rate Monitoring");

        // Quick single-shot measurement using defaults
        float instantBpm = hrm.measureHeartRate();
        System.out.printf("[Main] Instant BPM reading: %s%n",
                instantBpm > 0 ? String.format("%.1f", instantBpm) : "unavailable");

        // Averaged reading over 3 bursts for higher confidence
        float avgBpm = hrm.measureAverageBpm(3);
        System.out.printf("[Main] Averaged BPM (3 bursts): %s%n",
                avgBpm > 0 ? String.format("%.1f", avgBpm) : "unavailable");

        // Manual two-step call (raw access pattern)
        int[] rawSamples = hrm.readPpgSensor(
                HeartRateMonitor.DEFAULT_SENSOR_ID,
                HeartRateMonitor.DEFAULT_SAMPLE_COUNT);
        if (rawSamples != null) {
            float manualBpm = hrm.calculateBpm(
                    rawSamples, HeartRateMonitor.DEFAULT_SAMPLING_HZ);
            System.out.printf("[Main] Manual BPM calculation: %.1f%n", manualBpm);
        }

        // ── 2. Data Privacy ──────────────────────────────────────────
        banner("Phase 2 · Data Privacy — Key Generation & Encryption");

        // Build a synthetic health record payload
        String healthRecord = String.format(
                "{\"bpm\":%.1f,\"timestamp\":%d,\"deviceId\":\"WB-001\"}",
                avgBpm > 0 ? avgBpm : 72.0f,
                System.currentTimeMillis());
        byte[] plaintext = healthRecord.getBytes();

        // AAD binds the ciphertext to a specific user session ID
        byte[] aad = "session:user-42:2026-02-21".getBytes();

        // Pipeline: generate key then encrypt in one call
        DataPrivacyModule.EncryptionBundle bundle =
                privacy.generateKeyAndEncrypt(
                        DataPrivacyModule.ALGO_AES_256_GCM,
                        "hrm-session-key",
                        plaintext,
                        aad);

        if (bundle == null) {
            System.err.println("[Main] ✗ Encryption failed — aborting sync.");
            return;
        }

        // Standalone key-gen + encrypt for a second record (sleep data)
        String keyHandle = privacy.generateSecureKey(
                DataPrivacyModule.ALGO_CHACHA20_POLY1305,
                "sleep-session-key");
        if (keyHandle != null) {
            byte[] sleepRecord = "{\"deepSleepMin\":92,\"remCycles\":4}"
                    .getBytes();
            byte[] sleepCipher = privacy.encryptHealthData(
                    sleepRecord, keyHandle, new byte[0] /* no AAD */);
            System.out.printf("[Main] Sleep record ciphertext: %s%n",
                    sleepCipher != null
                        ? sleepCipher.length + " bytes" : "null");
        }

        // ── 3. Sync Service ──────────────────────────────────────────
        banner("Phase 3 · Sync Service — BLE Init, Scan & Transmit");

        // Initialise the BT stack with a balanced power profile
        boolean btReady = sync.initializeBluetoothStack(
                "WearableDev-01",
                WearableBridge.BluetoothPowerMode.BALANCED);

        if (!btReady) {
            System.err.println("[Main] ✗ Bluetooth unavailable — cannot sync.");
            return;
        }

        // Discover all devices then send heart-rate ciphertext to the first
        // connectable one found advertising the health-sync service UUID
        WearableBridge.PacketSendResult hrSendResult =
                sync.discoverAndSync(bundle.ciphertext, 2000 /* ms scan */);
        System.out.printf("[Main] HR sync result: %s%n", hrSendResult);

        // Manual pattern: targeted scan then explicit send
        WearableBridge.BleDevice[] allDevices =
                sync.scanForDevices(1500, null /* all services */);
        if (allDevices.length > 0) {
            WearableBridge.BleDevice target = allDevices[0];
            System.out.println("[Main] Sending directly to: " + target);
            WearableBridge.PacketSendResult directResult =
                    sync.sendDataPacket(
                            target.macAddress,
                            bundle.ciphertext,
                            true /* require ACK */);
            System.out.printf("[Main] Direct send result: %s%n", directResult);
        }

        banner("Session Complete");
    }

    // ----------------------------------------------------------------
    private static void banner(String title) {
        String bar = "═".repeat(title.length() + 4);
        System.out.println("\n╔" + bar + "╗");
        System.out.println("║  " + title + "  ║");
        System.out.println("╚" + bar + "╝");
    }
}