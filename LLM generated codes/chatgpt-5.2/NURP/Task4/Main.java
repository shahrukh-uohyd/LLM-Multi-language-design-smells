import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) throws Exception {

        BufferedImage input = ImageIO.read(new File("input.png"));

        int width = input.getWidth();
        int height = input.getHeight();

        int[] pixels = new int[width * height];
        input.getRGB(0, 0, width, height, pixels, 0, width);

        // Native filtering
        int[] filtered = NativeImageFilter.grayscale(pixels, width, height);

        BufferedImage output =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        output.setRGB(0, 0, width, height, filtered, 0, width);

        ImageIO.write(output, "png", new File("output.png"));

        System.out.println("Grayscale filter applied successfully.");
    }
}
