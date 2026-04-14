public class SharingUtility {

    private final NativeCameraLibrary nativeLib = new NativeCameraLibrary();

    public void share(byte[] jpegImage) {
        byte[] tagged = nativeLib.tagGeoLocation(jpegImage, 48.8566, 2.3522);
        byte[] thumbnail = nativeLib.generateThumbnail(tagged, 256, 256);
        nativeLib.uploadToSocialMedia(thumbnail, "PhotoShare");
    }
}
