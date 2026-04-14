public class GalleryPresenter {

    private final NativeCameraLibrary nativeLib = new NativeCameraLibrary();

    public byte[] processImage(byte[] rawImage) {
        byte[] sepia = nativeLib.applySepiaFilter(rawImage);
        int[] faces = nativeLib.detectFaceRectangles(sepia);
        return nativeLib.compressToJpeg(sepia, 85);
    }
}
