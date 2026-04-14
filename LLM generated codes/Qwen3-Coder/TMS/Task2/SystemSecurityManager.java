/**
 * Combined security manager that uses all three native features
 */
public class SystemSecurityManager {
    
    private final HighSpeedChecksumCalculator checksumCalculator;
    private final NativeMemoryClearer memoryClearer;
    private final HardwareIdRetriever hardwareIdRetriever;
    
    public SystemSecurityManager() {
        this.checksumCalculator = new HighSpeedChecksumCalculator();
        this.memoryClearer = new NativeMemoryClearer();
        this.hardwareIdRetriever = new HardwareIdRetriever();
    }
    
    /**
     * Verify file integrity using high-speed checksum
     * @param filePath Path to the file to verify
     * @param expectedChecksum Expected checksum value
     * @return true if checksum matches, false otherwise
     */
    public boolean verifyFileIntegrity(String filePath, String expectedChecksum) {
        String calculatedChecksum = checksumCalculator.getFileChecksum(filePath);
        return calculatedChecksum.equals(expectedChecksum);
    }
    
    /**
     * Securely clear sensitive data from memory
     * @param sensitiveData Data to be securely cleared
     */
    public void clearSensitiveData(byte[] sensitiveData) {
        memoryClearer.clearBytes(sensitiveData);
    }
    
    /**
     * Get the hardware ID for system identification
     * @return Hardware ID string
     */
    public String getSystemHardwareId() {
        return hardwareIdRetriever.getHardwareId();
    }
    
    /**
     * Verify system integrity by comparing hardware ID
     * @param storedHardwareId Previously stored hardware ID
     * @return true if hardware IDs match, false otherwise
     */
    public boolean verifySystemIntegrity(String storedHardwareId) {
        String currentHardwareId = hardwareIdRetriever.getHardwareId();
        return currentHardwareId.equals(storedHardwareId);
    }
}