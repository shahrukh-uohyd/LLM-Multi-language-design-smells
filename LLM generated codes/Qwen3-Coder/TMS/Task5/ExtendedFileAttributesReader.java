/**
 * Native Linux extended file attributes reader
 */
public class ExtendedFileAttributesReader {
    
    static {
        System.loadLibrary("linux_xattr_native");
    }
    
    // Native method to get extended attribute value
    private native String nativeGetXAttr(String filePath, String attributeName);
    
    // Native method to list all extended attributes for a file
    private native String[] nativeListXAttrs(String filePath);
    
    // Native method to set extended attribute value
    private native boolean nativeSetXAttr(String filePath, String attributeName, String value);
    
    // Native method to remove extended attribute
    private native boolean nativeRemoveXAttr(String filePath, String attributeName);
    
    /**
     * Get the value of a specific extended attribute for a file
     * @param filePath Path to the file
     * @param attributeName Name of the extended attribute (e.g., "user.myattr")
     * @return Value of the extended attribute, or null if not found
     * @throws IllegalArgumentException if file path or attribute name is null/empty
     */
    public String getXAttr(String filePath, String attributeName) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (attributeName == null || attributeName.isEmpty()) {
            throw new IllegalArgumentException("Attribute name cannot be null or empty");
        }
        return nativeGetXAttr(filePath, attributeName);
    }
    
    /**
     * List all extended attributes for a file
     * @param filePath Path to the file
     * @return Array of extended attribute names, or empty array if none exist
     * @throws IllegalArgumentException if file path is null or empty
     */
    public String[] listXAttrs(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        String[] attrs = nativeListXAttrs(filePath);
        return attrs != null ? attrs : new String[0];
    }
    
    /**
     * Set an extended attribute value for a file
     * @param filePath Path to the file
     * @param attributeName Name of the extended attribute
     * @param value Value to set for the attribute
     * @return true if successful, false otherwise
     * @throws IllegalArgumentException if parameters are null/empty
     */
    public boolean setXAttr(String filePath, String attributeName, String value) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (attributeName == null || attributeName.isEmpty()) {
            throw new IllegalArgumentException("Attribute name cannot be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("Attribute value cannot be null");
        }
        return nativeSetXAttr(filePath, attributeName, value);
    }
    
    /**
     * Remove an extended attribute from a file
     * @param filePath Path to the file
     * @param attributeName Name of the extended attribute to remove
     * @return true if successful, false otherwise
     * @throws IllegalArgumentException if parameters are null/empty
     */
    public boolean removeXAttr(String filePath, String attributeName) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (attributeName == null || attributeName.isEmpty()) {
            throw new IllegalArgumentException("Attribute name cannot be null or empty");
        }
        return nativeRemoveXAttr(filePath, attributeName);
    }
}