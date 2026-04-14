// Base abstract class for sensors
abstract class BaseSensor {
    protected boolean isInitialized = false;
    
    protected BaseSensor() {}
    
    public abstract boolean initialize();
    public abstract double readValue();
    public abstract void cleanup();
    
    public boolean isInitialized() {
        return isInitialized;
    }
}