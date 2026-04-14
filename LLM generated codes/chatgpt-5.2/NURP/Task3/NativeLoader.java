import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class NativeLoader {

    private static boolean loaded = false;

    private NativeLoader() {}

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String libName;

        if (os.contains("win")) {
            libName = "math_native.dll";
        } else if (os.contains("mac")) {
            libName = "libmath_native.dylib";
        } else {
            libName = "libmath_native.so";
        }

        Path baseDir = Paths.get(System.getProperty("user.dir"));
        Path libPath = baseDir.resolve("native")
                              .resolve("build")
                              .resolve(libName);

        File libFile = libPath.toFile();
        if (!libFile.exists()) {
            throw new UnsatisfiedLinkError(
                "Native library not found: " + libFile.getAbsolutePath()
            );
        }

        System.load(libFile.getAbsolutePath());
        loaded = true;
    }
}
