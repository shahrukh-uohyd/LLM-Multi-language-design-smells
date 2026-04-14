public final class NativeSolverLibraryLoader {

    private static final String OS_NAME =
            System.getProperty("os.name").toLowerCase();

    private static volatile boolean loaded = false;

    private NativeSolverLibraryLoader() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String libraryName;

        if (OS_NAME.contains("win")) {
            libraryName = "solver_native";     // solver_native.dll
        } else if (OS_NAME.contains("linux")) {
            libraryName = "solver_native";     // libsolver_native.so
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported operating system: " + OS_NAME
            );
        }

        try {
            System.loadLibrary(libraryName);
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            throw new IllegalStateException(
                    "Failed to load native solver library: " + libraryName,
                    e
            );
        }
    }
}
