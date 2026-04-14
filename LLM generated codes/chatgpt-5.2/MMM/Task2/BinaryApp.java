import java.util.Arrays;

public class BinaryApp {

    public static void main(String[] args) {

        // Produce binary payload
        byte[] payload = new byte[] {
            0x10, 0x20, 0x30, 0x40
        };

        System.out.println("Original payload: "
                + Arrays.toString(payload));

        // Pass to native code
        byte[] processed =
                NativeBinaryProcessor.process(payload);

        System.out.println("Processed payload: "
                + Arrays.toString(processed));
    }
}
