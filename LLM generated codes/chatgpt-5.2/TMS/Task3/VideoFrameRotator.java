package com.example.multimedia.nativebridge;

public final class VideoFrameRotator {

    static {
        System.loadLibrary("video_frame_rotator_native");
    }

    private VideoFrameRotator() {
        // utility class
    }

    /**
     * Rotates a raw video frame buffer.
     *
     * @param frameBuffer input frame buffer (e.g. RGB/RGBA)
     * @param width frame width in pixels
     * @param height frame height in pixels
     * @param rotationDegrees rotation angle (90, 180, or 270)
     * @return rotated frame buffer
     */
    public static native byte[] rotateFrame(
            byte[] frameBuffer,
            int width,
            int height,
            int rotationDegrees
    );
}
