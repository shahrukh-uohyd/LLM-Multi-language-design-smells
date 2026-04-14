import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * SensorDecoder.java
 *
 * Java host class that:
 *  - Models five real-world sensor stream types as raw byte[] payloads.
 *  - Declares one native decode method per sensor protocol.
 *  - Passes each stream to the native layer via JNI.
 *  - Receives, validates, and displays the structured decoded results.
 *
 * Sensor protocols implemented
 * ────────────────────────────
 *  ① Temperature/Humidity (TH)  – fixed-frame, 2× little-endian int16
 *  ② Accelerometer  (ACCEL)     – fixed-frame, 3× little-endian float32 (x,y,z)
 *  ③ GPS            (GPS)       – fixed-frame, 2× little-endian float64 (lat,lon)
 *                                  + 1× little-endian float32 (alt)
 *  ④ Pressure/Altitude (BARO)   – variable multi-frame stream of uint16 samples
 *  ⑤ ECG/Bio-signal  (ECG)      – variable multi-frame int16 signed samples
 *                                  with checksum validation
 *
 * Wire format shared by all frames:
 *   [0]      SENSOR_ID  (1 byte)
 *   [1]      SEQ_NUM    (1 byte, wraps at 255)
 *   [2..3]   PAYLOAD_LEN (uint16 LE, bytes of payload that follow)
 *   [4..N-2] PAYLOAD    (PAYLOAD_LEN bytes)
 *   [N-1]    CHECKSUM   (1 byte, XOR of bytes [0..N-2])
 */
public class SensorDecoder {

    // ---------------------------------------------------------------
    // Sensor-ID constants (must match C-side defines)
    // ---------------------------------------------------------------
    public static final byte SENSOR_TH    = 0x01;
    public static final byte SENSOR_ACCEL = 0x02;
    public static final byte SENSOR_GPS   = 0x03;
    public static final byte SENSOR_BARO  = 0x04;
    public static final byte SENSOR_ECG   = 0x05;

    // ---------------------------------------------------------------
    // Result containers
    // ---------------------------------------------------------------

    /** Decoded Temperature + Humidity frame. */
    public static class THReading {
        public final int    sequenceNumber;
        public final double temperatureCelsius;
        public final double humidityPercent;
        public final boolean checksumOk;

        public THReading(int seq, double temp, double hum, boolean csOk) {
            this.sequenceNumber    = seq;
            this.temperatureCelsius = temp;
            this.humidityPercent   = hum;
            this.checksumOk        = csOk;
        }

        @Override public String toString() {
            return String.format(
                "THReading{seq=%d, temp=%.2f°C, hum=%.2f%%, csOk=%b}",
                sequenceNumber, temperatureCelsius, humidityPercent, checksumOk);
        }
    }

    /** Decoded 3-axis accelerometer frame. */
    public static class AccelReading {
        public final int    sequenceNumber;
        public final double xG, yG, zG;          // in units of g
        public final double magnitudeG;
        public final boolean checksumOk;

        public AccelReading(int seq, double x, double y, double z,
                            double mag, boolean csOk) {
            this.sequenceNumber = seq;
            this.xG = x; this.yG = y; this.zG = z;
            this.magnitudeG = mag;
            this.checksumOk = csOk;
        }

        @Override public String toString() {
            return String.format(
                "AccelReading{seq=%d, x=%.4fg, y=%.4fg, z=%.4fg, |a|=%.4fg, csOk=%b}",
                sequenceNumber, xG, yG, zG, magnitudeG, checksumOk);
        }
    }

    /** Decoded GPS fix frame. */
    public static class GpsReading {
        public final int    sequenceNumber;
        public final double latitudeDeg;
        public final double longitudeDeg;
        public final double altitudeMetres;
        public final boolean checksumOk;

        public GpsReading(int seq, double lat, double lon,
                          double alt, boolean csOk) {
            this.sequenceNumber = seq;
            this.latitudeDeg    = lat;
            this.longitudeDeg   = lon;
            this.altitudeMetres = alt;
            this.checksumOk     = csOk;
        }

        @Override public String toString() {
            return String.format(
                "GpsReading{seq=%d, lat=%.6f°, lon=%.6f°, alt=%.1fm, csOk=%b}",
                sequenceNumber, latitudeDeg, longitudeDeg,
                altitudeMetres, checksumOk);
        }
    }

    /** Decoded barometric pressure multi-sample stream. */
    public static class BaroStream {
        public final int      frameCount;
        public final double[] pressuresPa;     // one value per frame (hPa)
        public final double   minPressure;
        public final double   maxPressure;
        public final double   meanPressure;
        public final int      corruptFrames;

