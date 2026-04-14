public class NativeBridge {

    static {
        System.loadLibrary("nativebridge");
    }

    /**
     * Native method:
     *  1. Reads the field  "balance"      from the Account object.
     *  2. Reads the field  "interestRate" from the Account object.
     *  3. Computes interest = balance * interestRate  (in C++).
     *  4. Calls account.applyInterest(interest)       (from C++).
     *
     * @param account the Account object to operate on
     * @return the computed interest amount (so Java can verify it)
     */
    public native double computeAndApplyInterest(Account account);
}