public class DesktopAudioApp {

    public static void main(String[] args) {
        AudioProcessor processor = new AudioProcessor();

        byte[] encodedAudio = loadAudioFromDisk();
        byte[] pcmAudio = processor.decode(encodedAudio);

        System.out.println("Decoded audio size: " + pcmAudio.length);
    }

    private static byte[] loadAudioFromDisk() {
        // Stub for example purposes
        return new byte[] { 1, 2, 3, 4 };
    }
}