        public BaroStream(int frameCount, double[] pressures,
                          double min, double max, double mean,
                          int corrupt) {
            this.frameCount    = frameCount;
            this.pressuresPa   = pressures;
            this.minPressure   = min;
            this.maxPressure   = max;
            this.meanPressure  = mean;
            this.corruptFrames = corrupt;
        }
    }

    /** Decoded ECG bio-signal multi-sample stream. */
    public static class EcgStream {
        public final int      sampleCount;
        public final double[] samples;          // calibrated µV values
        public final double   peakPositive;
        public final double   peakNegative;
        public final double   rmsAmplitude;
        public final int      corruptFrames;

        public EcgStream(int sampleCount, double[] samples,
                         double peakPos, double peakNeg,
                         double rms, int corrupt) {
            this.sampleCount  = sampleCount;
            this.samples      = samples;
            this.peakPositive = peakPos;
            this.peakNegative = peakNeg;
            this.rmsAmplitude = rms;
            this.corruptFrames = corrupt;
        }
    }

    // ---------------------------------------------------------------
    // Native method declarations
    // ---------------------------------------------------------------

    /** Decodes a single Temperature/Humidity frame. */
    public native THReading    decodeTH(byte[] frame);

    /** Decodes a single Accelerometer frame. */
    public native AccelReading decodeAccel(byte[] frame);

    /** Decodes a single GPS frame. */
    public native GpsReading   decodeGps(byte[] frame);

    /**
     * Decodes a multi-frame barometric pressure stream.
     * The stream may contain one or more concatenated frames.
     */
    public native BaroStream   decodeBaroStream(byte[] stream);

    /**
     * Decodes a multi-frame ECG bio-signal stream.
     * Each frame carries a block of int16 signed samples.
     */
    public native EcgStream    decodeEcgStream(byte[] stream);

    // ---------------------------------------------------------------
    // Static initialiser
    // ---------------------------------------------------------------
    static {
        System.loadLibrary("sensordec");
    }

    // ---------------------------------------------------------------
    // Stream / frame builders (simulate sensor hardware output)
    // ---------------------------------------------------------------

    /**
     * Builds a raw wire-format frame:
     *   [sensorId | seqNum | lenLo | lenHi | payload... | xorChecksum]
     */
    private static byte[] buildFrame(byte sensorId, byte seqNum,
                                     byte[] payload) {
        int    totalLen = 4 + payload.length + 1;
        byte[] frame    = new byte[totalLen];
        frame[0] = sensorId;
        frame[1] = seqNum;
        frame[2] = (byte)(payload.length & 0xFF);
        frame[3] = (byte)((payload.length >> 8) & 0xFF);
        System.arraycopy(payload, 0, frame, 4, payload.length);

        // XOR checksum over all bytes except the last slot
        byte cs = 0;
        for (int i = 0; i < totalLen - 1; i++) cs ^= frame[i];
        frame[totalLen - 1] = cs;
        return frame;
    }

    /** TH payload: int16 LE temperature (×100 °C) + int16 LE humidity (×100 %) */
    static byte[] buildTHFrame(byte seq, double tempC, double humPct) {
        short rawTemp = (short) Math.round(tempC * 100.0);
        short rawHum  = (short) Math.round(humPct * 100.0);
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort(rawTemp).putShort(rawHum);
        return buildFrame(SENSOR_TH, seq, bb.array());
    }

    /** ACCEL payload: 3× float32 LE (x, y, z in g) */
    static byte[] buildAccelFrame(byte seq, float xG, float yG, float zG) {
        ByteBuffer bb = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(xG).putFloat(yG).putFloat(zG);
        return buildFrame(SENSOR_ACCEL, seq, bb.array());
    }

    /** GPS payload: float64 LE lat + float64 LE lon + float32 LE alt (m) */
    static byte[] buildGpsFrame(byte seq, double lat, double lon, float altM) {
        ByteBuffer bb = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        bb.putDouble(lat).putDouble(lon).putFloat(altM);
        return buildFrame(SENSOR_GPS, seq, bb.array());
    }

