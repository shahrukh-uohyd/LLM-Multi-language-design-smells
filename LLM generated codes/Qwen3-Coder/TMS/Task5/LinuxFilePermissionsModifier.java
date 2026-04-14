/**
 * Native Linux file permissions modifier (chmod equivalent)
 */
public class LinuxFilePermissionsModifier {
    
    static {
        System.loadLibrary("linux_chmod_native");
    }
    
    // Native method to change file permissions using numeric mode
    private native boolean nativeChmod(String filePath, int mode);
    
    // Native method to change file permissions using symbolic notation
    private native boolean nativeChmodSymbolic(String filePath, String symbolicMode);
    
    // Native method to get current file permissions
    private native int nativeGetPermissions(String filePath);
    
    // Native method to check specific permission bits
    private native boolean nativeCheckPermission(String filePath, String permissionType);
    
    /**
     * Change file permissions using numeric mode (e.g., 0755, 0644)
     * @param filePath Path to the file
     * @param mode Numeric permission mode (octal format without leading zero)
     * @return true if successful, false otherwise
     * @throws IllegalArgumentException if file path is null or mode is invalid
     */
    public boolean chmod(String filePath, int mode) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (mode < 0 || mode > 07777) {
            throw new IllegalArgumentException("Invalid mode: must be between 0 and 07777");
        }
        return nativeChmod(filePath, mode);
    }
    
    /**
     * Change file permissions using symbolic notation (e.g., "u+rwx,g+rx,o+r")
     * @param filePath Path to the file
     * @param symbolicMode Symbolic permission mode string
     * @return true if successful, false otherwise
     * @throws IllegalArgumentException if parameters are invalid
     */
    public boolean chmodSymbolic(String filePath, String symbolicMode) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (symbolicMode == null || symbolicMode.isEmpty()) {
            throw new IllegalArgumentException("Symbolic mode cannot be null or empty");
        }
        return nativeChmodSymbolic(filePath, symbolicMode);
    }
    
    /**
     * Get current file permissions as numeric mode
     * @param filePath Path to the file
     * @return Current permission mode as integer
     * @throws IllegalArgumentException if file path is null or empty
     */
    public int getPermissions(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        return nativeGetPermissions(filePath);
    }
    
    /**
     * Check if file has specific permission (read, write, execute)
     * @param filePath Path to the file
     * @param permissionType Type of permission to check ("read", "write", "execute")
     * @return true if file has the specified permission, false otherwise
     * @throws IllegalArgumentException if parameters are invalid
     */
    public boolean checkPermission(String filePath, String permissionType) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (permissionType == null || !permissionType.matches("^(read|write|execute)$")) {
            throw new IllegalArgumentException("Permission type must be 'read', 'write', or 'execute'");
        }
        return nativeCheckPermission(filePath, permissionType);
    }
    
    /**
     * Convert numeric mode to readable string format (e.g., "rwxr-xr-x")
     * @param mode Numeric permission mode
     * @return String representation of permissions
     */
    public String modeToString(int mode) {
        StringBuilder sb = new StringBuilder();
        for (int i = 6; i >= 0; i -= 3) {
            int ownerBits = (mode >> i) & 7;
            sb.append((ownerBits & 4) != 0 ? 'r' : '-');
            sb.append((ownerBits & 2) != 0 ? 'w' : '-');
            sb.append((ownerBits & 1) != 0 ? 'x' : '-');
        }
        return sb.toString();
    }
}