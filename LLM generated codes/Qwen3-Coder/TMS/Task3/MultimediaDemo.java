/**
 * Demo application showing usage of multimedia native features
 */
public class MultimediaDemo {
    
    public static void main(String[] args) {
        MultimediaProcessor processor = new MultimediaProcessor();
        
        try {
            // Example 1: Audio metadata extraction
            System.out.println("=== Audio Metadata Extraction ===");
            String audioFile = "sample.mp3";
            AudioMetadata metadata = processor.processAudioMetadata(audioFile);
            System.out.println("Audio metadata: " + metadata);
            
            // Example 2: Specific metadata field
            String title = processor.getAudioField(audioFile, "title");
            System.out.println("Title: " + title);
            
            // Example 3: Video frame rotation
            System.out.println("\n=== Video Frame Rotation ===");
            // Create dummy frame buffer (in real app, this would come from video source)
            int width = 1920;
            int height = 1080;
            byte[] frameBuffer = new byte[width * height * 3]; // RGB format
            String pixelFormat = "RGB24";
            int rotationAngle = 90;
            
            byte[] rotatedFrame = processor.processVideoRotation(
                frameBuffer, width, height, rotationAngle, pixelFormat);
            
            System.out.println("Original frame size: " + frameBuffer.length + " bytes");
            System.out.println("Rotated frame size: " + (rotatedFrame != null ? rotatedFrame.length : 0) + " bytes");
            System.out.println("Rotation completed: " + (rotatedFrame != null));
            
            // Example 4: In-place rotation
            boolean inPlaceSuccess = processor.rotateFrameInPlace(frameBuffer, width, height, 180);
            System.out.println("In-place rotation success: " + inPlaceSuccess);
            
        } catch (Exception e) {
            System.err.println("Error in multimedia processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}