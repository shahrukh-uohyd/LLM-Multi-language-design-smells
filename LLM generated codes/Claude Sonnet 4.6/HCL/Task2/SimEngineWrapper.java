/**
 * Public API for the JNI-based simulation engine.
 *
 * <p>Referencing this class for the first time triggers {@link SimEngineLoader}
 * (via its static initialiser), which loads the correct native library
 * automatically before any native method can be called.</p>
 */
public final class SimEngineWrapper {

    // Force the loader class to initialise (and load the native library)
    // before any native method on this class is invoked.
    static {
        SimEngineLoader.loadNativeLibrary();
    }

    // -----------------------------------------------------------------------
    // Native method declarations – implemented in SimEngine64 / SimEngine32
    // -----------------------------------------------------------------------

    /** Initialises the simulation engine with the given configuration. */
    public native void initSimulation(int threadCount, double timeStep);

    /** Advances the simulation by one tick. */
    public native void tick();

    /** Returns the current simulation state as a serialised byte array. */
    public native byte[] getSimulationState();

    /** Releases all native resources held by the engine. */
    public native void shutdown();

    // -----------------------------------------------------------------------
    // Java-side convenience API
    // -----------------------------------------------------------------------

    /**
     * Runs a quick integration smoke-test to verify the native library
     * loaded correctly and responds as expected.
     *
     * @throws AssertionError if the native library behaves unexpectedly
     */
    public void selfTest() {
        System.out.println("[SimEngineWrapper] Running self-test …");
        initSimulation(1, 0.016);
        tick();
        byte[] state = getSimulationState();
        if (state == null || state.length == 0) {
            throw new AssertionError(
                "Self-test failed: getSimulationState() returned empty data."
            );
        }
        shutdown();
        System.out.println("[SimEngineWrapper] Self-test PASSED.");
    }
}