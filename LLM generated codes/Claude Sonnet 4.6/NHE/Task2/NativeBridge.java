public class NativeBridge {

    static {
        System.loadLibrary("nativebridge");
    }

    /**
     * Native method: receives a Calculator instance, an operation name,
     * and two operands. Dynamically resolves and invokes the matching
     * Calculator method at runtime from C code.
     *
     * @param calc      the Calculator object to invoke the method on
     * @param operation name of the method to call ("add","subtract","multiply","divide")
     * @param a         first operand
     * @param b         second operand
     * @return          result of the invoked method
     */
    public native int invokeOperation(Calculator calc, String operation, int a, int b);
}