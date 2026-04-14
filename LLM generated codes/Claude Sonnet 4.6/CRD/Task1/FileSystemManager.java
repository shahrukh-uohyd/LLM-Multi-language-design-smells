package com.app.filesystem;

/**
 * Manages file-system–related operations and exposes native methods
 * for low-level file access through JNI.
 *
 * <p>This class is NOT significantly modified — shown as architectural
 * context only. The native library it loads ({@code filesystem_manager})
 * is fully independent from the new crypto library.</p>
 */
public class FileSystemManager {

    static {
        System.loadLibrary("filesystem_manager"); // existing — unchanged
    }

    // ── existing native declarations (all unchanged) ──────────────────

    /** Opens a file at the given path with the specified flags. Returns a native file descriptor. */
    public native int openFile(String path, int flags);

    /** Closes a previously opened native file descriptor. */
    public native void closeFile(int fileDescriptor);

    /** Reads up to {@code length} bytes from {@code fd} into {@code buffer} at {@code offset}. */
    public native int readFile(int fileDescriptor, byte[] buffer, int offset, int length);

    /** Writes {@code length} bytes from {@code buffer} starting at {@code offset} to {@code fd}. */
    public native int writeFile(int fileDescriptor, byte[] buffer, int offset, int length);

    /** Returns the byte size of the file at the given path, or -1 if inaccessible. */
    public native long getFileSize(String path);

    /** Returns {@code true} if a file or directory exists at the given path. */
    public native boolean fileExists(String path);

    /** Deletes the file at the given path. Returns {@code true} on success. */
    public native boolean deleteFile(String path);

    /** Creates all directories in the given path. Returns {@code true} on success. */
    public native boolean createDirectories(String path);
}