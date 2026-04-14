import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * BinaryProcessor.java
 *
 * Java host class that:
 *  - Declares native binary-processing methods.
 *  - Produces a representative set of binary payloads.
 *  - Passes each payload to the native layer via JNI.
 *  - Displays the results returned from the native side.
 *
 * Native operations performed per payload:
 *   ① Inspection  — compute XOR checksum + count set bits (popcount).
 *   ② Transform   — invert every byte (bitwise NOT), then byte-reverse.
 *   ③ Encode      — lightweight XOR cipher with a rolling key.
 */
public class BinaryProcessor {

    // ---------------------------------------------------------------
    // Native method declarations
    // ---------------------------------------------------------------

    /**
     * Inspects a binary payload and returns a summary record.
     *
     * @param payload raw byte array to inspect (may be empty, never null)
     * @return long[3] = { xorChecksum, totalSetBits, payloadLength }
     */
    public native long[] inspectPayload(byte[] payload);

    /**
     * Transforms a binary payload and returns the transformed bytes.
     *
     * Pipeline: invert each byte (NOT) → reverse byte order.
     *
     * @param payload raw byte array to transform
     * @return new byte array with the transformation applied
     */
    public native byte[] transformPayload(byte[] payload);

    /**
     * Encodes a binary payload with a rolling-XOR cipher.
     *
     * Each byte b[i] is XORed with ((seed + i) & 0xFF).
     * The same call with the same seed decodes (XOR is self-inverse).
     *
     * @param payload raw byte array to encode/decode
     * @param seed    starting key byte (0–255)
     * @return encoded (or decoded) byte array
     */
    public native byte[] xorCipherPayload(byte[] payload, int seed);

    // ---------------------------------------------------------------
    // Static initialiser — load the shared library
    // ---------------------------------------------------------------
    static {
        System.loadLibrary("binproc");
    }

    // ---------------------------------------------------------------
    // Payload factory methods
    // ---------------------------------------------------------------

    /** Payload 1: sequential byte ramp 0x00 … 0xFF */
    private static byte[] buildRampPayload() {
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }
        return data;
    }

    /** Payload 2: little-endian 32-bit integers packed into bytes */
    private static byte[] buildIntegerPayload() {
        int[] values = {0xDEADBEEF, 0xCAFEBABE, 0x0BADC0DE, 0xFEEDFACE};
        ByteBuffer buf = ByteBuffer.allocate(values.length * 4)
                                   .order(ByteOrder.LITTLE_ENDIAN);
        for (int v : values) buf.putInt(v);
        return buf.array();
    }

    /** Payload 3: ASCII text treated as raw bytes */
    private static byte[] buildTextPayload() {
        return "Hello, JNI Binary World!\n".getBytes();
    }

    /** Payload 4: sparse payload — mostly zeros with a few set bits */
    private static byte[] buildSparsePayload() {
        byte[] data = new byte[32];
        data[0]  = 0x01;
        data[7]  = (byte) 0x80;
        data[15] = 0x0F;
        data[31] = (byte) 0xFF;
        return data;
    }

    /** Payload 5: empty payload (boundary case) */
    private static byte[] buildEmptyPayload() {
        return new byte[0];
    }

    /** Payload 6: single-byte payload */
    private static byte[] buildSingleBytePayload() {
        return new byte[]{ (byte) 0xA5 };
    }

    // ---------------------------------------------------------------
    // Formatting helpers
    // ---------------------------------------------------------------

    private static String toHex(byte[] data) {
        if (data.length == 0) return "(empty)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02X", data[i] & 0xFF));
            if ((i + 1) % 16 == 0 && i + 1 < data.length)
                sb.append("\n              ");  // align continuation lines
            else if (i + 1 < data.length)
                sb.append(" ");
        }
        return sb.toString();
    }

    private static String toAsciiSafe(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            char c = (char)(b & 0xFF);
            sb.append((c >= 0x20 && c < 0x7F) ? c : '.');
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------
    public static void main(String[] args) {

        BinaryProcessor proc = new BinaryProcessor();

        // Payloads and their descriptive names
        String[] names = {
            "Sequential Ramp (256 B)",
            "Packed 32-bit integers",
            "ASCII text bytes",
            "Sparse bit pattern",
            "Empty (boundary)",
            "Single byte (0xA5)"
        };
        byte[][] payloads = {
            buildRampPayload(),
            buildIntegerPayload(),
            buildTextPayload(),
            buildSparsePayload(),
            buildEmptyPayload(),
            buildSingleBytePayload()
        };

        final int CIPHER_SEED = 0x42;

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           JNI Binary Payload Processor Demo              ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");

        for (int idx = 0; idx < payloads.length; idx++) {
            byte[] payload = payloads[idx];

            System.out.println("\n┌─────────────────────────────────────────────────────────");
            System.out.printf ("│ Payload #%d — %s%n", idx + 1, names[idx]);
            System.out.println("├─────────────────────────────────────────────────────────");

            // --- Show first 32 raw bytes (or fewer) ---
            byte[] preview = Arrays.copyOf(payload, Math.min(payload.length, 32));
            System.out.printf("│ Raw (hex, first 32B) : %s%n", toHex(preview));
            if (payload.length > 0 && payload == payloads[2]) // ASCII payload
                System.out.printf("│ ASCII interpretation : %s%n", toAsciiSafe(payload));

            // ── ① Inspect ──────────────────────────────────────────
            long[] stats = proc.inspectPayload(payload);
            System.out.printf("│%n");
            System.out.printf("│ [INSPECT]%n");
            System.out.printf("│   Length       : %d byte(s)%n",  stats[2]);
            System.out.printf("│   XOR checksum : 0x%02X%n",       stats[0]);
            System.out.printf("│   Set bits     : %d%n",           stats[1]);

            // ── ② Transform ────────────────────────────────────────
            byte[] transformed = proc.transformPayload(payload);
            byte[] tPreview    = Arrays.copyOf(transformed,
                                               Math.min(transformed.length, 32));
            System.out.printf("│%n");
            System.out.printf("│ [TRANSFORM]  invert-bytes → reverse-order%n");
            System.out.printf("│   Result (hex, first 32B) : %s%n", toHex(tPreview));

            // Verify round-trip: transform(transform(x)) == x
            byte[] roundTrip     = proc.transformPayload(transformed);
            boolean rtOk         = Arrays.equals(roundTrip, payload);
            System.out.printf("│   Round-trip check        : %s%n",
                              rtOk ? "✓ PASS" : "✗ FAIL");

            // ── ③ XOR cipher ───────────────────────────────────────
            byte[] encoded = proc.xorCipherPayload(payload, CIPHER_SEED);
            byte[] decoded = proc.xorCipherPayload(encoded, CIPHER_SEED);
            byte[] ePreview = Arrays.copyOf(encoded,
                                            Math.min(encoded.length, 32));
            System.out.printf("│%n");
            System.out.printf("│ [XOR CIPHER]  seed=0x%02X%n", CIPHER_SEED);
            System.out.printf("│   Encoded (hex, first 32B): %s%n", toHex(ePreview));
            boolean cipherOk = Arrays.equals(decoded, payload);
            System.out.printf("│   Decode round-trip       : %s%n",
                              cipherOk ? "✓ PASS" : "✗ FAIL");
        }

        System.out.println("\n└─────────────────────────────────────────────────────────");
        System.out.println("All payloads processed successfully.");
    }
}