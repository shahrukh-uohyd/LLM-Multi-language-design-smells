public class CameraController {

    private final NativeCameraLibrary nativeLib = new NativeCameraLibrary();

    public void capturePhoto() {
        nativeLib.openShutter();
        nativeLib.setFocusMode(1); // auto-focus
    }
}
