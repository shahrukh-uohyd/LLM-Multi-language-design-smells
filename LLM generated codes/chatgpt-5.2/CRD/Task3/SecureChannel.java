import java.util.Arrays;

public class SecureChannel {

    private final NativeDiagnostics diagnostics = new NativeDiagnostics();
    private static final byte KEY = 0x5A;

    public byte[] send(byte[] plaintext) {
        return diagnostics.encrypt(plaintext, KEY);
    }

    public byte[] receive(byte[] ciphertext) {
        return diagnostics.decrypt(ciphertext, KEY);
    }

    public static void main(String[] args) {

        SecureChannel channel = new SecureChannel();

        // Existing diagnostics functionality
        channel.diagnostics.logStartup();

        // New native-backed encryption/decryption used ONLY here
        byte[] message = "Secure JNI channel".getBytes();
        byte[] encrypted = channel.send(message);
        byte[] decrypted = channel.receive(encrypted);

        System.out.println("Encrypted: " + Arrays.toString(encrypted));
        System.out.println("Decrypted: " + new String(decrypted));
    }
}
