public class Account {

    String owner;
    double balance;
    double interestRate;   // e.g. 0.05 means 5 %
    boolean active;

    public Account(String owner, double balance,
                   double interestRate, boolean active) {
        this.owner        = owner;
        this.balance      = balance;
        this.interestRate = interestRate;
        this.active       = active;
    }

    /**
     * Applies an interest payment to the balance.
     * Called BY native code, with the interest amount
     * calculated FROM the interestRate field also read natively.
     *
     * @param amount the interest amount to credit
     */
    public void applyInterest(double amount) {
        if (!active) {
            System.out.println("[java] Account is inactive. Interest not applied.");
            return;
        }
        System.out.printf("[java] Applying interest of %.2f to account '%s'%n",
                          amount, owner);
        balance += amount;
        System.out.printf("[java] New balance: %.2f%n", balance);
    }

    public String getSummary() {
        return String.format(
            "Account{owner='%s', balance=%.2f, interestRate=%.4f, active=%b}",
            owner, balance, interestRate, active);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}