import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class SecurityService {

    private final NativeFileSystem fs = new NativeFileSystem();

    public byte[] hashFile(String path) throws Exception {
        byte[] data = Files.readAllBytes(Path.of(path));
        return fs.computeHash(data);
    }

    public static void main(String[] args) throws Exception {
        SecurityService security = new SecurityService();

        // Prepare a real file
        Path file = Path.of("secure.txt");
        Files.writeString(file, "JNI crypto integration");

        // Existing native file-system operations
        security.fs.printFileInfo("secure.txt");

        // New native cryptographic feature (used mainly here)
        byte[] hash = security.hashFile("secure.txt");
        System.out.println("Native hash: " + Arrays.toString(hash));
    }
}
