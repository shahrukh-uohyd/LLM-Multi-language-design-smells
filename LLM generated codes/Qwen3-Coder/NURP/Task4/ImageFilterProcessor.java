// ImageFilterProcessor.java
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * A Java application that processes images by applying filters implemented in native code.
 * The native library containing the filter logic is loaded from a local subfolder.
 */
public class ImageFilterProcessor {

    // Load the native image processing library when the class is initialized.
    static {
        loadNativeLibrary();
    }

    /**
     * Loads the native library from the "native_libs" subfolder within the project.
     * This ensures the application is self-contained with its required native components.
     */
    private static void loadNativeLibrary() {
        try {
            String libPath = System.getProperty("user.dir") + 
                           System.getProperty("file.separator") + 
                           "native_libs" +
                           System.getProperty("file.separator") + 
                           System.mapLibraryName("image_filters");
            System.load(libPath);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                "Failed to load native library 'image_filters' from the 'native_libs' subfolder. " +
                "Ensure the library file exists there. Error: " + e.getMessage(), e);
        }
    }

    /**
     * Applies a grayscale filter to an image represented by its pixel array.
     *
     * @param pixels An array of integers representing the RGB pixel values of the image.
     * @param width  The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @return A new array of integers representing the grayscale pixel values.
     */
    public static native int[] applyGrayscaleFilter(int[] pixels, int width, int height);

    /**
     * Applies a blur filter to an image represented by its pixel array.
     *
     * @param pixels An array of integers representing the RGB pixel values of the image.
     * @param width  The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @return A new array of integers representing the blurred pixel values.
     */
    public static native int[] applyBlurFilter(int[] pixels, int width, int height);

    /**
     * Applies a sharpen filter to an image represented by its pixel array.
     *
     * @param pixels An array of integers representing the RGB pixel values of the image.
     * @param width  The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @return A new array of integers representing the sharpened pixel values.
     */
    public static native int[] applySharpenFilter(int[] pixels, int width, int height);

    /**
     * A helper method to convert a BufferedImage object into an integer pixel array,
     * which can be passed to native code.
     *
     * @param image The BufferedImage to convert.
     * @return An integer array of pixel values.
     */
    private static int[] getPixels(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        return pixels;
    }

    /**
     * A helper method to convert an integer pixel array back into a BufferedImage object.
     *
     * @param pixels An integer array of pixel values.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A BufferedImage object created from the pixel array.
     */
    private static BufferedImage createImageFromPixels(int[] pixels, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }

    /**
     * The main method demonstrates the usage of native image filters.
     * It loads an image, applies a filter, and saves the result.
     */
    public static void main(String[] args) {
        try {
            // Load an input image (replace "input.jpg" with your image file)
            File inputFile = new File("input.jpg"); // Example input file
            BufferedImage originalImage = ImageIO.read(inputFile);

            if (originalImage == null) {
                System.err.println("Could not read the input image. Please ensure 'input.jpg' exists.");
                return;
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            int[] originalPixels = getPixels(originalImage);

            // Apply a grayscale filter using the native implementation
            System.out.println("Applying Grayscale Filter...");
            int[] filteredPixels = applyGrayscaleFilter(originalPixels, width, height);

            // Convert the resulting pixel array back to a BufferedImage
            BufferedImage filteredImage = createImageFromPixels(filteredPixels, width, height);

            // Save the processed image
            File outputFile = new File("output_grayscale.jpg");
            ImageIO.write(filteredImage, "jpg", outputFile);

            System.out.println("Grayscale filter applied. Output saved to " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("An error occurred during image processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}