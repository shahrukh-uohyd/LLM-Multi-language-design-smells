public class ImageEncoder {

    // Load the native C/C++ library during class initialization
    static {
        // The library will be named libimageencoder.so (Linux), libimageencoder.dylib (Mac), or imageencoder.dll (Windows)
        System.loadLibrary("imageencoder");
    }

    /**
     * Native method to encode raw image pixels using a native implementation.
     * 
     * @param rawPixels The raw image data (e.g., grayscale pixel values).
     * @param width     The width of the image.
     * @param height    The height of the image.
     * @return          A byte array containing the encoded image data.
     */
    public native byte[] encodeImage(byte[] rawPixels, int width, int height);

    public static void main(String[] args) {
        ImageEncoder encoder = new ImageEncoder();

        // Simulate a 4x4 raw grayscale image (16 pixels total)
        // Notice the repeating pixel values, which are perfect for Run-Length Encoding.
        byte[] rawImage = {
            10, 10, 10, 10,
            20, 20, 30, 30,
            50, 50, 50, 50,
            15, 15, 15, 15
        };
        int width = 4;
        int height = 4;

        System.out.println("Original image size: " + rawImage.length + " bytes");

        // Call the native encoding method
        byte[] encodedImage = encoder.encodeImage(rawImage, width, height);

        System.out.println("Encoded image size: " + encodedImage.length + " bytes");
        
        // Print the encoded data
        // RLE format produced by our native code: [count, pixel_value, count, pixel_value...]
        System.out.print("Encoded data (RLE pairs [count, value]): ");
        for (byte b : encodedImage) {
            System.out.print(b + " ");
        }
        System.out.println();
    }
}