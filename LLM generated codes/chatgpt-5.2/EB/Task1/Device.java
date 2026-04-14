public class Device {

    // Internal device state
    private String deviceId;
    private int powerLevel;      // 0–100
    private double temperature;  // degrees Celsius
    private boolean active;

    static {
        System.loadLibrary("device_native");
    }

    public Device(String deviceId, int powerLevel, double temperature, boolean active) {
        this.deviceId = deviceId;
        this.powerLevel = powerLevel;
        this.temperature = temperature;
        this.active = active;
    }

    // Native operation that uses internal state
    public native double computeHealthScore();

    public static void main(String[] args) {
        Device device = new Device("DEV-001", 85, 42.5, true);

        double score = device.computeHealthScore();
        System.out.println("Device health score (native): " + score);
    }
}
