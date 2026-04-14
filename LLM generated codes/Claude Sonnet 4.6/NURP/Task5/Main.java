package com.serializer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * End-to-end demonstration of every JNI serialization / deserialization
 * operation exposed by {@link NativeSerializer}.
 *
 * <p>The demo is fully self-contained — no files are read or written,
 * making it runnable immediately after building.
 */
public final class Main {

    public static void main(String[] args) {

        banner("JNI Native Serializer — Demonstration");

        try (NativeSerializer ser = new NativeSerializer()) {

            // ── 1. Single-record round-trip ───────────────────────────────────
            section("1. Single-Record Round-Trip");
            singleRecordDemo(ser);

            // ── 2. Batch round-trip ───────────────────────────────────────────
            section("2. Batch Serialization / Deserialization");
            batchDemo(ser);

            // ── 3. Checksum validation ────────────────────────────────────────
            section("3. Checksum Validation");
            checksumDemo(ser);

            // ── 4. Payload compression / decompression ────────────────────────
            section("4. RLE Payload Compression");
            compressionDemo(ser);

            // ── 5. Edge cases ─────────────────────────────────────────────────
            section("5. Edge Cases");
            edgeCaseDemo(ser);
        }

        System.out.println("\n[Main] All operations completed successfully.");
    }

    // =========================================================================
    // Demo sections
    // =========================================================================

    // ── 1 ──────────────────���─────────────────────────────────────────────────

    private static void singleRecordDemo(NativeSerializer ser) {

        SerialRecord original = SerialRecord.builder()
                .id(1001L)
                .name("UserProfile")
                .score(98.765)
                .active(true)
                .tags("admin", "verified", "premium")
                .payload("Hello, JNI!".getBytes(StandardCharsets.UTF_8))
                .build();

        System.out.println("  Original : " + original);

        // Serialize
        SerializeResult sr = ser.serialize(original);
        System.out.printf("  Encoded  : %d bytes  (%,d ns)%n",
                          sr.getByteLength(), sr.getElapsedNanos());
        System.out.println("  HexDump  : " + sr.hexPreview(32));

        // Deserialize
        DeserializeResult dr = ser.deserialize(sr.getBytes());
        System.out.println("  Decoded  : " + dr.getRecord());
        System.out.printf("  Consumed : %d bytes  (%,d ns)%n",
                          dr.getBytesConsumed(), dr.getElapsedNanos());

        // Verify round-trip fidelity
        assertRoundTrip(original, dr.getRecord());
    }

    // ── 2 ────────────────────────────────────────────────────────────────────

    private static void batchDemo(NativeSerializer ser) {

        SerialRecord[] originals = {
            SerialRecord.builder().id(1L).name("Alpha").score(1.1)
                .active(true) .tags("a","b")
                .payload(new byte[]{0x01,0x02,0x03}).build(),
            SerialRecord.builder().id(2L).name("Beta") .score(2.2)
                .active(false).tags("c")
                .payload(new byte[]{0x0A,0x0B}).build(),
            SerialRecord.builder().id(3L).name("Gamma").score(3.3)
                .active(true) .tags("d","e","f")
                .payload("payload3".getBytes(StandardCharsets.UTF_8)).build(),
            SerialRecord.builder().id(4L).name("Delta").score(4.4)
                .active(false).tags()
                .payload(new byte[0]).build()
        };

        System.out.println("  Encoding " + originals.length + " records...");

        SerializeResult bsr = ser.serializeBatch(originals);
        System.out.printf("  Batch size : %d bytes  (%,d ns)%n",
                          bsr.getByteLength(), bsr.getElapsedNanos());

        SerialRecord[] decoded = ser.deserializeBatch(bsr.getBytes());
        System.out.println("  Decoded    : " + decoded.length + " records");

        for (int i = 0; i < originals.length; i++) {
            assertRoundTrip(originals[i], decoded[i]);
            System.out.println("    [" + i + "] PASS ✓  " + decoded[i].getName());
        }
    }

    // ── 3 ────────────────────────────────────────────────────────────────────

    private static void checksumDemo(NativeSerializer ser) {

        SerialRecord record = SerialRecord.builder()
                .id(42L).name("ChecksumTest").score(3.14).active(true)
                .tags("x").payload(new byte[]{1, 2, 3, 4, 5}).build();

        byte[] bytes = ser.serialize(record).getBytes();
        boolean valid = ser.validateChecksum(bytes);
        System.out.println("  Valid checksum     : " + (valid ? "PASS ✓" : "FAIL ✗"));

        // Corrupt one byte in the middle of the buffer and re-validate
        byte[] corrupted = bytes.clone();
        corrupted[bytes.length / 2] ^= 0xFF;
        boolean invalid = ser.validateChecksum(corrupted);
        System.out.println("  Corrupted checksum : " + (!invalid ? "PASS ✓ (correctly rejected)" : "FAIL ✗"));
    }

    // ── 4 ───────────────────────��────────────────────────────────────────────

