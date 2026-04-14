import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {

        DocumentMetadata doc = new DocumentMetadata(
            1001,                   // ownerID
            1_708_473_600_000L,     // creationTimestamp (ms since epoch)
            true,                   // isEncrypted
            0b110_100_000           // filePermissions (octal 640 → rw-r-----)
        );

        // Write binary header to file
        doc.writeHeaderToFile(Path.of("archive_header.bin"));

        // Or get the raw bytes directly
        byte[] header = doc.serializeToHeaderApproachB();

        System.out.println("Header size: " + header.length + " bytes");
        System.out.printf("Expected:    %d bytes%n", DocumentMetadata.HEADER_SIZE_BYTES);

        // Hex dump for verification
        System.out.print("Hex: ");
        for (byte b : header) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }
}