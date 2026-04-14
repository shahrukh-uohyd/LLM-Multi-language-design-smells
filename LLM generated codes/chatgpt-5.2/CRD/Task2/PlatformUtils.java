public class PlatformUtils {

    static {
        System.loadLibrary("platformutils");
    }

    // Existing native methods (platform utilities)
    public native long getCurrentTimeMillis();
    public native int getCpuCount();
    public native void nativeSleep(int millis);

    // New native feature: compression
    public native byte[] compress(byte[] input);

    // Internal platform work that uses existing natives
    public void performPlatformMaintenance() {
        for (int i = 0; i < 3; i++) {
            System.out.println(
                "[Platform] time=" + getCurrentTimeMillis() +
                ", cpu=" + getCpuCount()
            );
            nativeSleep(100);
        }
    }
}
