public class SharingUtility {

    static {
        System.loadLibrary("camera_native");
    }

    // Native declarations for metadata and networking
    private native byte[] tagGeoLocation(byte[] jpegData, double latitude, double longitude);
    private native byte[] generateThumbnail(byte[] imageData, int width, int height);
    private native boolean uploadToSocialMedia(byte[] imageData, String platform, String caption);

    /**
     * Prepares the image with EXIF data, generates a UI thumbnail, 
     * and uploads the high-res photo to a remote server.
     */
    public void sharePhoto(byte[] jpegImage, double lat, double lon, String platform, String caption) {
        // 1. Embed GPS coordinates into the image's EXIF metadata
        System.out.println("Writing EXIF GPS data: [" + lat + ", " + lon + "]");
        byte[] geotaggedImage = tagGeoLocation(jpegImage, lat, lon);
        
        // 2. Generate a small thumbnail to display in the app's UI immediately
        byte[] thumbnail = generateThumbnail(geotaggedImage, 256, 256);
        System.out.println("Generated 256x256 UI thumbnail (" + thumbnail.length + " bytes).");
        
        // 3. Execute the network upload via C++ backend
        System.out.println("Uploading full-resolution image to " + platform + "...");
        boolean isUploaded = uploadToSocialMedia(geotaggedImage, platform, caption);
        
        if (isUploaded) {
            System.out.println("Upload successful! Your photo is now live on " + platform + ".");
        } else {
            System.err.println("Network Error: Failed to upload image to " + platform + ".");
        }
    }
}