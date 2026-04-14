public class DeviceState {
    private int    batteryLevel;   // 0–100
    private boolean isOnline;
    private String  firmwareVersion;

    public DeviceState(int batteryLevel, boolean isOnline, String firmwareVersion) {
        this.batteryLevel    = batteryLevel;
        this.isOnline        = isOnline;
        this.firmwareVersion = firmwareVersion;
    }

    public int     getBatteryLevel()    { return batteryLevel;    }
    public boolean isOnline()           { return isOnline;        }
    public String  getFirmwareVersion() { return firmwareVersion; }
}