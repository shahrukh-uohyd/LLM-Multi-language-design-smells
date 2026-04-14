import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        try {
            // 1. Load the native JNI library
            ImageFilter.loadLibrary();
            ImageFilter filter = new ImageFilter();

            // 2. Generate a test image (a vibrant color gradient)
            int width = 800;
            int height = 600;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int r = (x * 255) / width;
                    int g = (y * 255) / height;
                    int b = 150; // Constant blue
                    int a = 255; // Fully opaque
                    
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    img.setRGB(x, y, argb);
                }
            }

            // Save the original colorful image
            File originalFile = new File("original_image.png");
            ImageIO.write(img, "png", originalFile);
            System.out.println("Saved original color image: " + originalFile.getAbsolutePath());

            // 3. Extract the raw pixel array from the image
            // This grabs the 2D image into a flat 1D integer array
            int[] pixels = img.getRGB(0, 0, width, height, null, 0, width);

            // 4. Delegate filtering task to the native C code
            System.out.println("Applying native grayscale filter...");
            long startTime = System.currentTimeMillis();
            
            filter.applyGrayscale(pixels, width, height);
            
            long endTime = System.currentTimeMillis();
            System.out.println("Filter applied in native code in " + (endTime - startTime) + "ms");

            // 5. Push the modified native pixels back into the Java BufferedImage
            img.setRGB(0, 0, width, height, pixels, 0, width);

            // Save the filtered image
            File filteredFile = new File("filtered_image.png");
            ImageIO.write(img, "png", filteredFile);
            System.out.println("Saved filtered grayscale image: " + filteredFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}