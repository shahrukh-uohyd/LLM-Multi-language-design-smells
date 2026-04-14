/**
 * Native memory clearing utility for secure memory management
 */
public class NativeMemoryClearer {
    
    static {
        System.loadLibrary("memory_clear_native");
    }
    
    // Native method to securely clear memory buffer
    private native void clearMemoryBuffer(byte[] buffer);
    
    // Native method to securely clear memory buffer with specific pattern
    private native void clearMemoryBufferWithPattern(byte[] buffer, byte pattern);
    
    // Native method to securely clear char array
    private native void clearCharArray(char[] array);
    
    // Native method to securely clear integer array
    private native void clearIntArray(int[] array);
    
    // Native method to securely clear long array
    private native void clearLongArray(long[] array);
    
    /**
     * Securely clear a byte array by overwriting with zeros
     * @param buffer The byte array to clear
     * @throws IllegalArgumentException if buffer is null
     */
    public void clearBytes(byte[] buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        clearMemoryBuffer(buffer);
    }
    
    /**
     * Securely clear a byte array by overwriting with specified pattern
     * @param buffer The byte array to clear
     * @param pattern The byte value to fill the array with
     * @throws IllegalArgumentException if buffer is null
     */
    public void clearBytesWithPattern(byte[] buffer, byte pattern) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer cannot be null");
        }
        clearMemoryBufferWithPattern(buffer, pattern);
    }
    
    /**
     * Securely clear a character array
     * @param array The character array to clear
     * @throws IllegalArgumentException if array is null
     */
    public void clearChars(char[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }
        clearCharArray(array);
    }
    
    /**
     * Securely clear an integer array
     * @param array The integer array to clear
     * @throws IllegalArgumentException if array is null
     */
    public void clearInts(int[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }
        clearIntArray(array);
    }
    
    /**
     * Securely clear a long array
     * @param array The long array to clear
     * @throws IllegalArgumentException if array is null
     */
    public void clearLongs(long[] array) {
        if (array == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }
        clearLongArray(array);
    }
}