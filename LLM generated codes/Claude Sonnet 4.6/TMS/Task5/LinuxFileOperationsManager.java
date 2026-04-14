package com.linux.native_fileops;

import java.nio.charset.StandardCharsets;

/**
 * Central orchestrator that composes {@link ExtendedAttributeReader},
 * {@link FilePermissionModifier}, and {@link SymbolicLinkCreator} into a
 * unified, lifecycle-managed API.
 *
 * <p>Enforces a strict platform guard at construction time: if the JVM is
 * not running on Linux, the constructor throws
 * {@link UnsupportedOperationException} before any native library is
 * loaded, providing a clear failure rather than a cryptic
 * {@code UnsatisfiedLinkError} at the first native call.</p>
 *
 * <p>All public methods guard against null or blank path arguments and
 * surface errors as {@link LinuxFileOperationException} carrying the
 * original {@code errno} code.</p>
 */
public class LinuxFileOperationsManager {

    private final ExtendedAttributeReader  xattrReader;
    private final FilePermissionModifier   permModifier;
    private final SymbolicLinkCreator      symlinkCreator;

    /**
     * Constructs a {@code LinuxFileOperationsManager} and verifies that the
     * JVM is running on Linux.
     *
     * @throws UnsupportedOperationException if the host OS is not Linux
     */
    public LinuxFileOperationsManager() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) {
            throw new UnsupportedOperationException(
                "LinuxFileOperationsManager is Linux-only. Detected OS: " +
                System.getProperty("os.name"));
        }
        this.xattrReader    = new ExtendedAttributeReader();
        this.permModifier   = new FilePermissionModifier();
        this.symlinkCreator = new SymbolicLinkCreator();
    }

    // ------------------------------------------------------------------
    // Extended Attributes
    // ------------------------------------------------------------------

    /**
     * Lists the names of all extended attributes attached to the file at
     * {@code filePath}.
     *
     * @param filePath path to the target file or directory
     * @return non-null, possibly empty array of xattr name strings
     * @throws LinuxFileOperationException  if the native {@code listxattr(2)}
     *                                      call fails
     * @throws IllegalArgumentException     if {@code filePath} is null or blank
     */
    public String[] listXattrNames(String filePath)
            throws LinuxFileOperationException {
        validatePath(filePath, "filePath");
        String[] names = xattrReader.listAttributeNames(filePath);
        System.out.printf("listxattr('%s') → %d attribute(s) found.%n",
                filePath, names.length);
        return names;
    }

    /**
     * Reads the value of a named extended attribute and decodes it as UTF-8.
     *
     * @param filePath      path to the target file
     * @param attributeName fully-qualified xattr name (e.g., {@code "user.author"})
     * @return UTF-8 decoded attribute value string
     * @throws LinuxFileOperationException  if the native {@code getxattr(2)}
     *                                      call fails
     * @throws IllegalArgumentException     if either argument is null or blank
     */
    public String readXattr(String filePath, String attributeName)
            throws LinuxFileOperationException {
        validatePath(filePath,      "filePath");
        validatePath(attributeName, "attributeName");
        byte[] raw = xattrReader.readAttribute(filePath, attributeName);
        String value = new String(raw, StandardCharsets.UTF_8);
        System.out.printf("getxattr('%s', '%s') → '%s'%n", filePath, attributeName, value);
        return value;
    }

    // ------------------------------------------------------------------
    // File Permissions
    // ------------------------------------------------------------------

    /**
     * Applies a POSIX mode to the file at {@code filePath}.
     *
     * @param filePath path to the target file or directory
     * @param mode     POSIX mode word (0 – 4095); use
     *                 {@link FilePermissionModifier#buildOctalMode} to
     *                 construct the value from individual flags
     * @throws LinuxFileOperationException  if the native {@code chmod(2)}
     *                                      call fails
     * @throws IllegalArgumentException     if {@code filePath} is null or
     *                                      blank, or if {@code mode} is
     *                                      outside [0, 4095]
     */
    public void chmod(String filePath, int mode)
            throws LinuxFileOperationException {
        validatePath(filePath, "filePath");
        permModifier.changePermissions(filePath, mode);
        System.out.printf("chmod('%s', %s) applied successfully.%n",
                filePath, permModifier.formatOctalMode(mode));
    }

    /**
     * Reads and returns the current POSIX mode word of the file at
     * {@code filePath} as a human-readable four-digit octal string.
     *
     * @param filePath path to the target file
     * @return formatted octal mode string (e.g., {@code "0755"})
     * @throws LinuxFileOperationException  if the native {@code stat(2)}
     *                                      call fails
     * @throws IllegalArgumentException     if {@code filePath} is null or blank
     */
    public String statMode(String filePath)
            throws LinuxFileOperationException {
        validatePath(filePath, "filePath");
        int mode = permModifier.getPermissions(filePath);
        String formatted = permModifier.formatOctalMode(mode);
        System.out.printf("stat('%s') → mode %s%n", filePath, formatted);
        return formatted;
    }

    // ------------------------------------------------------------------
    // Symbolic Links
    // ------------------------------------------------------------------

    /**
     * Creates a symbolic link at {@code linkPath} pointing to
     * {@code targetPath}.
     *
     * @param targetPath what the new symlink will point to
     * @param linkPath   filesystem path of the new symlink
     * @throws LinuxFileOperationException  if the native {@code symlink(2)}
     *                                      call fails
     * @throws IllegalArgumentException     if either argument is null or blank
     */
    public void createSymlink(String targetPath, String linkPath)
            throws LinuxFileOperationException {
        validatePath(targetPath, "targetPath");
        validatePath(linkPath,   "linkPath");
        symlinkCreator.createSymlink(targetPath, linkPath);
        System.out.printf("symlink('%s' → '%s') created.%n", linkPath, targetPath);
    }

    /**
     * Resolves and returns the raw target path stored in the symlink at
     * {@code linkPath}.
     *
     * @param linkPath path to an existing symbolic link
     * @return the raw target string stored in the symlink inode
     * @throws LinuxFileOperationException  if the native {@code readlink(2)}
     *                                      call fails
     * @throws IllegalArgumentException     if {@code linkPath} is null or blank
     */
    public String resolveSymlink(String linkPath)
            throws LinuxFileOperationException {
        validatePath(linkPath, "linkPath");
        String target = symlinkCreator.resolveSymlink(linkPath);
        System.out.printf("readlink('%s') → '%s'%n", linkPath, target);
        return target;
    }

    // ------------------------------------------------------------------
    // Entry point (smoke test)
    // ------------------------------------------------------------------

    public static void main(String[] args) throws LinuxFileOperationException {
        LinuxFileOperationsManager manager = new LinuxFileOperationsManager();

        // Extended attributes
        String testFile = "/var/data/report.pdf";
        String[] xattrs = manager.listXattrNames(testFile);
        for (String attr : xattrs) {
            System.out.println(attr + " = " + manager.readXattr(testFile, attr));
        }

        // File permissions
        FilePermissionModifier mod = new FilePermissionModifier();
        int mode = mod.buildOctalMode(
            true,  true,  true,    // owner  : rwx
            true,  false, true,    // group  : r-x
            false, false, false,   // others : ---
            false, false, false    // special: none
        );
        manager.chmod("/opt/app/launcher", mode);
        System.out.println("Current mode: " + manager.statMode("/opt/app/launcher"));

        // Symbolic links
        manager.createSymlink(
            "/opt/myapp/bin/myapp-2.1.0",
            "/usr/local/bin/myapp"
        );
        System.out.println("Resolves to: " +
            manager.resolveSymlink("/usr/local/bin/myapp"));
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Validates that a path argument is non-null and non-blank.
     *
     * @param path      the path value to validate
     * @param paramName the parameter name to include in the error message
     * @throws IllegalArgumentException if {@code path} is null or blank
     */
    private void validatePath(String path, String paramName) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                paramName + " must not be null or blank.");
        }
    }
}