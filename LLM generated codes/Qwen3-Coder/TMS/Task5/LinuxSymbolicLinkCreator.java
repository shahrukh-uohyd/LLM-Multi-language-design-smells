/**
 * Native Linux symbolic link creator
 */
public class LinuxSymbolicLinkCreator {
    
    static {
        System.loadLibrary("linux_symlink_native");
    }
    
    // Native method to create symbolic link
    private native boolean nativeCreateSymbolicLink(String targetPath, String linkPath);
    
    // Native method to read symbolic link target
    private native String nativeReadSymbolicLink(String linkPath);
    
    // Native method to check if path is a symbolic link
    private native boolean nativeIsSymbolicLink(String path);
    
    // Native method to remove symbolic link
    private native boolean nativeRemoveSymbolicLink(String linkPath);
    
    /**
     * Create a symbolic link pointing to the target file/directory
     * @param targetPath Path to the target file or directory
     * @param linkPath Path where the symbolic link should be created
     * @return true if successful, false otherwise
     * @throws IllegalArgumentException if paths are null or empty
     */
    public boolean createSymbolicLink(String targetPath, String linkPath) {
        if (targetPath == null || targetPath.isEmpty()) {
            throw new IllegalArgumentException("Target path cannot be null or empty");
        }
        if (linkPath == null || linkPath.isEmpty()) {
            throw new IllegalArgumentException("Link path cannot be null or empty");
        }
        return nativeCreateSymbolicLink(targetPath, linkPath);
    }
    
    /**
     * Read the target path from a symbolic link
     * @param linkPath Path to the symbolic link
     * @return Target path that the symlink points to, or null if not a symlink
     * @throws IllegalArgumentException if link path is null or empty
     */
    public String readSymbolicLink(String linkPath) {
        if (linkPath == null || linkPath.isEmpty()) {
            throw new IllegalArgumentException("Link path cannot be null or empty");
        }
        return nativeReadSymbolicLink(linkPath);
    }
    
    /**
     * Check if the given path is a symbolic link
     * @param path Path to check
     * @return true if path is a symbolic link, false otherwise
     * @throws IllegalArgumentException if path is null or empty
     */
    public boolean isSymbolicLink(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        return nativeIsSymbolicLink(path);
    }
    
    /**
     * Remove a symbolic link
     * @param linkPath Path to the symbolic link to remove
     * @return true if successful, false otherwise
     * @throws IllegalArgumentException if link path is null or empty
     */
    public boolean removeSymbolicLink(String linkPath) {
        if (linkPath == null || linkPath.isEmpty()) {
            throw new IllegalArgumentException("Link path cannot be null or empty");
        }
        return nativeRemoveSymbolicLink(linkPath);
    }
}