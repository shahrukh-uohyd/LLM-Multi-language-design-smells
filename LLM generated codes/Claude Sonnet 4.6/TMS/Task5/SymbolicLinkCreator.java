package com.linux.native_fileops;

/**
 * Provides native bindings for creating symbolic links and resolving their
 * targets on Linux via the {@code symlink(2)} and {@code readlink(2)} syscalls.
 *
 * <p>While {@code java.nio.file.Files.createSymbolicLink()} covers the
 * basic creation case, this class gives direct syscall access for
 * performance-critical paths (e.g., bulk link creation during deployment)
 * and exposes {@code readlink(2)} to retrieve the raw, un-canonicalised
 * link target without the overhead of {@code realpath(3)}.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   SymbolicLinkCreator linker = new SymbolicLinkCreator();
 *
 *   // Create: /usr/local/bin/myapp -> /opt/myapp/bin/myapp-2.1.0
 *   linker.createSymlink(
 *       "/opt/myapp/bin/myapp-2.1.0",  // target (what the link points to)
 *       "/usr/local/bin/myapp"         // linkPath (the new symlink itself)
 *   );
 *
 *   // Resolve:
 *   String target = linker.resolveSymlink("/usr/local/bin/myapp");
 *   System.out.println(target); // "/opt/myapp/bin/myapp-2.1.0"
 * }</pre>
 *
 * <p><strong>Platform requirement:</strong> Linux only.</p>
 *
 * <p><strong>Thread safety:</strong> All methods are stateless; the class
 * is safe for concurrent use.</p>
 */
public class SymbolicLinkCreator {

    static {
        // Loads libsymlink_creator.so — Linux only
        System.loadLibrary("symlink_creator");
    }

    // ------------------------------------------------------------------
    // Native method declarations
    // ------------------------------------------------------------------

    /**
     * Creates a symbolic link at {@code linkPath} pointing to
     * {@code targetPath} via the {@code symlink(2)} syscall.
     *
     * <p>The {@code targetPath} is stored verbatim in the symlink inode
     * — it is not resolved or validated by the kernel at creation time.
     * The target may be absolute ({@code "/opt/app/bin/server"}) or
     * relative ({@code "../lib/libc.so.6"}); relative targets are
     * interpreted at <em>dereference</em> time relative to the directory
     * containing the symlink, not the current working directory.</p>
     *
     * @param targetPath the path that the new symlink will point to;
     *                   may be absolute or relative; must not be
     *                   {@code null} or blank
     * @param linkPath   the filesystem path at which the new symlink
     *                   will be created; its parent directory must exist;
     *                   must not be {@code null} or blank
     * @throws LinuxFileOperationException if {@code symlink(2)} fails
     *         (e.g., {@code EEXIST} if {@code linkPath} already exists,
     *         {@code EACCES} if the caller lacks write permission on the
     *         parent directory, {@code ENOENT} if a parent directory
     *         component does not exist)
     * @throws IllegalArgumentException    if either argument is
     *                                     {@code null} or blank
     */
    public native void createSymlink(String targetPath, String linkPath)
            throws LinuxFileOperationException;

    /**
     * Reads and returns the raw target path stored in the symlink at
     * {@code linkPath} via the {@code readlink(2)} syscall.
     *
     * <p>Unlike {@code realpath(3)}, this method returns the <em>stored</em>
     * target string without resolving intermediate symlinks or
     * canonicalising the path.  If the stored target is a relative path,
     * the returned string will also be relative.</p>
     *
     * @param linkPath absolute or relative path to an existing symbolic
     *                 link; must not be {@code null} or blank
     * @return the raw target string stored in the symlink inode;
     *         never {@code null}
     * @throws LinuxFileOperationException if {@code readlink(2)} fails
     *         (e.g., {@code ENOENT} if {@code linkPath} does not exist,
     *         {@code EINVAL} if {@code linkPath} exists but is not a
     *         symbolic link)
     * @throws IllegalArgumentException    if {@code linkPath} is
     *                                     {@code null} or blank
     */
    public native String resolveSymlink(String linkPath)
            throws LinuxFileOperationException;

    // ------------------------------------------------------------------
    // Convenience wrappers (pure Java — no JNI overhead)
    // ------------------------------------------------------------------

    /**
     * Creates a symbolic link only if no file system object already exists
     * at {@code linkPath}, making the operation idempotent.
     *
     * @param targetPath the path the symlink should point to
     * @param linkPath   the path at which the symlink should be created
     * @return {@code true} if the symlink was created; {@code false} if
     *         {@code linkPath} already existed and was skipped
     * @throws LinuxFileOperationException if creation fails for any reason
     *                                     other than {@code EEXIST}
     *                                     (errno 17)
     */
    public boolean createSymlinkIfAbsent(String targetPath, String linkPath)
            throws LinuxFileOperationException {
        try {
            createSymlink(targetPath, linkPath);
            return true;
        } catch (LinuxFileOperationException ex) {
            if (ex.getErrnoCode() == 17) { // EEXIST
                return false;
            }
            throw ex;
        }
    }

    /**
     * Verifies that the symlink at {@code linkPath} currently points to
     * {@code expectedTarget} by resolving it and comparing the result.
     *
     * @param linkPath       path to an existing symbolic link
     * @param expectedTarget the target path the symlink is expected to store
     * @return {@code true} if the resolved target equals
     *         {@code expectedTarget}; {@code false} otherwise
     * @throws LinuxFileOperationException if {@link #resolveSymlink} fails
     */
    public boolean pointsTo(String linkPath, String expectedTarget)
            throws LinuxFileOperationException {
        String actual = resolveSymlink(linkPath);
        return actual.equals(expectedTarget);
    }
}