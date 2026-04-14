import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DocumentMetadata {
    private int ownerID;
    private long creationTimestamp;
    private boolean isEncrypted;
    private int filePermissions;

    // Constructor
    public DocumentMetadata(int ownerID, long creationTimestamp, boolean isEncrypted, int filePermissions) {
        this.ownerID = ownerID;
        this.creationTimestamp = creationTimestamp;
        this.isEncrypted = isEncrypted;
        this.filePermissions = filePermissions;
    }

    // Getters
    public int getOwnerID() { return ownerID; }
    public long getCreationTimestamp() { return creationTimestamp; }
    public boolean isEncrypted() { return isEncrypted; }
    public int getFilePermissions() { return filePermissions; }

    // Native method to serialize to binary header
    public native byte[] serializeToBinaryHeader();

    // Native method to deserialize from binary header
    public native void deserializeFromBinaryHeader(byte[] headerData);

    // Static block to load the native library
    static {
        System.loadLibrary("archival_utility"); // loads libarchival_utility.so or archival_utility.dll
    }
}