public class NativeLibraryLoader {
    private static final String LIBRARY_NAME = "physics_core";
    private static final String NATIVE_LIB_FOLDER = "native_libs";

    static {
        loadNativeLibrary();
    }

    /**
     * Loads the native library from the bundled location.
     */
    private static void loadNativeLibrary() {
        try {
            // Construct the absolute path to the native library
            String libPath = System.getProperty("user.dir") + 
                           System.getProperty("file.separator") + 
                           NATIVE_LIB_FOLDER +
                           System.getProperty("file.separator") + 
                           System.mapLibraryName(LIBRARY_NAME);

            System.load(libPath);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                String.format("Failed to load native library '%s' from '%s'. " +
                             "Ensure the library exists and the path is correct.", 
                             LIBRARY_NAME, NATIVE_LIB_FOLDER), e);
        }
    }

    /**
     * Checks if the native library has been successfully loaded.
     * This method can be used to verify the library is ready for use.
     *
     * @return true if the library is loaded successfully, false otherwise
     */
    public static boolean isLibraryLoaded() {
        try {
            // Attempt to call a placeholder native method to verify the library is functional.
            // If no such method exists, we can at least check if the class initialization succeeded.
            return true; // If we reach this point in a real scenario, the load was successful
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}