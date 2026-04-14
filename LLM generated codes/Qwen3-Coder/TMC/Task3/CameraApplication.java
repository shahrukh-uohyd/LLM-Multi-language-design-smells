// CameraApplication.java
public class CameraApplication {
    static {
        System.loadLibrary("camera_native");
    }

    // Camera hardware methods
    private native void openShutter();
    private native void setFocusMode(int mode);

    // Gallery presenter methods
    private native byte[] applySepiaFilter(byte[] imageData);
    private native int[][] detectFaceRectangles(byte[] imageData);
    private native byte[] compressToJpeg(byte[] imageData, int quality);

    // Sharing utility methods
    private native boolean uploadToSocialMedia(byte[] imageData, String platform);
    private native void tagGeoLocation(double latitude, double longitude);
    private native byte[] generateThumbnail(byte[] imageData, int width, int height);

    private static final CameraApplication INSTANCE = new CameraApplication();
    
    private CameraApplication() {}
    
    public static CameraApplication getInstance() {
        return INSTANCE;
    }
}

class CameraHardware {
    private CameraApplication app = CameraApplication.getInstance();

    public void captureImage() {
        app.openShutter();
    }

    public void changeFocus(int focusMode) {
        app.setFocusMode(focusMode);
    }
}

class GalleryPresenter {
    private CameraApplication app = CameraApplication.getInstance();

    public byte[] processImage(byte[] image) {
        return app.applySepiaFilter(image);
    }

    public int[][] findFaces(byte[] image) {
        return app.detectFaceRectangles(image);
    }

    public byte[] saveAsJpeg(byte[] image, int quality) {
        return app.compressToJpeg(image, quality);
    }
}

class SharingUtility {
    private CameraApplication app = CameraApplication.getInstance();

    public boolean shareImage(byte[] image, String socialPlatform) {
        return app.uploadToSocialMedia(image, socialPlatform);
    }

    public void addLocation(double lat, double lon) {
        app.tagGeoLocation(lat, lon);
    }

    public byte[] createPreview(byte[] image, int width, int height) {
        return app.generateThumbnail(image, width, height);
    }
}