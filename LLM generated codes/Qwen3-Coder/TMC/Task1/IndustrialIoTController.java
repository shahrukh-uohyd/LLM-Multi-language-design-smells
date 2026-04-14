// IndustrialIoTController.java
public class IndustrialIoTController {
    static {
        System.loadLibrary("iot_controller_native");
    }

    // Hardware unit methods
    private native void startMotor();
    private native boolean performSelfTest();

    // Security module methods
    private native byte[] encryptTransmission(byte[] data);
    private native boolean verifyCertificate(String certificatePath);
    private native int calculatePayloadChecksum(byte[] payload);

    // Cloud service methods
    private native boolean connectToRemoteRelay(String relayAddress, int port);
    private native String fetchConfigUpdate(String deviceId);
    private native boolean logEventToCloud(String eventType, String message, long timestamp);

    private static final IndustrialIoTController INSTANCE = new IndustrialIoTController();
    
    private IndustrialIoTController() {}
    
    public static IndustrialIoTController getInstance() {
        return INSTANCE;
    }
}

class HardwareUnit {
    private IndustrialIoTController controller = IndustrialIoTController.getInstance();

    public void initializeMotor() {
        controller.startMotor();
    }

    public boolean runSelfTest() {
        return controller.performSelfTest();
    }
}

class SecurityModule {
    private IndustrialIoTController controller = IndustrialIoTController.getInstance();

    public byte[] secureData(byte[] rawData) {
        return controller.encryptTransmission(rawData);
    }

    public boolean validateCertificate(String certPath) {
        return controller.verifyCertificate(certPath);
    }

    public int generateChecksum(byte[] payload) {
        return controller.calculatePayloadChecksum(payload);
    }
}

class CloudService {
    private IndustrialIoTController controller = IndustrialIoTController.getInstance();

    public boolean establishConnection(String relayAddr, int relayPort) {
        return controller.connectToRemoteRelay(relayAddr, relayPort);
    }

    public String retrieveConfiguration(String deviceId) {
        return controller.fetchConfigUpdate(deviceId);
    }

    public boolean sendLogToCloud(String event, String msg) {
        return controller.logEventToCloud(event, msg, System.currentTimeMillis());
    }
}