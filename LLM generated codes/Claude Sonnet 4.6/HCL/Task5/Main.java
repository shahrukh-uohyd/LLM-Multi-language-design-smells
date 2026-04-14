package com.audio;

import com.audio.AudioFormat.BitDepth;
import com.audio.AudioFormat.SampleRate;

import java.nio.charset.StandardCharsets;

/**
 * Entry point — demonstrates the full audio processing lifecycle:
 * <ol>
 *   <li>Library loading via {@link NativeAudioLibraryLoader} (auto-triggered).</li>
 *   <li>Initialisation with CD-quality and professional {@link AudioFormat}s.</li>
 *   <li>Audio decoding, gain adjustment, and resampling.</li>
 *   <li>Diagnostic inspection from {@link AudioProcessingResult}.</li>
 *   <li>Safe resource cleanup via try-with-resources.</li>
 * </ol>
 */
public final class Main {

    public static void main(String[] args) {
        printBanner();

        // ── Scenario 1 — CD quality decode ──────────────────────────────────
        System.out.println("\n══════ Scenario 1: CD-Quality Audio Decode ══════");
        try (AudioProcessor processor = new AudioProcessor()) {

            AudioFormat cdFormat = AudioFormat.cdQuality();
            processor.initialize(cdFormat);

            System.out.println("Library version   : " + processor.getLibraryVersion());
            System.out.println("Active format     : " + processor.getActiveFormat());

            // Simulate compressed audio payload (stub: UTF-8 bytes)
            byte[] encodedAudio = "SIMULATED_COMPRESSED_AUDIO_STREAM_MP3".getBytes(StandardCharsets.UTF_8);
            AudioProcessingResult result = processor.decodeAudio(encodedAudio);

            System.out.println("Decode result     : " + result);
            System.out.printf ("  PCM buffer      : %d bytes%n",   result.getBufferSize());
            System.out.printf ("  Duration        : %d ms%n",      result.getDurationMs());
            System.out.printf ("  Peak amplitude  : %.4f%n",       result.getPeakAmplitude());
            System.out.printf ("  RMS level       : %.4f%n",       result.getRmsLevel());
            System.out.printf ("  Clipped         : %b%n",         result.isClipped());

            // Gain: boost +6 dB
            byte[] boosted = processor.applyGain(result.getPcmData(), +6.0f);
            System.out.printf ("%nAfter +6 dB gain  : %d bytes%n", boosted.length);

            // Resample from 44.1 kHz → 48 kHz
            byte[] resampled = processor.resample(result.getPcmData(), 48_000);
            System.out.printf ("After resample    : %d bytes (44100→48000 Hz)%n", resampled.length);

        } catch (AudioException e) {
            System.err.println("Audio error: " + e.getMessage());
            System.exit(1);
        }

        // ── Scenario 2 — Professional format + attenuation ──────────────────
        System.out.println("\n══════ Scenario 2: Professional Format + Attenuation ══════");
        try (AudioProcessor processor = new AudioProcessor()) {

            AudioFormat proFormat = AudioFormat.professional();
            processor.initialize(proFormat);
            System.out.println("Active format     : " + processor.getActiveFormat());

            byte[] encodedAudio = "PROFESSIONAL_AUDIO_STREAM_96KHZ".getBytes(StandardCharsets.UTF_8);
            AudioProcessingResult result = processor.decodeAudio(encodedAudio);
            System.out.println("Decode result     : " + result);

            // Gain: attenuate -12 dB
            byte[] attenuated = processor.applyGain(result.getPcmData(), -12.0f);
            System.out.printf ("After -12 dB gain : %d bytes%n", attenuated.length);

        } catch (AudioException e) {
            System.err.println("Audio error: " + e.getMessage());
            System.exit(1);
        }

        // ── Scenario 3 — Custom format via builder ───────────────────────────
        System.out.println("\n══════ Scenario 3: Custom Format (8-ch Surround, 96 kHz) ══════");
        try (AudioProcessor processor = new AudioProcessor()) {

            AudioFormat surroundFormat = AudioFormat.builder()
                    .sampleRate(SampleRate.HZ_96000)
                    .bitDepth(BitDepth.BITS_24)
                    .channels(8)
                    .bigEndian(false)
                    .build();

            processor.initialize(surroundFormat);
            System.out.println("Active format     : " + processor.getActiveFormat());
            System.out.println("Byte rate         : " + surroundFormat.byteRate() + " B/s");

            byte[] encodedAudio = "SURROUND_AUDIO_8CH_96KHZ".getBytes(StandardCharsets.UTF_8);
            AudioProcessingResult result = processor.decodeAudio(encodedAudio);
            System.out.println("Decode result     : " + result);

        } catch (AudioException e) {
            System.err.println("Audio error: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("\n══════ All scenarios completed successfully ══════");
    }

    // -------------------------------------------------------------------------

    private static void printBanner() {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         Java JNI Native Audio Processor Demo         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println("Platform: " + com.audio.platform.Platform.current());
        System.out.println("Loaded  : " + NativeAudioLibraryLoader.isLoaded()
                         + "  →  " + (NativeAudioLibraryLoader.isLoaded()
                                       ? NativeAudioLibraryLoader.getLoadedLibraryPath()
                                       : "n/a"));
    }
}