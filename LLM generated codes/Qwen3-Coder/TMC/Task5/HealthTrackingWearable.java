// HealthTrackingWearable.java
public class HealthTrackingWearable {
    static {
        System.loadLibrary("health_wearable_native");
    }

    // Heart rate monitor methods
    private native int readPpgSensor();
    private native int calculateBpm(int[] ppgData);

    // Sync service methods
    private native void initializeBluetoothStack();
    private native String[] scanForDevices();
    private native boolean sendDataPacket(byte[] data, String deviceId);

    // Data privacy module methods
    private native String generateSecureKey();
    private native byte[] encryptHealthData(byte[] data, String key);

    private static final HealthTrackingWearable INSTANCE = new HealthTrackingWearable();
    
    private HealthTrackingWearable() {}
    
    public static HealthTrackingWearable getInstance() {
        return INSTANCE;
    }
}

class HeartRateMonitor {
    private HealthTrackingWearable wearable = HealthTrackingWearable.getInstance();

    public int getCurrentHeartRate() {
        int ppgValue = wearable.readPpgSensor();
        int[] ppgData = {ppgValue}; // Simplified for example
        return wearable.calculateBpm(ppgData);
    }
}

class SyncService {
    private HealthTrackingWearable wearable = HealthTrackingWearable.getInstance();

    public void setupConnection() {
        wearable.initializeBluetoothStack();
    }

    public String[] findPairedDevices() {
        return wearable.scanForDevices();
    }

    public boolean transmitData(byte[] healthData, String device) {
        return wearable.sendDataPacket(healthData, device);
    }
}

class DataPrivacyModule {
    private HealthTrackingWearable wearable = HealthTrackingWearable.getInstance();

    public String createEncryptionKey() {
        return wearable.generateSecureKey();
    }

    public byte[] protectHealthInfo(byte[] data, String key) {
        return wearable.encryptHealthData(data, key);
    }
}