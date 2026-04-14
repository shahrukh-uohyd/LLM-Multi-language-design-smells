/**
 * Exercises all three native image encoders.
 *
 * Compile & run:
 *   ./build.sh run
 * or manually:
 *   javac ImageEncoder.java Main.java -d out
 *   # (generate header + compile .so — see build.sh)
 *   java -Djava.library.path=./native/build -cp out Main
 */
public class Main {

    public static void main(String[] args) {

        ImageEncoder encoder = new ImageEncoder();

        // ------------------------------------------------------------ //
        // Library metadata                                               //
        // ------------------------------------------------------------ //
        System.out.println("=== Native Library Info ===");
        System.out.println(encoder.getNativeLibraryInfo());
        System.out.println();

        // ------------------------------------------------------------ //
        // Synthetic 4×4 RGBA test image                                  //
        // ------------------------------------------------------------ //
        int    width    = 4;
        int    height   = 4;
        int    channels = 4;            // RGBA
        byte[] pixels   = makeTestImage(width, height, channels);

        System.out.printf("Image: %d×%d px, %d ch, %d bytes total%n%n",
                          width, height, channels, pixels.length);

        // ------------------------------------------------------------ //
        // Base64                                                         //
        // ------------------------------------------------------------ //
        ImageEncoder.EncodingResult b64 =
                encoder.encode(pixels, width, height, channels, "base64");
        System.out.println("=== Base64 ===");
        System.out.println(b64);
        System.out.println("Value: " + b64.getEncodedString());
        System.out.println();

        // ------------------------------------------------------------ //
        // RLE                                                            //
        // ------------------------------------------------------------ //
        ImageEncoder.EncodingResult rle =
                encoder.encode(pixels, width, height, channels, "rle");
        System.out.println("=== RLE ===");
        System.out.println(rle);
        System.out.println("Pairs: " + rleToString(rle.getEncodedBytes()));
        System.out.printf("Compression: %.1f%%  (%d → %d bytes)%n%n",
                          100.0 * rle.getEncodedSize() / pixels.length,
                          pixels.length, rle.getEncodedSize());

        // ------------------------------------------------------------ //
        // Hex                                                            //
        // ------------------------------------------------------------ //
        ImageEncoder.EncodingResult hex =
                encoder.encode(pixels, width, height, channels, "hex");
        System.out.println("=== Hex ===");
        System.out.println(hex);
        System.out.println("Value: " + hex.getEncodedString());
        System.out.println();

        // ------------------------------------------------------------ //
        // Validation error paths                                         //
        // ------------------------------------------------------------ //
        System.out.println("=== Validation ===");
        try {
            encoder.encode(new byte[5], width, height, channels, "base64");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught (expected): " + e.getMessage());
        }
        try {
            encoder.encode(pixels, width, height, channels, "jpeg");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught (expected): " + e.getMessage());
        }

        System.out.println("\nDone.");
    }

    // ------------------------------------------------------------------ //
    // Helpers                                                             //
    // ------------------------------------------------------------------ //

    /** Creates a synthetic RGBA checkerboard-gradient image. */
    private static byte[] makeTestImage(int w, int h, int c) {
        byte[] data = new byte[w * h * c];
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean checker = ((x + y) % 2 == 0);
                data[idx++] = checker ? (byte) 0xFF : (byte)(x * 64); // R
                data[idx++] = checker ? (byte) 0x00 : (byte)(y * 64); // G
                data[idx++] = checker ? (byte) 0x80 : (byte) 0xFF;    // B
                data[idx++] = (byte) 0xFF;                             // A
            }
        }
        return data;
    }

    /** Pretty-prints RLE output as [(count,0xVAL), ...] */
    private static String rleToString(byte[] rle) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i + 1 < rle.length; i += 2) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("(%d,0x%02X)", rle[i] & 0xFF, rle[i + 1] & 0xFF));
        }
        return sb.append("]").toString();
    }
}