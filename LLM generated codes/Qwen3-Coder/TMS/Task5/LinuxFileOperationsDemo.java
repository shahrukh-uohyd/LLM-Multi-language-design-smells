/**
 * Demo application showing usage of Linux-specific file operations
 */
public class LinuxFileOperationsDemo {
    
    public static void main(String[] args) {
        LinuxFileManager fileManager = new LinuxFileManager();
        
        try {
            // Example 1: Extended file attributes
            System.out.println("=== Extended File Attributes ===");
            String testFile = "/tmp/test_file.txt";
            
            // Create a test file first (you might want to do this differently in production)
            java.io.File file = new java.io.File(testFile);
            file.createNewFile();
            
            // Set an extended attribute
            boolean attrSet = fileManager.setXAttr(testFile, "user.description", "Test file for demo");
            System.out.println("Extended attribute set: " + attrSet);
            
            // Get the extended attribute
            String attrValue = fileManager.getXAttr(testFile, "user.description");
            System.out.println("Extended attribute value: " + attrValue);
            
            // Example 2: File permissions
            System.out.println("\n=== File Permissions ===");
            // Change permissions to 0644 (rw-r--r--)
            boolean permChanged = fileManager.chmod(testFile, 0644);
            System.out.println("Permissions changed: " + permChanged);
            
            // Get current permissions
            LinuxFilePermissionsModifier permMod = new LinuxFilePermissionsModifier();
            int currentPerms = permMod.getPermissions(testFile);
            System.out.println("Current permissions (numeric): " + currentPerms);
            System.out.println("Current permissions (string): " + permMod.modeToString(currentPerms));
            
            // Example 3: Symbolic links
            System.out.println("\n=== Symbolic Links ===");
            String linkPath = "/tmp/test_link";
            
            // Create a symbolic link
            boolean linkCreated = fileManager.createSymbolicLink(testFile, linkPath);
            System.out.println("Symbolic link created: " + linkCreated);
            
            // Check if it's a symbolic link
            boolean isSymlink = fileManager.isSymbolicLink(linkPath);
            System.out.println("Is symbolic link: " + isSymlink);
            
            // Read the target of the symbolic link
            LinuxSymbolicLinkCreator symlinkCreator = new LinuxSymbolicLinkCreator();
            String target = symlinkCreator.readSymbolicLink(linkPath);
            System.out.println("Symbolic link target: " + target);
            
            System.out.println("\nLinux file operations completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Linux file operation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}