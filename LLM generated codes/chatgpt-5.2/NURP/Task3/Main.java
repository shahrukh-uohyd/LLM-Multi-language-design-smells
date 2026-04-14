public class Main {

    public static void main(String[] args) {

        double power = MathNative.fastPower(2.0, 10.0);
        double magnitude = MathNative.vectorMagnitude(3.0, 4.0, 12.0);

        System.out.println("2^10 = " + power);
        System.out.println("Vector magnitude = " + magnitude);
    }
}
