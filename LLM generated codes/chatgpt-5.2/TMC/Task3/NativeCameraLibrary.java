public class NativeCameraLibrary {

    static {
        System.loadLibrary("native_camera");
    }

    /* Camera hardware related */
    public native void openShutter();
    public native void setFocusMode(int mode);

    /* Gallery-related processing */
    public native byte[] applySepiaFilter(byte[] imageData);
    public native int[] detectFaceRectangles(byte[] imageData);
    public native byte[] compressToJpeg(byte[] imageData, int quality);

    /* Sharing-related utilities */
    public native boolean uploadToSocialMedia(byte[] imageData, String platform);
    public native byte[] tagGeoLocation(byte[] imageData, double lat, double lon);
    public native byte[] generateThumbnail(byte[] imageData, int width, int height);
}
