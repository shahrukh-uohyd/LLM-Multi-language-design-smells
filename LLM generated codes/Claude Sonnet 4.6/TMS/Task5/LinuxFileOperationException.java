package com.linux.native_fileops;

/**
 * Checked exception thrown when any native Linux file operation fails.
 *
 * <p>Every native method in this package throws this exception so that
 * callers have a single, predictable type to handle at the boundary
 * between Java and the Linux kernel.  The {@code errnoCode} field carries
 * the raw {@code errno} value set by the failing syscall, enabling callers
 * to distinguish between, for example, {@code ENOENT} (2) and
 * {@code EPERM} (1) programmatically.</p>
 *
 * <p>Common {@code errno} values:</p>
 * <ul>
 *   <li>{@code 1  – EPERM}  : Operation not permitted</li>
 *   <li>{@code 2  – ENOENT} : No such file or directory</li>
 *   <li>{@code 13 – EACCES} : Permission denied</li>
 *   <li>{@code 17 – EEXIST} : File already exists</li>
 *   <li>{@code 22 – EINVAL} : Invalid argument</li>
 *   <li>{@code 61 – ENODATA}: No data available (no such xattr)</li>
 * </ul>
 */
public class LinuxFileOperationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * The raw {@code errno} value captured immediately after the failing
     * syscall, or {@code -1} if the error originated above the syscall layer.
     */
    private final int errnoCode;

    /**
     * Constructs a {@code LinuxFileOperationException} with a detail message
     * and the raw {@code errno} code from the failing syscall.
     *
     * @param message   human-readable description of the failure, including
     *                  the syscall name and the file path involved
     * @param errnoCode raw {@code errno} value set by the failing syscall,
     *                  or {@code -1} if not applicable
     */
    public LinuxFileOperationException(String message, int errnoCode) {
        super(message);
        this.errnoCode = errnoCode;
    }

    /**
     * Constructs a {@code LinuxFileOperationException} with a detail message,
     * an {@code errno} code, and an underlying cause.
     *
     * @param message   human-readable description of the failure
     * @param errnoCode raw {@code errno} value
     * @param cause     the original throwable from the native layer
     */
    public LinuxFileOperationException(String message, int errnoCode, Throwable cause) {
        super(message, cause);
        this.errnoCode = errnoCode;
    }

    /**
     * Returns the raw {@code errno} value captured from the failing syscall.
     *
     * @return {@code errno} code, or {@code -1} if not set
     */
    public int getErrnoCode() {
        return errnoCode;
    }

    @Override
    public String toString() {
        return String.format("LinuxFileOperationException{errno=%d, message='%s'}",
                errnoCode, getMessage());
    }
}