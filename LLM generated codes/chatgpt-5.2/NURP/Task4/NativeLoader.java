import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class NativeLoader {

    private static boolean loaded = false;

    private NativeLoader() {}

    public static synchronized void load() {
        if (loaded) return;

        String os = System.getProperty("os.name").toLowerCase();
        String libName;

        if (os.contains("win")) {
            libName = "image_filter.dll";
        } else if (os.contains("mac")) {
            libName = "libimage_filter.dylib";
        } else {
            libName = "libimage_filter.so";
        }

        Path libPath = Paths.get(System.getProperty("user.dir"))
                            .resolve("native")
                            .resolve("build")
                            .resolve(libName);

        File libFile = libPath.toFile();
        if (!libFile.exists()) {
            throw new UnsatisfiedLinkError(
                "Native image filter library not found: " +
                libFile.getAbsolutePath()
            );
        }

        System.load(libFile.getAbsolutePath());
        loaded = true;
    }
}
