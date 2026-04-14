public enum DeviceState {
    IDLE(0),
    RUNNING(1),
    ERROR(2),
    MAINTENANCE(3);

    private final int value;

    DeviceState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}