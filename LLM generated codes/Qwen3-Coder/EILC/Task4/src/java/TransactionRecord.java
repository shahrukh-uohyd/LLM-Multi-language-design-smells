// TransactionRecord.java
public class TransactionRecord {
    private String transactionId;
    private String accountId;
    private double amount;
    private String currency;
    private long timestamp;
    private String description;
    private String status;
    
    public TransactionRecord(String transactionId, String accountId, double amount, 
                           String currency, long timestamp, String description, String status) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.description = description;
        this.status = status;
    }
    
    // Getters
    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public long getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    
    @Override
    public String toString() {
        return String.format("TransactionRecord{id='%s', account='%s', amount=%.2f, currency='%s'}", 
                           transactionId, accountId, amount, currency);
    }
}