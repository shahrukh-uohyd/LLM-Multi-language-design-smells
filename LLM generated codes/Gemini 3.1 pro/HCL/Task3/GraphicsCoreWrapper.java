public class GraphicsCoreWrapper {

    static {
        // List of potential library names to try loading in order of preference
        String[] possibleLibNames = {
            "graphics_core", 
            "libgraphics_core", 
            "graphics_core_v1"
        };
        
        boolean isLibraryLoaded = false;
        StringBuilder errorLog = new StringBuilder();

        for (String libName : possibleLibNames) {
            try {
                // Attempt to load the library
                System.loadLibrary(libName);
                isLibraryLoaded = true;
                
                // Optional: Log which library was successfully loaded
                System.out.println("Successfully loaded native graphics library: " + libName);
                
                // Break out of the loop as soon as we successfully load one
                break; 
            } catch (UnsatisfiedLinkError e) {
                // Library wasn't found or couldn't be loaded. 
                // Append the error message to our log and try the next one.
                errorLog.append("\n - Failed to load '").append(libName).append("': ").append(e.getMessage());
            } catch (SecurityException e) {
                // Handle cases where a SecurityManager prevents loading the library
                errorLog.append("\n - Security exception for '").append(libName).append("': ").append(e.getMessage());
            }
        }

        // If the loop finishes and no library was successfully loaded, we must abort.
        if (!isLibraryLoaded) {
            System.err.println("CRITICAL ERROR: Could not find the native graphics library.");
            System.err.println("Attempted variations and their errors:" + errorLog.toString());
            System.err.println("Please check your java.library.path and ensure the native binary exists.");
            
            // Throwing an error prevents the application from continuing in a broken state
            throw new UnsatisfiedLinkError("No valid variant of the graphics core library could be loaded.");
        }
    }

    /**
     * Example native method declaration for initializing the graphics context.
     */
    public native void initGraphicsContext();

    /**
     * Example native method declaration for rendering a frame.
     */
    public native void renderFrame();
}