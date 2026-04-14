import java.util.List;

public class UserAccount {

    private String username;
    private String passwordHash;
    private String emailAddress;
    private List<String> billingHistory;
    private byte[] profilePicture;
    private int yearsOfService;

    static {
        System.loadLibrary("useraccount");
    }

    public UserAccount(String username,
                       String passwordHash,
                       String emailAddress,
                       List<String> billingHistory,
                       byte[] profilePicture,
                       int yearsOfService) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.emailAddress = emailAddress;
        this.billingHistory = billingHistory;
        this.profilePicture = profilePicture;
        this.yearsOfService = yearsOfService;
    }

    // Native eligibility check
    public native boolean isEligibleForLegacyReward();

    // Demo
    public static void main(String[] args) {
        UserAccount user = new UserAccount(
            "alice",
            "hash123",
            "alice@example.com",
            List.of("INV-001", "INV-002"),
            new byte[]{1, 2, 3},
            12
        );

        System.out.println(
            "Eligible for legacy reward: " +
            user.isEligibleForLegacyReward()
        );
    }
}
