import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class DataStorageManager {

    private final PlatformUtils utils = new PlatformUtils();

    public byte[] store(byte[] data) throws Exception {
        // Compression used mainly here
        byte[] compressed = utils.compress(data);
        Files.write(Path.of("data.bin"), compressed);
        return compressed;
    }

    public static void main(String[] args) throws Exception {
        DataStorageManager storage = new DataStorageManager();

        // Existing native utilities used in the platform class
        storage.utils.performPlatformMaintenance();

        // New native compression used primarily here
        byte[] input = "Native compression via JNI".getBytes();
        byte[] compressed = storage.store(input);

        System.out.println("Compressed bytes:");
        System.out.println(Arrays.toString(compressed));
    }
}
