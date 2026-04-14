public class ImageRecord {
    public String imageId;
    public byte[] imageData; 

    public ImageRecord(String imageId, byte[] imageData) {
        this.imageId = imageId;
        this.imageData = imageData;
    }
}