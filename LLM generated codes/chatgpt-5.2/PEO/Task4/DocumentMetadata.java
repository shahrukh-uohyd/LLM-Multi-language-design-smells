public class DocumentMetadata {

    private int ownerID;
    private long creationTimestamp;
    private boolean isEncrypted;
    private int filePermissions;

    static {
        System.loadLibrary("archival");
    }

    public DocumentMetadata(int ownerID,
                            long creationTimestamp,
                            boolean isEncrypted,
                            int filePermissions) {
        this.ownerID = ownerID;
        this.creationTimestamp = creationTimestamp;
        this.isEncrypted = isEncrypted;
        this.filePermissions = filePermissions;
    }

    /**
     * Packs this metadata into a compact binary header.
     * Layout: [ownerID][creationTimestamp][isEncrypted][filePermissions]
     */
    public native byte[] toBinaryHeader();

    // Demo
    public static void main(String[] args) {
        DocumentMetadata meta =
            new DocumentMetadata(42, 1700000000000L, true, 0644);

        byte[] header = meta.toBinaryHeader();
        System.out.println("Header size: " + header.length);
    }
}
