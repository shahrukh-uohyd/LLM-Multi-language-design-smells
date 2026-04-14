public class FinancialAccount {

    // Financial data
    private String accountNumber;
    private String customerName;
    private String accountType;
    private double balance;
    private double creditLimit;
    private double interestRate;
    private String accountStatus;

    static {
        System.loadLibrary("financial");
    }

    public FinancialAccount(String accountNumber,
                            String customerName,
                            String accountType,
                            double balance,
                            double creditLimit,
                            double interestRate,
                            String accountStatus) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.accountType = accountType;
        this.balance = balance;
        this.creditLimit = creditLimit;
        this.interestRate = interestRate;
        this.accountStatus = accountStatus;
    }

    /**
     * Native risk metric calculation.
     * Uses only balance and creditLimit.
     */
    public native double calculateRiskMetric();

    // Demo
    public static void main(String[] args) {
        FinancialAccount account = new FinancialAccount(
            "ACC-1001",
            "Jane Doe",
            "CREDIT",
            7500.0,
            10000.0,
            0.18,
            "ACTIVE"
        );

        double risk = account.calculateRiskMetric();
        System.out.println("Risk metric: " + risk);
    }
}
