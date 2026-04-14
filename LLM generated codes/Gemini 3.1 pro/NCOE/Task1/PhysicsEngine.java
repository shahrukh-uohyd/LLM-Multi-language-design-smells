package com.example.game;

public class PhysicsEngine {
    
    static {
        // Load the compiled C++ library (e.g., libphysics.so, physics.dll, or libphysics.dylib)
        System.loadLibrary("physics");
        
        // Cache the JNI Field IDs immediately upon loading the library
        initNativeIDs();
    }

    // Native method to initialize and cache the field ID
    private static native void initNativeIDs();

    // Your requested native method
    public native void updateVelocityNative(Entity entityObj, float newVelocity);

    // Call site: Simulated 60 FPS Game Loop
    public void gameLoop(Entity myEntity) {
        boolean isRunning = true;
        
        while (isRunning) {
            // Calculate physics step...
            float calculatedNewVelocity = 9.8f; // Example calculated value
            
            // Call to C++ JNI - High frequency call
            updateVelocityNative(myEntity, calculatedNewVelocity);
            
            // Sleep/Sync to maintain 60 FPS limit (approx. 16.6ms per frame)
            try { Thread.sleep(16); } catch (InterruptedException e) {}
        }
    }
}