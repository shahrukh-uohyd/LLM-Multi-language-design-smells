// ImageInfo.java
public class ImageInfo {
    private String imagePath;
    private String fileName;
    
    public ImageInfo(String imagePath, String fileName) {
        this.imagePath = imagePath;
        this.fileName = fileName;
    }
    
    public String getImagePath() { return imagePath; }
    public String getFileName() { return fileName; }
}