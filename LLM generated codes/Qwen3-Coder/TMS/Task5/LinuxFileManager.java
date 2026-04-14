/**
 * Combined Linux file operations manager
 */
public class LinuxFileManager {
    
    private final ExtendedFileAttributesReader xattrReader;
    private final LinuxFilePermissionsModifier permissionModifier;
    private final LinuxSymbolicLinkCreator symlinkCreator;
    
    public LinuxFileManager() {
        this.xattrReader = new ExtendedFileAttributesReader();
        this.permissionModifier = new LinuxFilePermissionsModifier();
        this.symlinkCreator = new LinuxSymbolicLinkCreator();
    }
    
    /**
     * Get extended attribute value
     * @param filePath Path to the file
     * @param attributeName Name of the extended attribute
     * @return Attribute value or null
     */
    public String getXAttr(String filePath, String attributeName) {
        return xattrReader.getXAttr(filePath, attributeName);
    }
    
    /**
     * Set extended attribute value
     * @param filePath Path to the file
     * @param attributeName Name of the extended attribute
     * @param value Value to set
     * @return true if successful
     */
    public boolean setXAttr(String filePath, String attributeName, String value) {
        return xattrReader.setXAttr(filePath, attributeName, value);
    }
    
    /**
     * Change file permissions
     * @param filePath Path to the file
     * @param mode Numeric permission mode
     * @return true if successful
     */
    public boolean chmod(String filePath, int mode) {
        return permissionModifier.chmod(filePath, mode);
    }
    
    /**
     * Create symbolic link
     * @param targetPath Target path for the link
     * @param linkPath Path where to create the link
     * @return true if successful
     */
    public boolean createSymbolicLink(String targetPath, String linkPath) {
        return symlinkCreator.createSymbolicLink(targetPath, linkPath);
    }
    
    /**
     * Check if path is a symbolic link
     * @param path Path to check
     * @return true if it's a symbolic link
     */
    public boolean isSymbolicLink(String path) {
        return symlinkCreator.isSymbolicLink(path);
    }
}