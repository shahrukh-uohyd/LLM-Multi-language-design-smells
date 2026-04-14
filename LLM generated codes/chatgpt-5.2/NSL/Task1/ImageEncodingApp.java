import java.util.Arrays;

public class ImageEncodingApp {

    public static void main(String[] args) {

        // Simple 4x4 grayscale image
        byte[] pixels = {
            10, 10, 10, 10,
            20, 20, 20, 20,
            20, 20, 20, 20,
            30, 30, 30, 30
        };

        int width = 4;
        int height = 4;

        byte[] encoded =
            NativeImageEncoder.encode(pixels, width, height);

        System.out.println("Original size: " + pixels.length);
        System.out.println("Encoded size: " + encoded.length);
        System.out.println("Encoded data: " + Arrays.toString(encoded));
    }
}
