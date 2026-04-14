package com.linux.native_fileops;

/**
 * Provides native bindings for modifying POSIX file permissions on Linux
 * via the {@code chmod(2)} syscall.
 *
 * <p>Java's {@code java.nio.file.Files.setPosixFilePermissions()} covers the
 * basic nine permission bits but cannot express the sticky bit ({@code 1000}),
 * setuid ({@code 4000}), or setgid ({@code 2000}) flags.  This class exposes
 * the full 12-bit POSIX mode word directly to the native layer, removing that
 * limitation.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   FilePermissionModifier modifier = new FilePermissionModifier();
 *
 *   // Build a mode: rwxr-x--- (0750)
 *   int mode = modifier.buildOctalMode(
 *       true,  true,  true,   // owner  : rwx
 *       true,  false, true,   // group  : r-x
 *       false, false, false,  // others : ---
 *       false, false, false   // special: no setuid / setgid / sticky
 *   );
 *   modifier.changePermissions("/opt/app/launcher", mode);
 * }</pre>
 *
 * <p><strong>Platform requirement:</strong> Linux only.</p>
 *
 * <p><strong>Thread safety:</strong> All methods are stateless; the class
 * is safe for concurrent use.</p>
 */
public class FilePermissionModifier {

    static {
        // Loads libfile_permissions.so — Linux only
        System.loadLibrary("file_permissions");
    }

    // ------------------------------------------------------------------
    // Native method declarations
    // ------------------------------------------------------------------

    /**
     * Applies the supplied POSIX mode to the file at {@code filePath} via
     * the {@code chmod(2)} syscall.
     *
     * <p>The {@code mode} argument is the full 12-bit POSIX mode word
     * in decimal form — use {@link #buildOctalMode} to construct it
     * safely from individual permission flags, or supply a well-known
     * octal literal such as {@code 0755} (= 493 decimal) directly.</p>
     *
     * @param filePath absolute or relative path to the target file or
     *                 directory; symlinks are followed; must not be
     *                 {@code null} or blank
     * @param mode     full POSIX mode word (0 – 4095 inclusive);
     *                 use {@link #buildOctalMode} to construct the value
     * @throws LinuxFileOperationException if {@code chmod(2)} fails
     *         (e.g., {@code EPERM} if the caller does not own the file,
     *         {@code ENOENT} if the path does not exist)
     * @throws IllegalArgumentException    if {@code filePath} is null or
     *                                     blank, or if {@code mode} is
     *                                     outside [0, 4095]
     */
    public native void changePermissions(String filePath, int mode)
            throws LinuxFileOperationException;

    /**
     * Reads and returns the current POSIX mode word of the file at
     * {@code filePath} via the {@code stat(2)} syscall.
     *
     * <p>The returned integer is the raw {@code st_mode} value masked to
     * the lower 12 bits (i.e., permission bits + setuid/setgid/sticky).
     * Pass it to {@link #formatOctalMode(int)} for a human-readable
     * representation.</p>
     *
     * @param filePath path to the target file; must not be {@code null}
     *                 or blank
     * @return current POSIX mode word in the range [0, 4095]
     * @throws LinuxFileOperationException if {@code stat(2)} fails
     * @throws IllegalArgumentException    if {@code filePath} is null or blank
     */
    public native int getPermissions(String filePath)
            throws LinuxFileOperationException;

    // ------------------------------------------------------------------
    // Convenience wrappers (pure Java — no JNI overhead)
    // ------------------------------------------------------------------

    /**
     * Constructs a POSIX mode word from twelve individual permission flags
     * plus the three special bits.
     *
     * <p>The resulting integer can be passed directly to
     * {@link #changePermissions(String, int)}.</p>
     *
     * @param ownerRead    owner read  bit  (0400)
     * @param ownerWrite   owner write bit  (0200)
     * @param ownerExecute owner exec  bit  (0100)
     * @param groupRead    group read  bit  (0040)
     * @param groupWrite   group write bit  (0020)
     * @param groupExecute group exec  bit  (0010)
     * @param othersRead   others read  bit (0004)
     * @param othersWrite  others write bit (0002)
     * @param othersExecute others exec bit (0001)
     * @param setuid       setuid bit       (4000)
     * @param setgid       setgid bit       (2000)
     * @param sticky       sticky bit       (1000)
     * @return POSIX mode word in the range [0, 4095]
     */
    public int buildOctalMode(boolean ownerRead,    boolean ownerWrite,   boolean ownerExecute,
                               boolean groupRead,    boolean groupWrite,   boolean groupExecute,
                               boolean othersRead,   boolean othersWrite,  boolean othersExecute,
                               boolean setuid,       boolean setgid,       boolean sticky) {
        int mode = 0;

        // Special bits
        if (setuid)        mode |= 04000;
        if (setgid)        mode |= 02000;
        if (sticky)        mode |= 01000;

        // Owner bits
        if (ownerRead)     mode |= 0400;
        if (ownerWrite)    mode |= 0200;
        if (ownerExecute)  mode |= 0100;

        // Group bits
        if (groupRead)     mode |= 0040;
        if (groupWrite)    mode |= 0020;
        if (groupExecute)  mode |= 0010;

        // Others bits
        if (othersRead)    mode |= 0004;
        if (othersWrite)   mode |= 0002;
        if (othersExecute) mode |= 0001;

        return mode;
    }

    /**
     * Formats a POSIX mode word as a zero-padded four-digit octal string
     * (e.g., {@code 493} → {@code "0755"}, {@code 420} → {@code "0644"}).
     *
     * @param mode POSIX mode word in the range [0, 4095]
     * @return four-character octal string prefixed with {@code '0'}
     * @throws IllegalArgumentException if {@code mode} is outside [0, 4095]
     */
    public String formatOctalMode(int mode) {
        if (mode < 0 || mode > 4095) {
            throw new IllegalArgumentException(
                "mode must be in the range [0, 4095], got: " + mode);
        }
        return String.format("0%03o", mode);
    }
}