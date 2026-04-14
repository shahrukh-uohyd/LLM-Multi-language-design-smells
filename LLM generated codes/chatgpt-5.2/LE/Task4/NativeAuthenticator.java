public class NativeAuthenticator {

    static {
        System.loadLibrary("nativeauth");
    }

    // ----- Existing native capabilities -----

    // 1. Extract biometric minutiae
    private native int[] nativeExtractMinutiae(byte[] rawData);

    // 2. Generate cryptographic signature
    private native byte[] nativeGenerateSignature(int[] minutiae);

    // 3. Transmit signature to secure hardware vault
    private native boolean nativeTransmitSignature(byte[] signature);

    // ----- New high-level authentication feature -----

    public boolean authenticate(byte[] rawBiometricData) {
        int[] minutiae = nativeExtractMinutiae(rawBiometricData);
        byte[] signature = nativeGenerateSignature(minutiae);
        return nativeTransmitSignature(signature);
    }

    // Example usage
    public static void main(String[] args) {
        NativeAuthenticator auth = new NativeAuthenticator();

        byte[] biometricSample = { 12, 45, 78, 23, 9 };
        boolean result = auth.authenticate(biometricSample);

        System.out.println("Authentication result: " + result);
    }
}
