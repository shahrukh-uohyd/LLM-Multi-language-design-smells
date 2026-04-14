public class LinuxNativeFileOps {

    // Load the shared C/C++ Linux library
    static {
        // Loads liblinux_file_ops.so on Linux
        System.loadLibrary("linux_file_ops");
    }

    // ---------------------------------------------------------
    // 1. Extended Attributes (xattr) Reader
    // ---------------------------------------------------------
    public static class ExtendedAttributes {
        /**
         * Reads the value of an extended attribute for a given file.
         * Wraps the Linux 'getxattr' system call.
         *
         * @param filePath      The absolute or relative path to the file.
         * @param attributeName The name of the extended attribute (e.g., "user.comment").
         * @return A byte array containing the attribute's value, or null if the 
         *         attribute does not exist or an error occurs.
         */
        public native byte[] getExtendedAttribute(String filePath, String attributeName);
    }

    // ---------------------------------------------------------
    // 2. File Permissions Modifier (chmod)
    // ---------------------------------------------------------
    public static class FilePermissions {
        /**
         * Changes the file mode bits (permissions) of a given file.
         * Wraps the standard POSIX 'chmod' system call.
         *
         * @param filePath The absolute or relative path to the file.
         * @param mode     The permissions mode as an integer (typically written in octal in Java, e.g., 0755).
         * @return 0 on success, or -1 on failure (e.g., file not found, permission denied).
         */
        public native int changePermissions(String filePath, int mode);
    }

    // ---------------------------------------------------------
    // 3. Symbolic Link Creator (symlink)
    // ---------------------------------------------------------
    public static class SymbolicLinks {
        /**
         * Creates a symbolic link at the specified linkpath that points to the targetpath.
         * Wraps the standard POSIX 'symlink' system call.
         *
         * @param targetPath The existing path that the symlink should point to.
         * @param linkPath   The path where the new symbolic link will be created.
         * @return 0 on success, or -1 on failure (e.g., linkPath already exists).
         */
        public native int createSymbolicLink(String targetPath, String linkPath);
    }

    // ---------------------------------------------------------
    // Example Usage
    // ---------------------------------------------------------
    public static void main(String[] args) {
        // 1. Extended Attributes Example
        ExtendedAttributes xattrUtil = new ExtendedAttributes();
        byte[] attrValue = xattrUtil.getExtendedAttribute("/tmp/testfile.txt", "user.mime_type");
        if (attrValue != null) {
            System.out.println("Extended Attribute Value: " + new String(attrValue));
        }

        // 2. File Permissions Example (chmod)
        FilePermissions chmodUtil = new FilePermissions();
        // 0755 in octal: rwxr-xr-x
        int chmodResult = chmodUtil.changePermissions("/tmp/testscript.sh", 0755); 
        System.out.println("chmod result: " + (chmodResult == 0 ? "Success" : "Failed"));

        // 3. Symbolic Link Example
        SymbolicLinks symlinkUtil = new SymbolicLinks();
        int symlinkResult = symlinkUtil.createSymbolicLink("/usr/bin/python3", "/tmp/my_python_link");
        System.out.println("symlink result: " + (symlinkResult == 0 ? "Success" : "Failed"));
    }
}