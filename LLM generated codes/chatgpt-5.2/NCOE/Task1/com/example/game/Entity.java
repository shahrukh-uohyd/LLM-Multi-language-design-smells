package com.example.game;

public class Entity {
    // Must NOT be private unless you use GetFieldID with access checks disabled (not recommended)
    public float velocity;

    public Entity(float initialVelocity) {
        this.velocity = initialVelocity;
    }
}
