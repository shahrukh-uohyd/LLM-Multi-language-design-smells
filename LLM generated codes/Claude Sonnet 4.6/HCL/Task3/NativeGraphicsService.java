/**
 * Example consumer that declares JNI native methods backed by the
 * graphics-core library.
 *
 * <p>Referencing this class triggers {@link GraphicsCoreLibraryLoader}'s
 * static initialiser, which loads the correct library variant automatically.</p>
 */
public final class NativeGraphicsService {

    // Ensure the loader runs before any native method is invoked.
    static {
        // Referencing the loader class is enough to fire its static initialiser.
        // The explicit call to getLoadedLibraryName() also serves as a fast-fail
        // sanity check: it will throw IllegalStateException if loading failed
        // silently (should never happen, but defensive is good).
        String lib = GraphicsCoreLibraryLoader.getLoadedLibraryName();
        System.out.println("[NativeGraphicsService] Backed by native library: " + lib);
    }

    // -----------------------------------------------------------------------
    // JNI native method declarations
    // -----------------------------------------------------------------------

    /** Initialises the graphics context with the specified resolution. */
    public native void initGraphicsContext(int width, int height);

    /** Renders a frame and returns the pixel buffer. */
    public native byte[] renderFrame();

    /** Releases all native graphics resources. */
    public native void destroyContext();

    // -----------------------------------------------------------------------
    // Java-side convenience wrapper
    // -----------------------------------------------------------------------

    /**
     * Quick smoke-test to verify the native library is functional.
     *
     * @throws AssertionError if the library returns unexpected results
     */
    public void selfTest() {
        System.out.println("[NativeGraphicsService] Running self-test …");
        initGraphicsContext(800, 600);
        byte[] frame = renderFrame();
        if (frame == null || frame.length == 0) {
            throw new AssertionError(
                "Self-test failed: renderFrame() returned empty pixel buffer."
            );
        }
        destroyContext();
        System.out.println("[NativeGraphicsService] Self-test PASSED. "
            + "Frame buffer size: " + frame.length + " bytes.");
    }
}