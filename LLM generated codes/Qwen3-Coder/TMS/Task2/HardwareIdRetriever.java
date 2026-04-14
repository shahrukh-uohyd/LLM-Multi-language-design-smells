/**
 * Utility to retrieve unique hardware ID of the machine
 */
public class HardwareIdRetriever {
    
    static {
        System.loadLibrary("hardware_id_native");
    }
    
    // Native method to retrieve unique hardware ID
    private native String getUniqueHardwareId();
    
    // Native method to retrieve detailed hardware information
    private native String getDetailedHardwareInfo();
    
    // Native method to check if system has valid hardware ID
    private native boolean hasValidHardwareId();
    
    /**
     * Get the unique hardware ID of the current machine
     * @return Unique hardware identifier string
     * @throws RuntimeException if hardware ID cannot be retrieved
     */
    public String getHardwareId() {
        String hwId = getUniqueHardwareId();
        if (hwId == null) {
            throw new RuntimeException("Could not retrieve hardware ID");
        }
        return hwId;
    }
    
    /**
     * Get detailed hardware information including multiple identifiers
     * @return Detailed hardware information string
     */
    public String getHardwareDetails() {
        return getDetailedHardwareInfo();
    }
    
    /**
     * Check if the system has a valid hardware ID available
     * @return true if valid hardware ID exists, false otherwise
     */
    public boolean isValidHardwareIdAvailable() {
        return hasValidHardwareId();
    }
    
    /**
     * Get a formatted hardware ID suitable for licensing or identification
     * @return Formatted hardware ID string
     */
    public String getFormattedHardwareId() {
        String rawId = getHardwareId();
        // Add any formatting as needed
        return formatHardwareId(rawId);
    }
    
    /**
     * Format the hardware ID for display or storage
     * @param rawId The raw hardware ID string
     * @return Formatted hardware ID
     */
    private String formatHardwareId(String rawId) {
        // Example formatting: add hyphens every 8 characters
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < rawId.length(); i++) {
            if (i > 0 && i % 8 == 0) {
                formatted.append('-');
            }
            formatted.append(rawId.charAt(i));
        }
        return formatted.toString().toUpperCase();
    }
}