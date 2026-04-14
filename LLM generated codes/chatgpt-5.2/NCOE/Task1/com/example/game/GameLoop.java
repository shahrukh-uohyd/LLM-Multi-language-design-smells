package com.example.game;

public class GameLoop {

    public static void main(String[] args) {
        Entity player = new Entity(0.0f);

        // Simulated game loop
        while (true) {
            float newVelocity = computeVelocity();
            PhysicsNative.updateVelocityNative(player, newVelocity);

            // 60 FPS simulation
            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {}
        }
    }

    private static float computeVelocity() {
        return (float) Math.random() * 10.0f;
    }
}
