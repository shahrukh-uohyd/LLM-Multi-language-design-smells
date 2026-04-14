public class FinancialAccount {
    // Financial information fields
    private String accountNumber;
    private String customerName;
    private String accountType;
    private double balance;
    private double creditLimit;
    private double interestRate;
    private String accountStatus;

    static {
        // Load the native library (e.g., libriskcalculator.so or riskcalculator.dll)
        System.loadLibrary("riskcalculator");
    }

    public FinancialAccount(double balance, double creditLimit) {
        this.balance = balance;
        this.creditLimit = creditLimit;
    }

    // Instance native method. The object itself is passed to C++ as 'thisObj'
    public native double calculateRiskMetric();
}