public final class GraphicsNative {

    static {
        loadNativeLibrary();
    }

    private GraphicsNative() {
        // utility class
    }

    private static void loadNativeLibrary() {
        String[] candidates = {
            "graphics_core",
            "libgraphics_core",
            "graphics_core_v1"
        };

        UnsatisfiedLinkError lastError = null;

        for (String lib : candidates) {
            try {
                System.loadLibrary(lib);
                // Successfully loaded — stop trying
                return;
            } catch (UnsatisfiedLinkError e) {
                lastError = e;
            }
        }

        // If we get here, none of the candidates worked
        throw new IllegalStateException(
                "Failed to load native graphics library. Tried: " +
                String.join(", ", candidates),
                lastError
        );
    }
}
