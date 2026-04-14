public class NativeSystemUtilities {

    // Load the shared C/C++ library containing the implementations for all these utilities
    static {
        // Loads system_utils_native.dll (Windows), libsystem_utils_native.so (Linux), etc.
        System.loadLibrary("system_utils_native");
    }

    // ---------------------------------------------------------
    // 1. High-Speed File Checksum Calculator
    // ---------------------------------------------------------
    public static class FileChecksum {
        /**
         * Computes a checksum (e.g., CRC32C, BLAKE3, or SHA-256) entirely in native code.
         * This avoids copying large file buffers into the JVM heap and allows the 
         * C/C++ backend to utilize hardware-accelerated SIMD instructions.
         *
         * @param filePath The absolute path to the file on disk.
         * @return A byte array representing the computed checksum/hash.
         */
        public native byte[] calculateChecksum(String filePath);
    }

    // ---------------------------------------------------------
    // 2. Native Memory-Clearing Utility
    // ---------------------------------------------------------
    public static class MemoryUtility {
        /**
         * Securely overwrites a block of native memory with zeros or random data.
         * This is typically used to clear highly sensitive data (like cryptographic keys 
         * or passwords) directly in RAM, wrapping OS-specific calls like SecureZeroMemory 
         * (Windows) or explicit_bzero (Linux).
         *
         * @param memoryAddress The starting memory address (pointer) cast to a long.
         * @param lengthInBytes The number of bytes to clear.
         */
        public native void secureClearMemory(long memoryAddress, long lengthInBytes);
    }

    // ---------------------------------------------------------
    // 3. Hardware ID Retriever
    // ---------------------------------------------------------
    public static class HardwareInfo {
        /**
         * Retrieves a unique, persistent hardware identifier for the current machine.
         * The native backend typically queries low-level SMBIOS tables, motherboard 
         * serial numbers, or CPU IDs via OS-specific APIs (e.g., WMI on Windows).
         *
         * @return A String containing the unique hardware ID.
         */
        public native String getHardwareId();
    }

    // ---------------------------------------------------------
    // Example Usage
    // ---------------------------------------------------------
    public static void main(String[] args) {
        // 1. Checksum Example
        FileChecksum checksumUtil = new FileChecksum();
        byte[] hash = checksumUtil.calculateChecksum("/path/to/large/video.mp4");
        System.out.println("Checksum computed: " + hash.length + " bytes.");

        // 2. Memory Clearing Example
        MemoryUtility memUtil = new MemoryUtility();
        long fakePointerAddress = 0x7FFA23456789L; // Example memory address
        long sizeToClear = 256; // 256 bytes
        memUtil.secureClearMemory(fakePointerAddress, sizeToClear);
        System.out.println("Memory securely cleared at address: " + Long.toHexString(fakePointerAddress));

        // 3. Hardware ID Example
        HardwareInfo hwInfo = new HardwareInfo();
        String uniqueId = hwInfo.getHardwareId();
        System.out.println("Machine Hardware ID: " + uniqueId);
    }
}