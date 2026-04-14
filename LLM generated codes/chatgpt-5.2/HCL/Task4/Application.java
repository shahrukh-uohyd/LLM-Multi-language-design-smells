public class Application {

    public static void main(String[] args) {
        Solver solver = new Solver();

        int input = 42;
        int result = solver.solve(input);

        System.out.println("Solver result: " + result);
    }
}
