public class SecurityContext {

    private byte[] key;

    public SecurityContext(byte[] key) {
        this.key = key;
    }

    // Native entry point
    public native void initNativeEncryption();
}
