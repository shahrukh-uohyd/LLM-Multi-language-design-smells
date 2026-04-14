public class TransactionRecord {
    public final String transactionId;
    public final double amount;
    public final long timestamp;

    public TransactionRecord(String transactionId, double amount, long timestamp) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.timestamp = timestamp;
    }
}