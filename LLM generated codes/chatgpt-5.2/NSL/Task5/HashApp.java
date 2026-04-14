import java.util.Arrays;

public class HashApp {

    public static void main(String[] args) {

        byte[] input = "JNI cryptographic hashing".getBytes();

        byte[] hash = NativeHash.sha256(input);

        System.out.println("Input:  " + new String(input));
        System.out.println("SHA-256: " + toHex(hash));
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
