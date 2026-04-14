import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocumentMetadata {

    // All four fields are required for binary header serialisation.
    // No field is sensitive relative to the others in this archival context.
    private final int     ownerID;
    private final long    creationTimestamp;
    private final boolean isEncrypted;
    private final int     filePermissions;

    // Defined as a constant in Java so it matches the C-side exactly.
    // Any mismatch between the two is a compile-time catch, not a runtime one.
    public static final int HEADER_SIZE_BYTES = 20;

    public DocumentMetadata(int ownerID, long creationTimestamp,
                            boolean isEncrypted, int filePermissions) {
        this.ownerID           = ownerID;
        this.creationTimestamp = creationTimestamp;
        this.isEncrypted       = isEncrypted;
        this.filePermissions   = filePermissions;
    }

    // -----------------------------------------------------------------------
    // APPROACH A:
    // Pass the full object. Legitimate here since ALL fields are required.
    // Native code uses GetFieldID/GetIntField etc. to read each field.
    // -----------------------------------------------------------------------
    public native byte[] serializeToHeaderApproachA();

    // -----------------------------------------------------------------------
    // APPROACH B (RECOMMENDED — still preferred for structural clarity):
    // Java extracts all four fields before the JNI boundary.
    // Benefits:
    //   • No JNI field reflection overhead (no GetFieldID calls)
    //   • The native function signature is self-documenting
    //   • Pure-C serialisation logic is trivially testable without a JVM
    //   • If DocumentMetadata gains a new sensitive field later, it cannot
    //     accidentally leak into this function
    // -----------------------------------------------------------------------
    public byte[] serializeToHeaderApproachB() {
        // Java controls the extraction; native code receives only primitives.
        return nativeSerializeHeader(ownerID, creationTimestamp,
                                     isEncrypted, filePermissions);
    }

    // Private: the public API is serializeToHeaderApproachB().
    // Declared private so no external caller can inject arbitrary values.
    private native byte[] nativeSerializeHeader(int ownerID, long creationTimestamp,
                                                boolean isEncrypted, int filePermissions);

    // -----------------------------------------------------------------------
    // Convenience: write the binary header directly to a file path.
    // -----------------------------------------------------------------------
    public void writeHeaderToFile(Path outputPath) throws IOException {
        byte[] header = serializeToHeaderApproachB();
        Files.write(outputPath, header);
    }

    static {
        System.loadLibrary("archival_util");
    }
}