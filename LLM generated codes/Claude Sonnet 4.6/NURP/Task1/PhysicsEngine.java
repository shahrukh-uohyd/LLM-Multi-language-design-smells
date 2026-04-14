package com.engine.physics;

import com.engine.native_support.NativeLibraryLoader;

public class PhysicsEngine {

    static {
        // Load the native physics_core library before any native methods are called.
        NativeLibraryLoader.load();
    }

    // Declare your native methods here after the library is loaded.
    public native void initializeWorld(float gravity);
    public native void stepSimulation(float deltaTime);
    public native void destroyWorld();
}