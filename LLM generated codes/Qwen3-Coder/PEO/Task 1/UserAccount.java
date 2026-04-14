public class UserAccount {
    private String username;
    private String passwordHash;
    private String emailAddress;
    private java.util.List<String> billingHistory;
    private byte[] profilePicture;
    private int yearsOfService;

    // Constructor
    public UserAccount(String username, String passwordHash, String emailAddress, 
                      java.util.List<String> billingHistory, byte[] profilePicture, int yearsOfService) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.emailAddress = emailAddress;
        this.billingHistory = billingHistory != null ? new java.util.ArrayList<>(billingHistory) : new java.util.ArrayList<>();
        this.profilePicture = profilePicture != null ? profilePicture.clone() : null;
        this.yearsOfService = yearsOfService;
    }

    // Getters
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmailAddress() { return emailAddress; }
    public java.util.List<String> getBillingHistory() { return new java.util.ArrayList<>(billingHistory); }
    public byte[] getProfilePicture() { return profilePicture != null ? profilePicture.clone() : null; }
    public int getYearsOfService() { return yearsOfService; }

    // Native method declaration
    public native boolean isEligibleForLegacyReward();

    // Static block to load the native library
    static {
        System.loadLibrary("legacy_reward"); // This will load liblegacy_reward.so (Linux/Mac) or legacy_reward.dll (Windows)
    }
}