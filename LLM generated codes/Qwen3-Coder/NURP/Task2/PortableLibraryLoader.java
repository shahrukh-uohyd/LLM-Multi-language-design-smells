import java.io.File;

public class PortableLibraryLoader {

    static {
        loadPortableNativeLibrary();
    }

    /**
     * Loads the native library from a 'bin' folder that is a sibling to the application's execution folder.
     */
    private static void loadPortableNativeLibrary() {
        try {
            // Get the parent directory of the current working directory (execution folder)
            File currentDir = new File(System.getProperty("user.dir"));
            File parentDir = currentDir.getParentFile();

            if (parentDir == null) {
                throw new RuntimeException(
                    "Cannot determine parent directory of the execution folder.");
            }

            // Construct the path to the 'bin' sibling folder
            File binFolder = new File(parentDir, "bin");

            // Construct the full path to the native library within the 'bin' folder
            String libName = System.mapLibraryName("data_helper");
            File libFile = new File(binFolder, libName);
            String libPath = libFile.getAbsolutePath();

            System.load(libPath);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                String.format("Failed to load native library 'data_helper' from the expected " +
                              "sibling 'bin' folder. Ensure the library exists at the correct location " +
                              "relative to the application's execution directory."), e);
        }
    }
}