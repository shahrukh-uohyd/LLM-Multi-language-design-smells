// Entity.java
public class Entity {
    private float velocity;
    
    public Entity(float initialVelocity) {
        this.velocity = initialVelocity;
    }
    
    public float getVelocity() {
        return velocity;
    }
    
    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }
    
    // Native method declaration
    public native void updateVelocityNative(float newVelocity);
    
    static {
        System.loadLibrary("physics_engine"); // Load native library
    }
}