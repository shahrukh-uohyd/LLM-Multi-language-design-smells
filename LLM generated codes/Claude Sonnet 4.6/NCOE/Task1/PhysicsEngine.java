public class PhysicsEngine {

    static {
        // Load the shared native library (libphysicsengine.so / physicsengine.dll)
        System.loadLibrary("physicsengine");
    }

    /**
     * One-time initialization: caches the jfieldID for Entity.velocity on
     * the C++ side so the game loop never pays the lookup cost.
     * Must be called ONCE before the game loop starts.
     */
    public static native void initNativeCache(Class<?> entityClass);

    /**
     * Updates the velocity field on the given Entity object.
     * Called 60 times per second from the game loop — uses the cached
     * field ID internally, so there is no per-frame lookup overhead.
     *
     * @param entityObj   the Entity instance to modify
     * @param newVelocity the new velocity value to set
     */
    public static native void updateVelocityNative(Object entityObj, float newVelocity);


    // -------------------------------------------------------------------------
    // Example game loop call site
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws InterruptedException {
        Entity player = new Entity(0.0f);

        // ✅ Cache the field ID once before the loop starts
        initNativeCache(Entity.class);

        System.out.println("Starting game loop...");

        // Simulate 60 FPS for 3 seconds (180 frames)
        for (int frame = 0; frame < 180; frame++) {
            float newVelocity = frame * 0.1f; // some computed value

            // ✅ Safe to call 60x/sec — no field lookup happening inside
            updateVelocityNative(player, newVelocity);

            if (frame % 20 == 0) {
                // Read back from Java side to verify
                System.out.printf("Frame %3d | player.velocity = %.2f%n", frame, player.velocity);
            }

            Thread.sleep(16); // ~60 FPS
        }
    }
}