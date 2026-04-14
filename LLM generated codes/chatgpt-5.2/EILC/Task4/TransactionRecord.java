public final class TransactionRecord {
    private final long id;
    private final byte[] payload;

    public TransactionRecord(long id, byte[] payload) {
        this.id = id;
        this.payload = payload;
    }

    public long getId() {
        return id;
    }

    public byte[] getPayload() {
        return payload;
    }
}
