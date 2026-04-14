public class CameraApplication {

    public static void main(String[] args) {
        CameraController camera = new CameraController();
        GalleryPresenter gallery = new GalleryPresenter();
        SharingUtility sharing = new SharingUtility();

        camera.capturePhoto();

        byte[] rawImage = new byte[0]; // placeholder
        byte[] jpeg = gallery.processImage(rawImage);

        sharing.share(jpeg);
    }
}
