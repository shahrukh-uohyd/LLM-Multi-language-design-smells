public class FinancialAccount {
    private String accountNumber;
    private String customerName;
    private String accountType;
    private double balance;
    private double creditLimit;
    private double interestRate;
    private String accountStatus;

    // Constructor
    public FinancialAccount(String accountNumber, String customerName, String accountType,
                           double balance, double creditLimit, double interestRate, String accountStatus) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.accountType = accountType;
        this.balance = balance;
        this.creditLimit = creditLimit;
        this.interestRate = interestRate;
        this.accountStatus = accountStatus;
    }

    // Getters
    public String getAccountNumber() { return accountNumber; }
    public String getCustomerName() { return customerName; }
    public String getAccountType() { return accountType; }
    public double getBalance() { return balance; }
    public double getCreditLimit() { return creditLimit; }
    public double getInterestRate() { return interestRate; }
    public String getAccountStatus() { return accountStatus; }

    // Setters
    public void setBalance(double balance) { this.balance = balance; }
    public void setCreditLimit(double creditLimit) { this.creditLimit = creditLimit; }

    // Native method declarations
    public native double calculateRiskMetric();
    
    // Alternative native method that allows for custom risk calculation parameters
    public native double calculateRiskMetricWithWeight(double balanceWeight, double creditLimitWeight);

    // Static block to load the native library
    static {
        System.loadLibrary("financial_risk"); // loads libfinancial_risk.so (Linux/Mac) or financial_risk.dll (Windows)
    }
}