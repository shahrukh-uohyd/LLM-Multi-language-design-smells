// Main solver interface
public class Solver {
    
    static {
        loadNativeLibrary();
    }
    
    // Native method declarations
    private static native double solveLinearEquation(double a, double b);
    private static native double[] solveQuadraticEquation(double a, double b, double c);
    private static native double solvePolynomial(double[] coefficients, double x);
    
    /**
     * Solves linear equation ax + b = 0
     */
    public static double solveLinear(double a, double b) {
        return solveLinearEquation(a, b);
    }
    
    /**
     * Solves quadratic equation ax^2 + bx + c = 0
     * Returns array [root1, root2] or null if no real roots
     */
    public static double[] solveQuadratic(double a, double b, double c) {
        return solveQuadraticEquation(a, b, c);
    }
    
    /**
     * Evaluates polynomial at given x value
     * Coefficients ordered from highest degree to constant term
     */
    public static double evaluatePolynomial(double[] coefficients, double x) {
        return solvePolynomial(coefficients, x);
    }
    
    private static void loadNativeLibrary() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        boolean is64bit = arch.contains("64") || arch.equals("aarch64");
        
        String libName;
        if (osName.contains("win")) {
            libName = is64bit ? "solver_win64" : "solver_win32";
        } else if (osName.contains("mac")) {
            libName = is64bit ? "libsolver_mac64" : "libsolver_mac32";
        } else { // Linux and others
            libName = is64bit ? "libsolver_linux64" : "libsolver_linux32";
        }
        
        try {
            System.loadLibrary(libName);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load solver native library: " + libName, e);
        }
    }
}