import java.util.Arrays;

/**
 * SecurityModule
 *
 * Provides payload encryption, certificate verification, and checksum
 * calculation by delegating to the native layer through IIoTController.
 */
public class SecurityModule {

    private final IIoTController controller;

    /**
     * @param controller  Shared IIoTController instance that owns the
     *                    loaded native library.
     */
    public SecurityModule(IIoTController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("IIoTController must not be null.");
        }
        this.controller = controller;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Encrypts a payload and returns the ciphertext.
     *
     * @param plaintext  Data to encrypt.
     * @param keyAlias   Key alias in the native key-store.
     * @return           Encrypted bytes, or null on failure.
     */
    public byte[] securePayload(byte[] plaintext, String keyAlias) {
        if (plaintext == null || plaintext.length == 0) {
            throw new IllegalArgumentException("Plaintext must not be null or empty.");
        }
        System.out.println("[Security] Encrypting " + plaintext.length
                + " bytes with key '" + keyAlias + "'...");

        byte[] ciphertext = controller.encryptTransmission(plaintext, keyAlias);

        if (ciphertext == null) {
            System.err.println("[Security] encryptTransmission() returned null.");
        } else {
            System.out.println("[Security] Encryption successful. Ciphertext length: "
                    + ciphertext.length);
        }
        return ciphertext;
    }

    /**
     * Validates a DER-encoded X.509 certificate against the device trust-store.
     *
     * @param certDerBytes  DER-encoded certificate.
     * @return              true if the certificate is trusted.
     */
    public boolean validateCertificate(byte[] certDerBytes) {
        if (certDerBytes == null || certDerBytes.length == 0) {
            System.err.println("[Security] Certificate bytes are null or empty.");
            return false;
        }
        System.out.println("[Security] Verifying certificate ("
                + certDerBytes.length + " bytes)...");

        boolean valid = controller.verifyCertificate(certDerBytes);
        System.out.println("[Security] Certificate verification result: "
                + (valid ? "TRUSTED" : "UNTRUSTED / REJECTED"));
        return valid;
    }

    /**
     * Computes and returns the checksum of a payload.
     *
     * @param payload  Raw bytes to checksum.
     * @return         Hex-encoded checksum string.
     */
    public String computeChecksum(byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Payload must not be null or empty.");
        }
        System.out.println("[Security] Calculating checksum for "
                + payload.length + " bytes...");

        String checksum = controller.calculatePayloadChecksum(payload);
        System.out.println("[Security] Checksum: " + checksum);
        return checksum;
    }

    /**
     * Convenience method: verify certificate → compute checksum → encrypt.
     * Returns null if the certificate is not trusted.
     *
     * @param certDerBytes  Certificate to verify first.
     * @param plaintext     Payload to encrypt.
     * @param keyAlias      Key alias.
     * @return              Encrypted bytes, or null if cert is invalid.
     */
    public byte[] verifyThenEncrypt(byte[] certDerBytes,
                                    byte[] plaintext,
                                    String keyAlias) {
        if (!validateCertificate(certDerBytes)) {
            System.err.println("[Security] Aborting encryption — certificate not trusted.");
            return null;
        }
        String checksum = computeChecksum(plaintext);
        System.out.println("[Security] Pre-encryption checksum: " + checksum);
        return securePayload(plaintext, keyAlias);
    }
}