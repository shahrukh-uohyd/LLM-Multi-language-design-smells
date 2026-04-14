/**
 * CameraMain
 *
 * End-to-end demonstration of all three Camera Application components
 * sharing a single {@link CameraApplication} bridge instance.
 *
 * Simulated workflow:
 *   1. CameraHardware  — focus → shutter open
 *   2. GalleryPresenter — sepia → face detection → JPEG compression
 *   3. SharingUtility  — geo-tag → thumbnail → social upload
 */
public class CameraMain {

    public static void main(String[] args) {

        // One bridge — one native library load for the entire session.
        CameraApplication app = new CameraApplication();

        printBanner("Automotive Camera Application — Boot");

        // ── 1. Camera Hardware ───────────────────────────────────────
        printBanner("1 · Camera Hardware");
        CameraHardware hardware = new CameraHardware(app, 0 /* rear camera */);

        // Auto-focus then capture with a 120 ms exposure
        hardware.focusAndCapture(CameraApplication.FocusMode.AUTO, 120);

        // Switch to macro mode for a close-up shot, manual: no distance needed
        hardware.setFocusMode(CameraApplication.FocusMode.MACRO, 0f);

        // Long-exposure landscape shot at infinity focus
        hardware.focusAndCapture(CameraApplication.FocusMode.INFINITY, 2000);

        System.out.println();

        // ── 2. Gallery Presenter ─────────────────────────────────────
        printBanner("2 · Gallery Presenter");
        GalleryPresenter gallery = new GalleryPresenter(app);

        // Synthesise a 4×4 px RGBA test frame (64 bytes of dummy data)
        final int W = 4, H = 4;
        byte[] fakeRgba = new byte[W * H * 4];
        for (int i = 0; i < fakeRgba.length; i++) fakeRgba[i] = (byte) (i & 0xFF);

        // Full pipeline: sepia → face detection → JPEG
        byte[] jpeg = gallery.processCapturedFrame(
                fakeRgba, W, H, GalleryPresenter.JPEG_QUALITY_HIGH);
        System.out.printf("[Main] Final JPEG from pipeline: %s%n",
                jpeg != null ? jpeg.length + " bytes" : "null (native not loaded)");

        System.out.println();

        // ── 3. Sharing Utility ───────────────────────────────────────
        printBanner("3 · Sharing Utility");
        SharingUtility sharing = new SharingUtility(app);

        if (jpeg != null) {
            // Full sharing pipeline: geo-tag → thumbnail → upload
            CameraApplication.UploadResult uploadResult = sharing.geoTagAndShare(
                    jpeg,
                    48.8566,  9.1825,   // Paris (lat, lon)
                    35.0,               // 35 m above sea level
                    "instagram",
                    "oauth-token-placeholder");

            System.out.printf("[Main] Upload outcome: %s%n", uploadResult);
        } else {
            // Native library not loaded — exercise individual calls with stubs
            byte[] stubJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00};

            sharing.tagGeoLocation(stubJpeg, 48.8566, 9.1825, 35.0);
            sharing.generateThumbnail(stubJpeg, 160, 120);
            sharing.uploadToSocialMedia(stubJpeg, "twitter", "oauth-token-placeholder");
        }

        System.out.println();
        printBanner("Session Complete");
    }

    // ----------------------------------------------------------------
    private static void printBanner(String title) {
        String bar = "─".repeat(title.length() + 4);
        System.out.println("┌" + bar + "┐");
        System.out.println("│  " + title + "  │");
        System.out.println("└" + bar + "┘");
    }
}