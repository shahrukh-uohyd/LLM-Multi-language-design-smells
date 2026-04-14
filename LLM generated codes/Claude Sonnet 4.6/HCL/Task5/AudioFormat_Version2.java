package com.audio;

/**
 * Describes the encoding parameters of a PCM audio stream that is passed
 * to or returned from the native audio decoder.
 *
 * <p>All fields are immutable after construction.</p>
 */
public final class AudioFormat {

    /** Standard sample rates supported by the native decoder. */
    public enum SampleRate {
        HZ_8000(8_000), HZ_22050(22_050), HZ_44100(44_100), HZ_48000(48_000), HZ_96000(96_000);

        private final int hz;

        SampleRate(int hz) { this.hz = hz; }

        /** Returns the numeric sample rate in Hz. */
        public int getHz() { return hz; }
    }

    /** Bit-depth options supported by the native decoder. */
    public enum BitDepth {
        BITS_8(8), BITS_16(16), BITS_24(24), BITS_32(32);

        private final int bits;

        BitDepth(int bits) { this.bits = bits; }

        /** Returns the bit depth as an integer. */
        public int getBits() { return bits; }
    }

    private final SampleRate sampleRate;
    private final BitDepth   bitDepth;
    private final int        channels;   // 1 = mono, 2 = stereo, etc.
    private final boolean    bigEndian;

    private AudioFormat(Builder builder) {
        this.sampleRate = builder.sampleRate;
        this.bitDepth   = builder.bitDepth;
        this.channels   = builder.channels;
        this.bigEndian  = builder.bigEndian;
    }

    public SampleRate getSampleRate() { return sampleRate; }
    public BitDepth   getBitDepth()   { return bitDepth;   }
    public int        getChannels()   { return channels;   }
    public boolean    isBigEndian()   { return bigEndian;  }

    /** Calculates the byte rate: sampleRate × channels × (bitDepth / 8). */
    public int byteRate() {
        return sampleRate.getHz() * channels * (bitDepth.getBits() / 8);
    }

    @Override
    public String toString() {
        return "AudioFormat{"
             + "sampleRate=" + sampleRate.getHz() + " Hz"
             + ", bitDepth=" + bitDepth.getBits() + "-bit"
             + ", channels=" + channels
             + ", endian="   + (bigEndian ? "big" : "little")
             + ", byteRate=" + byteRate() + " B/s"
             + '}';
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    /** Stereo, 44.1 kHz, 16-bit, little-endian — CD quality. */
    public static AudioFormat cdQuality() {
        return builder()
                .sampleRate(SampleRate.HZ_44100)
                .bitDepth(BitDepth.BITS_16)
                .channels(2)
                .bigEndian(false)
                .build();
    }

    /** Mono, 48 kHz, 32-bit, little-endian — professional recording. */
    public static AudioFormat professional() {
        return builder()
                .sampleRate(SampleRate.HZ_48000)
                .bitDepth(BitDepth.BITS_32)
                .channels(1)
                .bigEndian(false)
                .build();
    }

    public static final class Builder {

        private SampleRate sampleRate = SampleRate.HZ_44100;
        private BitDepth   bitDepth   = BitDepth.BITS_16;
        private int        channels   = 2;
        private boolean    bigEndian  = false;

        private Builder() {}

        public Builder sampleRate(SampleRate sampleRate) {
            this.sampleRate = sampleRate; return this;
        }
        public Builder bitDepth(BitDepth bitDepth) {
            this.bitDepth = bitDepth; return this;
        }
        public Builder channels(int channels) {
            if (channels < 1 || channels > 8)
                throw new IllegalArgumentException("channels must be 1–8, got: " + channels);
            this.channels = channels; return this;
        }
        public Builder bigEndian(boolean bigEndian) {
            this.bigEndian = bigEndian; return this;
        }
        public AudioFormat build() {
            return new AudioFormat(this);
        }
    }
}