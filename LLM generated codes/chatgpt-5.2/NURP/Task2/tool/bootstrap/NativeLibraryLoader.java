package tool.bootstrap;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class NativeLibraryLoader {

    private static boolean loaded = false;

    private NativeLibraryLoader() {
        // no instances
    }

    public static synchronized void loadDataHelper() {
        if (loaded) {
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String libFileName;

        if (os.contains("win")) {
            libFileName = "data_helper.dll";
        } else if (os.contains("mac")) {
            libFileName = "libdata_helper.dylib";
        } else {
            // Linux / Unix
            libFileName = "libdata_helper.so";
        }

        // JVM working directory (where the app is launched from)
        Path executionDir = Paths.get(System.getProperty("user.dir"));

        // Sibling "bin" directory
        Path binDir = executionDir.getParent().resolve("bin");

        File nativeLib = binDir.resolve(libFileName).toFile();

        if (!nativeLib.exists()) {
            throw new UnsatisfiedLinkError(
                "Native library not found: " + nativeLib.getAbsolutePath()
            );
        }

        System.load(nativeLib.getAbsolutePath());
        loaded = true;
    }
}