    private static void compressionDemo(NativeSerializer ser) {

        // Highly compressible: long run of identical bytes
        byte[] repetitive = new byte[256];
        Arrays.fill(repetitive, (byte) 0xAB);
        repetitive[128] = 0x01;  // one different byte to break the single run

        byte[] compressed   = ser.compressPayload(repetitive);
        byte[] decompressed = ser.decompressPayload(compressed);

        System.out.printf("  Original     : %d bytes%n",    repetitive.length);
        System.out.printf("  Compressed   : %d bytes  (%.1f%% of original)%n",
                          compressed.length,
                          100.0 * compressed.length / repetitive.length);
        System.out.printf("  Decompressed : %d bytes%n",    decompressed.length);

        boolean match = Arrays.equals(repetitive, decompressed);
        System.out.println("  Fidelity     : " + (match ? "PASS ✓" : "FAIL ✗"));

        // Serialize a record whose payload is stored compressed
        byte[] largePayload = new byte[512];
        for (int i = 0; i < largePayload.length; i++) largePayload[i] = (byte)(i % 8);
        byte[] compressedPayload = ser.compressPayload(largePayload);

        SerialRecord rec = SerialRecord.builder()
                .id(99L).name("CompressedRecord").score(0.0).active(false)
                .tags("compressed").payload(compressedPayload).build();

        SerializeResult  sr = ser.serialize(rec);
        DeserializeResult dr = ser.deserialize(sr.getBytes());

        byte[] recovered = ser.decompressPayload(dr.getRecord().getPayload());
        System.out.println("  Payload in record: " +
                           (Arrays.equals(largePayload, recovered) ? "PASS ✓" : "FAIL ✗"));
    }

    // ── 5 ────────────────────────────────────────────────────────────────────

    private static void edgeCaseDemo(NativeSerializer ser) {

        // Empty name, no tags, no payload
        SerialRecord empty = SerialRecord.builder()
                .id(0L).name("").score(0.0).active(false)
                .tags().payload(new byte[0]).build();
        SerializeResult  es  = ser.serialize(empty);
        DeserializeResult ed  = ser.deserialize(es.getBytes());
        assertRoundTrip(empty, ed.getRecord());
        System.out.println("  Empty record            : PASS ✓");

        // Maximum id value
        SerialRecord maxId = SerialRecord.builder()
                .id(Long.MAX_VALUE).name("MaxId").score(Double.MAX_VALUE)
                .active(true).tags("max").payload(new byte[]{(byte)0xFF}).build();
        SerializeResult  ms  = ser.serialize(maxId);
        DeserializeResult md  = ser.deserialize(ms.getBytes());
        assertRoundTrip(maxId, md.getRecord());
        System.out.println("  Max id / max double     : PASS ✓");

        // Negative score
        SerialRecord neg = SerialRecord.builder()
                .id(-1L).name("Negative").score(-273.15).active(false)
                .tags("neg").payload(new byte[0]).build();
        SerializeResult  ns  = ser.serialize(neg);
        DeserializeResult nd  = ser.deserialize(ns.getBytes());
        assertRoundTrip(neg, nd.getRecord());
        System.out.println("  Negative id / score     : PASS ✓");

        // Unicode name
        SerialRecord uni = SerialRecord.builder()
                .id(7L).name("Ünïcödé-日本語-العربية").score(1.0)
                .active(true).tags("utf8","多言語")
                .payload("🔥".getBytes(StandardCharsets.UTF_8)).build();
        SerializeResult  us  = ser.serialize(uni);
        DeserializeResult ud  = ser.deserialize(us.getBytes());
        assertRoundTrip(uni, ud.getRecord());
        System.out.println("  Unicode name + tags     : PASS ✓");

        // Large binary payload (4 KB)
        byte[] bigPayload = new byte[4096];
        for (int i = 0; i < bigPayload.length; i++) bigPayload[i] = (byte)i;
        SerialRecord big = SerialRecord.builder()
                .id(8L).name("BigPayload").score(8.8).active(true)
                .tags("large").payload(bigPayload).build();
        SerializeResult  bs  = ser.serialize(big);
        DeserializeResult bd  = ser.deserialize(bs.getBytes());
        assertRoundTrip(big, bd.getRecord());
        System.out.printf("  4 KB payload round-trip : PASS ✓  (%d bytes on wire)%n",
                          bs.getByteLength());

        // Slice-based deserialization (embedded in a larger buffer)
        byte[] prefix  = new byte[]{0x00, 0x01, 0x02};
        byte[] core    = ser.serialize(empty).getBytes();
        byte[] suffix  = new byte[]{0x0A, 0x0B};
        byte[] combined = concat(prefix, core, suffix);
        DeserializeResult sliced = ser.deserialize(combined, prefix.length, core.length);
        assertRoundTrip(empty, sliced.getRecord());
        System.out.println("  Slice-based decode      : PASS ✓");
    }

    // =========================================================================
    // Assertion helpers
    // =========================================================================

    private static void assertRoundTrip(SerialRecord expected, SerialRecord actual) {
        if (!expected.equals(actual))
            throw new AssertionError(
                "Round-trip mismatch!\n  expected: " + expected +
                "\n  actual  : " + actual);
    }

    // =========================================================================
    // Display helpers
    // =========================================================================

    private static void banner(String title) {
        String line = "═".repeat(title.length() + 4);
        System.out.println("╔" + line + "╗");
        System.out.println("║  " + title + "  ║");
        System.out.println("╚" + line + "╝");
        System.out.println();
    }

    private static void section(String title) {
        System.out.printf("%n── %s %s%n", title, "─".repeat(60 - title.length()));
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }
}