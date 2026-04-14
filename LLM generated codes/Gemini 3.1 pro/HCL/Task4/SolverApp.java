public class SolverApp {

    public static void main(String[] args) {
        System.out.println("--- Starting Native Solver Application ---");
        
        NativeSolver solver = null;
        
        try {
            // The instantiation triggers the static block (if not already loaded) 
            // and calls the native initSolver() method.
            solver = new NativeSolver();
            
            // Example data: A system of 3 linear equations
            double[][] matrix = {
                {2.0, 1.0, -1.0},
                {-3.0, -1.0, 2.0},
                {-2.0, 1.0, 2.0}
            };
            double[] constants = {8.0, -11.0, -3.0};
            
            System.out.println("Sending data to the native solver...");
            
            // Invoke the Java wrapper which calls the native executeSolve()
            double[] results = solver.solve(matrix, constants);
            
            System.out.println("--- Solver Results ---");
            for (int i = 0; i < results.length; i++) {
                System.out.printf("x[%d] = %.4f%n", i, results[i]);
            }
            
        } catch (Exception e) {
            System.err.println("Application encountered an error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure native resources are cleaned up safely
            if (solver != null) {
                solver.close();
                System.out.println("Native solver resources released.");
            }
        }
    }
}