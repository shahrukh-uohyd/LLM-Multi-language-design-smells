package engine.nativeutils;

import java.io.File;

public final class PhysicsCoreLoader {

    private static boolean loaded = false;

    private PhysicsCoreLoader() {
        // Prevent instantiation
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String osName = System.getProperty("os.name").toLowerCase();
        String libName;

        if (osName.contains("win")) {
            libName = "physics_core.dll";
        } else if (osName.contains("mac")) {
            libName = "libphysics_core.dylib";
        } else {
            // Assume Linux / Unix
            libName = "libphysics_core.so";
        }

        File libPath = new File("native_libs", libName);

        if (!libPath.exists()) {
            throw new UnsatisfiedLinkError(
                "Native library not found: " + libPath.getAbsolutePath()
            );
        }

        System.load(libPath.getAbsolutePath());
        loaded = true;
    }
}
