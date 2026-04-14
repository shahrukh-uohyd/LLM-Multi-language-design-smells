package com.hardware.native_utils;

/**
 * Provides a native utility for securely clearing raw memory regions.
 *
 * <p>Java's garbage collector does not guarantee immediate zeroing of
 * sensitive data (e.g., cryptographic keys, passwords, private keys).
 * This class exposes a single native method that performs a guaranteed,
 * compiler-optimisation-resistant {@code memset} (or equivalent secure-erase
 * intrinsic) on a specified off-heap address range.</p>
 *
 * <p><strong>Safety contract:</strong> The caller is entirely responsible for
 * ensuring that {@code address} points to a valid, writable, off-heap memory
 * region of exactly {@code length} bytes.  Passing an invalid address or an
 * incorrect length will result in undefined behaviour at the native level.</p>
 *
 * <p>Usage:
 * <pre>{@code
 *   // Typically used alongside java.nio.ByteBuffer.allocateDirect()
 *   ByteBuffer secretBuffer = ByteBuffer.allocateDirect(256);
 *   long address = ((DirectBuffer) secretBuffer).address();
 *
 *   // ... use secretBuffer for sensitive operations ...
 *
 *   NativeMemoryCleaner cleaner = new NativeMemoryCleaner();
 *   cleaner.clearMemory(address, secretBuffer.capacity());
 * }</pre>
 * </p>
 */
public class NativeMemoryCleaner {

    static {
        // Loads libnative_memory_cleaner.so (Linux/macOS) or native_memory_cleaner.dll (Windows)
        System.loadLibrary("native_memory_cleaner");
    }

    /**
     * Securely zeroes {@code length} bytes of memory starting at {@code address}.
     *
     * <p>The native implementation uses a volatile write or platform-specific
     * secure-erase intrinsic (e.g., {@code explicit_bzero} on Linux,
     * {@code SecureZeroMemory} on Windows) to prevent the compiler or CPU
     * from optimising away the zeroing operation.</p>
     *
     * @param address the starting address of the memory region to clear;
     *                must be a valid, writable, off-heap pointer
     * @param length  the number of bytes to zero out; must be &gt; 0
     * @throws IllegalArgumentException if {@code length} is zero or negative
     */
    public native void clearMemory(long address, int length);

    // -----------------------------------------------------------------------
    // Convenience wrapper
    // -----------------------------------------------------------------------

    /**
     * Validates parameters before delegating to the native {@link #clearMemory} call.
     *
     * @param address the memory address to clear
     * @param length  the number of bytes to zero; must be positive
     * @throws IllegalArgumentException if {@code length} &le; 0
     */
    public void secureClear(long address, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException(
                "length must be a positive integer, got: " + length);
        }
        clearMemory(address, length);
    }
}