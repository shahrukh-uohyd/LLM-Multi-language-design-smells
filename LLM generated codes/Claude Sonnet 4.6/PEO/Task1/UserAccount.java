import java.util.List;

public class UserAccount {

    private String username;
    private String passwordHash;       // sensitive
    private String emailAddress;       // sensitive
    private List<String> billingHistory; // sensitive
    private byte[] profilePicture;     // sensitive
    private int yearsOfService;        // the ONLY field needed for the reward check

    public UserAccount(String username, String passwordHash, String emailAddress,
                       List<String> billingHistory, byte[] profilePicture, int yearsOfService) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.emailAddress = emailAddress;
        this.billingHistory = billingHistory;
        this.profilePicture = profilePicture;
        this.yearsOfService = yearsOfService;
    }

    // Native method declaration
    // Takes the full object but the C implementation MUST only read yearsOfService
    public native boolean isEligibleForLegacyReward();

    // Alternatively (RECOMMENDED - see notes below), pass only what's needed:
    public native boolean isEligibleForLegacyRewardSecure(int yearsOfService);

    static {
        System.loadLibrary("legacy_reward"); // loads libliblegacy_reward.so / legacy_reward.dll
    }
}