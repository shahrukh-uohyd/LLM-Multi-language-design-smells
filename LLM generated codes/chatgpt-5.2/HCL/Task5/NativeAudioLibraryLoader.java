public final class NativeAudioLibraryLoader {

    private static final String OS_NAME =
            System.getProperty("os.name").toLowerCase();

    private static volatile boolean loaded = false;

    private NativeAudioLibraryLoader() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }

        String libraryName;

        if (OS_NAME.contains("win")) {
            libraryName = "audio_decoder_win";     // audio_decoder_win.dll
        } else if (OS_NAME.contains("linux")) {
            libraryName = "audio_decoder_linux";   // libaudio_decoder_linux.so
        } else if (OS_NAME.contains("mac")) {
            libraryName = "audio_decoder_mac";     // libaudio_decoder_mac.dylib
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
                    "Failed to load native audio library: " + libraryName,
                    e
            );
        }
    }
}
