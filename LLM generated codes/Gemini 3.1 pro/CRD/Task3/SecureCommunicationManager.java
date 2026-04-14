// The class responsible for handling secure data transmission
public class SecureCommunicationManager {
    private final CryptoEngine cryptoEngine;

    public SecureCommunicationManager() {
        // Initialize the native crypto dependency
        this.cryptoEngine = new CryptoEngine();
    }

    public void transmitSecureMessage(byte[] message, byte[] sessionKey) {
        System.out.println("Preparing message for secure transmission...");
        
        // Delegate encryption to native C++ implementation
        byte[] encryptedPayload = cryptoEngine.encryptPayload(message, sessionKey);
        
        System.out.println("Payload encrypted. Size: " + encryptedPayload.length + " bytes.");
        // TODO: Logic to send 'encryptedPayload' over the network socket
        System.out.println("Message transmitted successfully.");
    }

    public byte[] receiveSecureMessage(byte[] incomingEncryptedData, byte[] sessionKey) {
        System.out.println("Encrypted data received. Decrypting payload...");
        
        // Delegate decryption to native C++ implementation
        byte[] decryptedMessage = cryptoEngine.decryptPayload(incomingEncryptedData, sessionKey);
        
        System.out.println("Payload decrypted successfully.");
        return decryptedMessage;
    }
}