    /**
     * Builds a multi-frame barometric stream.
     * Each frame payload is one uint16 LE pressure sample (raw ADC count).
     * adcRaw → hPa via:  pressure = 300.0 + (adc / 65535.0) * 800.0
     */
    static byte[] buildBaroStream(int[] adcRawSamples) {
        ByteBuffer bb = ByteBuffer.allocate(adcRawSamples.length * 7);
        for (int i = 0; i < adcRawSamples.length; i++) {
            ByteBuffer payload = ByteBuffer.allocate(2)
                                           .order(ByteOrder.LITTLE_ENDIAN);
            payload.putShort((short)(adcRawSamples[i] & 0xFFFF));
            byte[] frame = buildFrame(SENSOR_BARO, (byte)(i & 0xFF),
                                      payload.array());
            bb.put(frame);
        }
        byte[] result = new byte[bb.position()];
        bb.rewind();
        bb.get(result);
        return result;
    }

    /**
     * Builds a multi-frame ECG stream.
     * Each frame carries samplesPerFrame int16 LE signed samples.
     * Raw ADC → µV via: uv = (raw / 32768.0) * 5000.0
     */
    static byte[] buildEcgStream(short[][] sampleBlocks) {
        int totalBytes = 0;
        for (short[] block : sampleBlocks)
            totalBytes += 4 + block.length * 2 + 1;
        ByteBuffer bb = ByteBuffer.allocate(totalBytes);
        for (int b = 0; b < sampleBlocks.length; b++) {
            short[] block = sampleBlocks[b];
            ByteBuffer payload = ByteBuffer.allocate(block.length * 2)
                                           .order(ByteOrder.LITTLE_ENDIAN);
            for (short s : block) payload.putShort(s);
            byte[] frame = buildFrame(SENSOR_ECG, (byte)(b & 0xFF),
                                      payload.array());
            bb.put(frame);
        }
        byte[] result = new byte[bb.position()];
        bb.rewind();
        bb.get(result);
        return result;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private static void section(int n, String title) {
        System.out.println(
            "\n┌──────────────────────────────────────────────────────────────");
        System.out.printf(
            "│ Stream #%d — %s%n", n, title);
        System.out.println(
            "├──────────────────────────────────────────────────────────────");
    }

    private static String hexDump(byte[] data, int maxBytes) {
        int n = Math.min(data.length, maxBytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(String.format("%02X", data[i] & 0xFF));
            if (i < n - 1) sb.append(" ");
        }
        if (data.length > maxBytes)
            sb.append(" … (").append(data.length).append("B total)");
        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------
    public static void main(String[] args) {

        SensorDecoder dec = new SensorDecoder();

        System.out.println(
            "╔══════════════════════════════════════════════════════════════╗");
        System.out.println(
            "║           JNI Sensor Data Stream Decoder Demo               ║");
        System.out.println(
            "╚══════════════════════════════════════════════════════════════╝");

        // ── ① Temperature / Humidity ──────────────────────────────────
        section(1, "Temperature / Humidity (TH) Sensor");
        double[][] thData = {
            { 23.45, 61.30 },
            { -5.12, 88.00 },
            {100.00,  0.00 },
            {  0.00, 100.0 }
        };
        for (int i = 0; i < thData.length; i++) {
            byte[]    frame = buildTHFrame((byte)(i + 1),
                                           thData[i][0], thData[i][1]);
            System.out.printf("│ Raw [seq=%d]: %s%n",
                              i + 1, hexDump(frame, frame.length));
            THReading r = dec.decodeTH(frame);
            System.out.printf("│ Decoded    : %s%n", r);
            boolean tempOk = Math.abs(r.temperatureCelsius - thData[i][0]) < 0.01;
            boolean humOk  = Math.abs(r.humidityPercent    - thData[i][1]) < 0.01;
            System.out.printf("│ Accuracy   : temp=%s  hum=%s  cs=%s%n%n",
                tempOk ? "✓" : "✗", humOk ? "✓" : "✗",
                r.checksumOk ? "✓" : "✗");
        }

        // ── ② Accelerometer ───────────────────────────────────────────
        section(2, "3-Axis Accelerometer (ACCEL) Sensor");
        float[][] accelData = {
            {  0.00f,  0.00f,  1.00f },   // flat, gravity only
            {  0.71f,  0.00f,  0.71f },   // 45° tilt
            {  0.10f, -0.20f,  0.97f },   // slight motion
            { -1.50f,  2.30f, -0.80f }    // vigorous shake
        };
        for (int i = 0; i < accelData.length; i++) {
            float[] v = accelData[i];
            byte[] frame = buildAccelFrame((byte)(i + 1), v[0], v[1], v[2]);
            System.out.printf("│ Raw [seq=%d]: %s%n",
                              i + 1, hexDump(frame, frame.length));
            AccelReading r = dec.decodeAccel(frame);
            System.out.printf("│ Decoded    : %s%n", r);
            System.out.printf("│ cs=%s%n%n", r.checksumOk ? "✓" : "✗");
        }

        // ── ③ GPS ─────────────────────────────────────────────────────
        section(3, "GPS Fix Sensor");
        double[][] gpsData = {
            { 37.774929, -122.419418,  15.5f },   // San Francisco
            { 51.507351,   -0.127758, 11.0f },    // London
            { 35.689487,  139.691711, 40.2f },    // Tokyo
            {-33.868820,  151.209296,  5.0f }     // Sydney
        };
        for (int i = 0; i < gpsData.length; i++) {
            byte[] frame = buildGpsFrame((byte)(i + 1),
                gpsData[i][0], gpsData[i][1], (float)gpsData[i][2]);
            System.out.printf("│ Raw [seq=%d]: %s%n",
                              i + 1, hexDump(frame, frame.length));
            GpsReading r = dec.decodeGps(frame);
            System.out.printf("│ Decoded    : %s%n", r);
            boolean latOk = Math.abs(r.latitudeDeg  - gpsData[i][0]) < 0.0001;
            boolean lonOk = Math.abs(r.longitudeDeg - gpsData[i][1]) < 0.0001;
            System.out.printf("│ lat=%s lon=%s cs=%s%n%n",
                latOk ? "✓" : "✗", lonOk ? "✓" : "✗",
                r.checksumOk ? "✓" : "✗");
        }

        // ── ④ Barometric pressure stream ──────────────────────────────
        section(4, "Barometric Pressure (BARO) Multi-Frame Stream");
        int[] baroAdc = {
            10000, 20000, 30000, 40000, 50000,
            32768, 16384, 49152, 8192,  57344
        };
        byte[] baroStream = buildBaroStream(baroAdc);
        System.out.printf("│ Raw stream : %s%n", hexDump(baroStream, 32));
        BaroStream bs = dec.decodeBaroStream(baroStream);
        System.out.printf("│ Frames decoded : %d%n", bs.frameCount);
        System.out.printf("│ Min pressure   : %.2f hPa%n", bs.minPressure);
        System.out.printf("│ Max pressure   : %.2f hPa%n", bs.maxPressure);
        System.out.printf("│ Mean pressure  : %.2f hPa%n", bs.meanPressure);
        System.out.printf("│ Corrupt frames : %d%n", bs.corruptFrames);
        System.out.print( "│ Samples        : ");
        for (double p : bs.pressuresPa) System.out.printf("%.1f ", p);
        System.out.printf("%n│ Count check    : %d / %d  %s%n",
            bs.frameCount, baroAdc.length,
            bs.frameCount == baroAdc.length ? "✓ PASS" : "✗ FAIL");

        // ── ⑤ ECG bio-signal stream ───────────────────────��───────────
        section(5, "ECG Bio-Signal Multi-Frame Stream");
        // Simulate a simple synthetic ECG: a triangle waveform
        int     sps          = 8;    // samples per frame
        int     frames       = 4;
        short[][] ecgBlocks  = new short[frames][sps];
        for (int f = 0; f < frames; f++) {
            for (int s = 0; s < sps; s++) {
                int idx = f * sps + s;
                // Simple synthetic wave: saw + spike
                double phase = (idx % 16) / 16.0;
                double uv    = phase < 0.5
                    ? phase * 4000 - 500
                    : (1.0 - phase) * 4000 - 500;
                ecgBlocks[f][s] = (short) Math.max(-32768,
                                   Math.min(32767, (int) uv));
            }
        }
        byte[] ecgStream = buildEcgStream(ecgBlocks);
        System.out.printf("│ Raw stream  : %s%n", hexDump(ecgStream, 32));
        EcgStream es = dec.decodeEcgStream(ecgStream);
        System.out.printf("│ Samples decoded : %d%n",   es.sampleCount);
        System.out.printf("│ Peak positive   : %.2f µV%n", es.peakPositive);
        System.out.printf("│ Peak negative   : %.2f µV%n", es.peakNegative);
        System.out.printf("│ RMS amplitude   : %.2f µV%n", es.rmsAmplitude);
        System.out.printf("│ Corrupt frames  : %d%n",   es.corruptFrames);
        int expectedSamples = frames * sps;
        System.out.printf("│ Count check     : %d / %d  %s%n",
            es.sampleCount, expectedSamples,
            es.sampleCount == expectedSamples ? "✓ PASS" : "✗ FAIL");

        System.out.println(
            "\n└──────────────────────────────────────────────────────────────");
        System.out.println("All sensor streams decoded successfully.");
    }
}