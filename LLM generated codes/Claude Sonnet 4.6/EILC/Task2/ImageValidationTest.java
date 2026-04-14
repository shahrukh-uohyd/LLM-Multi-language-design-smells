package com.example.imageservice;

import com.example.imageservice.model.UploadedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Standalone driver that exercises the full validation pipeline
 * without a framework.
 */
public class ImageValidationTest {

    public static void main(String[] args) {
        List<UploadedImage> batch = Arrays.asList(
            // Valid JPEG (FF D8 FF header, FF D9 footer)
            new UploadedImage("photo.jpg", makeJpegBytes()),

            // Valid PNG
            new UploadedImage("graphic.png", makePngBytes()),

            // Unsupported / unknown format
            new UploadedImage("archive.zip", new byte[]{ 0x50, 0x4B, 0x03, 0x04 }),

            // Too short to identify
            new UploadedImage("tiny.dat",    new byte[]{ 0x01, 0x02 }),

            // JPEG magic bytes but truncated body (no EOI)
            new UploadedImage("bad.jpg",
                new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0,
                            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01 })
        );

        new ImageUploadService().handleUploadBatch(batch);
    }

    // ── Minimal valid JPEG stub ────────────────────────────────────────────────
    private static byte[] makeJpegBytes() {
        // SOI (FF D8) + APP0 marker (FF E0) + minimal payload + EOI (FF D9)
        return new byte[]{
            (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            (byte)0xFF, (byte)0xD9  // EOI
        };
    }

    // ── Minimal valid PNG stub ────────────────────────────────────────────────
    private static byte[] makePngBytes() {
        return new byte[]{
            // PNG signature
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            // IHDR chunk length (13 bytes)
            0x00, 0x00, 0x00, 0x0D,
            // "IHDR"
            'I', 'H', 'D', 'R',
            // width = 1
            0x00, 0x00, 0x00, 0x01,
            // height = 1
            0x00, 0x00, 0x00, 0x01,
            // bit depth, color type, compression, filter, interlace, CRC
            0x08, 0x02, 0x00, 0x00, 0x00, (byte)0x90, 0x77, 0x53, (byte)0xDE,
            // IEND chunk (length 0, type, CRC)
            0x00, 0x00, 0x00, 0x00,
            'I', 'E', 'N', 'D',
            (byte)0xAE, 0x42, 0x60, (byte)0x82
        };
    }
}