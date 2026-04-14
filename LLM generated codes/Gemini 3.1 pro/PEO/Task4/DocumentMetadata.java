public class DocumentMetadata {
    private int ownerID;
    private long creationTimestamp;
    private boolean isEncrypted;
    private int filePermissions;

    static {
        System.loadLibrary("archivalutility");
    }

    public DocumentMetadata(int ownerID, long creationTimestamp, boolean isEncrypted, int filePermissions) {
        this.ownerID = ownerID;
        this.creationTimestamp = creationTimestamp;
        this.isEncrypted = isEncrypted;
        this.filePermissions = filePermissions;
    }

    // Instance native method. The object itself is passed to C as 'thisObj'
    // Returns a 17-byte array representing the packed binary header
    public native byte[] generateCompactHeader();
}