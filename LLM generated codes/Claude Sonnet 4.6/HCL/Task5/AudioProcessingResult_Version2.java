package com.audio;

import java.util.Objects;

/**
 * Immutable value object returned by each native audio processing call.
 *
 * <ul>
 *   <li>{@code pcmData}        — decoded PCM audio bytes</li>
 *   <li>{@code format}         — audio format of the decoded data</li>
 *   <li>{@code durationMs}     — audio duration in milliseconds</li>
 *   <li>{@code peakAmplitude}  — normalised peak amplitude [0.0, 1.0]</li>
 *   <li>{@code rmsLevel}       — root-mean-square signal level [0.0, 1.0]</li>
 *   <li>{@code clipped}        — {@code true} if clipping was detected</li>
 * </ul>
 */
public final class AudioProcessingResult {

    private final byte[]      pcmData;
    private final AudioFormat format;
    private final long        durationMs;
    private final float       peakAmplitude;
    private final float       rmsLevel;
    private final boolean     clipped;

    public AudioProcessingResult(
            byte[]      pcmData,
            AudioFormat format,
            long        durationMs,
            float       peakAmplitude,
            float       rmsLevel,
            boolean     clipped) {

        this.pcmData       = Objects.requireNonNull(pcmData, "pcmData").clone();
        this.format        = Objects.requireNonNull(format,  "format");
        this.durationMs    = durationMs;
        this.peakAmplitude = peakAmplitude;
        this.rmsLevel      = rmsLevel;
        this.clipped       = clipped;
    }

    /** Returns a defensive copy of the decoded PCM audio bytes. */
    public byte[]      getPcmData()       { return pcmData.clone(); }
    public AudioFormat getFormat()        { return format;          }
    public long        getDurationMs()    { return durationMs;      }
    public float       getPeakAmplitude() { return peakAmplitude;   }
    public float       getRmsLevel()      { return rmsLevel;        }
    public boolean     isClipped()        { return clipped;         }

    /** Convenience: PCM buffer size in bytes. */
    public int getBufferSize() { return pcmData.length; }

    @Override
    public String toString() {
        return String.format(
            "AudioProcessingResult{"
            + "bufferSize=%d B"
            + ", format=%s"
            + ", durationMs=%d ms"
            + ", peakAmplitude=%.4f"
            + ", rmsLevel=%.4f"
            + ", clipped=%b"
            + "}",
            pcmData.length, format, durationMs,
            peakAmplitude, rmsLevel, clipped);
    }
}