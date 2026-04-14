public class PayloadProcessor {

    // Load the native shared library
    static {
        System.loadLibrary("payloadprocessor");
    }

    /**
     * Native method that receives a binary payload, transforms it, 
     * and returns the processed payload.
     * 
     * @param payload The raw binary data (byte array).
     * @return The processed binary data.
     */
    public native byte[] processPayload(byte[] payload);

    public static void main(String[] args) {
        PayloadProcessor processor = new PayloadProcessor();

        // 1. Produce a binary payload (e.g., raw bytes from a file, network, etc.)
        byte[] originalPayload = { 
            0x12, 0x34, 0x56, 0x78, 
            (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0 
        };

        System.out.println("--- JNI Binary Payload Processing ---");
        System.out.println("Original Payload  : " + bytesToHex(originalPayload));

        // 2. Pass the binary payload to the native method
        byte[] processedPayload = processor.processPayload(originalPayload);

        // 3. The result of the processing is returned to Java
        System.out.println("Processed Payload : " + bytesToHex(processedPayload));

        // Optional: Pass the processed payload back through to revert it
        // (Since bitwise NOT applied twice returns the original bytes)
        byte[] restoredPayload = processor.processPayload(processedPayload);
        System.out.println("Restored Payload  : " + bytesToHex(restoredPayload));
    }

    /**
     * Helper method to convert a byte array to a readable Hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == NULL || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}