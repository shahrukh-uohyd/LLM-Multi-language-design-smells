/**
 * Native cryptographically secure random number generator
 */
public class SecureRandomGenerator {
    
    static {
        System.loadLibrary("secure_random_native");
    }
    
    // Native method to generate secure random bytes
    private native byte[] nativeGenerateSecureRandomBytes(int length);
    
    // Native method to generate secure random integer
    private native int nativeGenerateSecureRandomInt();
    
    // Native method to generate secure random long
    private native long nativeGenerateSecureRandomLong();
    
    /**
     * Generate cryptographically secure random bytes
     * @param length Number of random bytes to generate
     * @return Random byte array of specified length
     * @throws IllegalArgumentException if length is negative
     */
    public byte[] generateSecureRandomBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative");
        }
        return nativeGenerateSecureRandomBytes(length);
    }
    
    /**
     * Generate cryptographically secure random integer
     * @return Random integer value
     */
    public int generateSecureRandomInt() {
        return nativeGenerateSecureRandomInt();
    }
    
    /**
     * Generate cryptographically secure random long
     * @return Random long value
     */
    public long generateSecureRandomLong() {
        return nativeGenerateSecureRandomLong();
    }
    
    /**
     * Generate secure random integer within a range
     * @param min Minimum value (inclusive)
     * @param max Maximum value (exclusive)
     * @return Random integer in the specified range
     * @throws IllegalArgumentException if min >= max
     */
    public int generateSecureRandomInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Min value must be less than max value");
        }
        int randomInt = nativeGenerateSecureRandomInt();
        return min + (Math.abs(randomInt) % (max - min));
    }
}