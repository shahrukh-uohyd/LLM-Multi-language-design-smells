package com.example;

public class PhysicsEngine {
    static {
        System.loadLibrary("physicsengine");
    }

    // Passes the entire Entity object across the JNI boundary
    public native boolean isEntityInSafetyZone(Entity entity, double centerX, double centerY, double radius);
}