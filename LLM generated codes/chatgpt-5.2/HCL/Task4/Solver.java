public final class Solver {

    static {
        // Ensure native library is loaded before any native call
        NativeSolverLibraryLoader.load();
    }

    /**
     * Solves a problem using the native solver.
     *
     * @param input input value
     * @return computed solution
     */
    public int solve(int input) {
        return nativeSolve(input);
    }

    // JNI method implemented in native code
    private native int nativeSolve(int input);
}
