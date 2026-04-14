class HumiditySensor extends BaseSensor {
    static {
        System.loadLibrary("humidity_sensor_native");
    }
    
    private native boolean nativeInitialize();
    private native double nativeReadValue();
    private native void nativeCleanup();
    
    @Override
    public boolean initialize() {
        isInitialized = nativeInitialize();
        return isInitialized;
    }
    
    @Override
    public double readValue() {
        if (!isInitialized) {
            throw new IllegalStateException("Humidity sensor not initialized");
        }
        return nativeReadValue();
    }
    
    @Override
    public void cleanup() {
        if (isInitialized) {
            nativeCleanup();
            isInitialized = false;
        }
    }
}