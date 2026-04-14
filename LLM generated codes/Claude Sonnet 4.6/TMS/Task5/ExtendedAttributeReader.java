package com.linux.native_fileops;

/**
 * Provides native bindings for reading extended attributes (xattrs) from
 * Linux file system objects.
 *
 * <p>Extended attributes are key-value pairs attached to inodes by the
 * kernel or user-space tools.  Common namespaces include:</p>
 * <ul>
 *   <li>{@code user.*}     – application-defined metadata</li>
 *   <li>{@code security.*} – SELinux labels, capabilities</li>
 *   <li>{@code trusted.*}  – kernel-only privileged attributes</li>
 *   <li>{@code system.*}   – POSIX ACL entries</li>
 * </ul>
 *
 * <p>The native layer calls the Linux syscalls {@code listxattr(2)} and
 * {@code getxattr(2)} directly, bypassing the JVM's file abstraction
 * layer which has no support for xattrs.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   ExtendedAttributeReader reader = new ExtendedAttributeReader();
 *   String[] names = reader.listAttributeNames("/var/data/report.pdf");
 *   for (String name : names) {
 *       byte[] value = reader.readAttribute("/var/data/report.pdf", name);
 *       System.out.println(name + " = " + new String(value, StandardCharsets.UTF_8));
 *   }
 * }</pre>
 *
 * <p><strong>Platform requirement:</strong> This class is Linux-only.
 * The native library will throw {@link LinuxFileOperationException} on
 * any other OS.</p>
 *
 * <p><strong>Thread safety:</strong> All methods are stateless with respect
 * to instance fields; the class is safe to use from multiple threads
 * provided the underlying file is not concurrently modified.</p>
 */
public class ExtendedAttributeReader {

    static {
        // Loads libextended_attr.so — Linux only
        System.loadLibrary("extended_attr");
    }

    // ------------------------------------------------------------------
    // Native method declarations
    // ------------------------------------------------------------------

    /**
     * Lists the names of all extended attributes attached to the file at
     * {@code filePath}.
     *
     * <p>Internally calls {@code listxattr(2)}.  The returned array
     * contains fully-qualified attribute names including their namespace
     * prefix (e.g., {@code "user.author"}, {@code "security.selinux"}).
     * An empty array is returned when the file has no extended attributes.</p>
     *
     * @param filePath absolute or relative path to the target file or
     *                 directory; symlinks are followed automatically;
     *                 must not be {@code null} or empty
     * @return a non-null, possibly empty array of xattr name strings
     * @throws LinuxFileOperationException if {@code listxattr(2)} fails
     *         (e.g., {@code ENOENT} if the path does not exist,
     *         {@code EACCES} if the caller lacks search permission on a
     *         parent directory component)
     * @throws IllegalArgumentException    if {@code filePath} is
     *                                     {@code null} or blank
     */
    public native String[] listAttributeNames(String filePath)
            throws LinuxFileOperationException;

    /**
     * Reads the raw byte value of a single named extended attribute from
     * the file at {@code filePath}.
     *
     * <p>Internally calls {@code getxattr(2)}.  The returned array is a
     * verbatim copy of the attribute value as stored by the kernel — no
     * charset conversion is performed.  The caller is responsible for
     * interpreting the bytes (e.g., as UTF-8 text, a binary blob, or a
     * structured data format).</p>
     *
     * @param filePath      path to the target file; must not be
     *                      {@code null} or blank
     * @param attributeName fully-qualified xattr name including namespace
     *                      prefix (e.g., {@code "user.checksum"});
     *                      must not be {@code null} or blank
     * @return a non-null byte array containing the raw attribute value
     * @throws LinuxFileOperationException if {@code getxattr(2)} fails
     *         (e.g., {@code ENODATA} / errno 61 if the named attribute
     *         does not exist, {@code EACCES} for permission errors)
     * @throws IllegalArgumentException    if either argument is
     *                                     {@code null} or blank
     */
    public native byte[] readAttribute(String filePath, String attributeName)
            throws LinuxFileOperationException;

    // ------------------------------------------------------------------
    // Convenience wrappers (pure Java — no JNI overhead)
    // ------------------------------------------------------------------

    /**
     * Reads the value of a named xattr and decodes it as a UTF-8 string.
     *
     * <p>This is a pure-Java convenience wrapper over
     * {@link #readAttribute(String, String)} for the common case where
     * the attribute value is known to be UTF-8 text.</p>
     *
     * @param filePath      path to the target file
     * @param attributeName fully-qualified xattr name
     * @return the attribute value decoded as a UTF-8 string
     * @throws LinuxFileOperationException if the underlying native call fails
     */
    public String readAttributeAsString(String filePath, String attributeName)
            throws LinuxFileOperationException {
        byte[] raw = readAttribute(filePath, attributeName);
        return new String(raw, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Returns {@code true} if the file at {@code filePath} has at least
     * one extended attribute whose name starts with the given
     * {@code namespacePrefix} (e.g., {@code "user."}, {@code "security."}).
     *
     * @param filePath        path to the target file
     * @param namespacePrefix xattr namespace prefix to filter on
     * @return {@code true} if at least one matching xattr exists
     * @throws LinuxFileOperationException if {@link #listAttributeNames}
     *                                     fails
     * @throws IllegalArgumentException    if either argument is null or blank
     */
    public boolean hasAttributesInNamespace(String filePath, String namespacePrefix)
            throws LinuxFileOperationException {
        if (namespacePrefix == null || namespacePrefix.isBlank()) {
            throw new IllegalArgumentException("namespacePrefix must not be null or blank.");
        }
        for (String name : listAttributeNames(filePath)) {
            if (name.startsWith(namespacePrefix)) {
                return true;
            }
        }
        return false;
    }
}