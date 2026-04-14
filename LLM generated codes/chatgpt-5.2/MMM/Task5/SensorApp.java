import java.util.Arrays;

public class SensorApp {

    public static void main(String[] args) {

        // Example sensor data stream (3 readings)
        // Values: 100, 250, 1024
        byte[] stream = {
            0x00, 0x00, 0x00, 0x64,
            0x00, 0x00, 0x00, (byte)0xFA,
            0x00, 0x00, 0x04, 0x00
        };

        int[] decoded =
            NativeSensorDecoder.decode(stream);

        System.out.println("Raw stream: " + Arrays.toString(stream));
        System.out.println("Decoded readings: " + Arrays.toString(decoded));
    }
}
