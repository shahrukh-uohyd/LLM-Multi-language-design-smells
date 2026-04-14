public class FinancialAccount {

    private String accountNumber;   // PCI-DSS sensitive — must NEVER reach native code
    private String customerName;    // PII / GDPR sensitive
    private String accountType;     // irrelevant to risk calculation
    private double balance;         // needed
    private double creditLimit;     // needed
    private double interestRate;    // irrelevant to risk calculation
    private String accountStatus;   // irrelevant to risk calculation

    public FinancialAccount(String accountNumber, String customerName,
                            String accountType,   double balance,
                            double creditLimit,   double interestRate,
                            String accountStatus) {
        this.accountNumber  = accountNumber;
        this.customerName   = customerName;
        this.accountType    = accountType;
        this.balance        = balance;
        this.creditLimit    = creditLimit;
        this.interestRate   = interestRate;
        this.accountStatus  = accountStatus;
    }

    // -----------------------------------------------------------------------
    // APPROACH A (Less Preferred):
    // Native code receives the full object reference. It must self-restrict
    // to only reading balance and creditLimit — but that is enforced only
    // by convention, not by the type system. NOT recommended for financial data.
    // -----------------------------------------------------------------------
    public native double calculateRiskMetric();

    // -----------------------------------------------------------------------
    // APPROACH B (RECOMMENDED — Principle of Least Privilege):
    // Java extracts only the two required values before the JNI boundary.
    // accountNumber, customerName, interestRate, accountStatus are
    // structurally unreachable from the native side.
    // -----------------------------------------------------------------------
    public double calculateRiskMetricSecure() {
        // The sensitive field extraction and boundary-crossing happen
        // here in trusted Java code — native code never sees 'this'
        return nativeCalculateRiskMetric(this.balance, this.creditLimit);
    }

    // Private: callers use calculateRiskMetricSecure(), not this directly.
    // Declared private so no other Java code can pass arbitrary values.
    private native double nativeCalculateRiskMetric(double balance, double creditLimit);

    static {
        System.loadLibrary("financial_risk");
    }

    // -----------------------------------------------------------------------
    // Standard accessors — omitted for brevity in the JNI context
    // -----------------------------------------------------------------------
}