package com.example.game;

public final class PhysicsNative {

    static {
        // Name must match your compiled native library
        System.loadLibrary("physics_native");
    }

    // Native method declaration
    public static native void updateVelocityNative(Entity entity, float newVelocity);
}
