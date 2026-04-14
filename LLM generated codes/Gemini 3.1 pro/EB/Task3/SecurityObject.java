public class SecurityObject {
    // The internal key (JNI Signature: "[B")
    private byte[] encryptionKey;

    public SecurityObject(byte[] key) {
        this.encryptionKey = key;
    }
}