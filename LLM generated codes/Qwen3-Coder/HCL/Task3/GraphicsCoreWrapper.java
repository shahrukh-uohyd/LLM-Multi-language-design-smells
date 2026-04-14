public class GraphicsCoreWrapper {
    
    private static final String[] POSSIBLE_LIBRARY_NAMES = {
        "graphics_core",
        "libgraphics_core", 
        "graphics_core_v1"
    };
    
    static {
        boolean loaded = false;
        
        for (String libName : POSSIBLE_LIBRARY_NAMES) {
            try {
                System.loadLibrary(libName);
                loaded = true;
                break;
            } catch (UnsatisfiedLinkError e) {
                // Try next name
                continue;
            }
        }
        
        if (!loaded) {
            throw new RuntimeException(
                "Could not load graphics core library. Tried: " + 
                String.join(", ", POSSIBLE_LIBRARY_NAMES)
            );
        }
    }
}