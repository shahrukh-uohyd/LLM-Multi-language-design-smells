public class NativeFileSystem {

    static {
        System.loadLibrary("nativefs");
    }

    // Existing native methods (file-system related)
    public native long getFileSize(String path);
    public native boolean fileExists(String path);

    // New native cryptographic feature
    public native byte[] computeHash(byte[] data);

    // Internal file-system logic
    public void printFileInfo(String path) {
        if (fileExists(path)) {
            System.out.println("File size (native): " + getFileSize(path));
        } else {
            System.out.println("File does not exist");
        }
    }
}
