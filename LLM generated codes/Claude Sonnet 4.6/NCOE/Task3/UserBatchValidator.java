import java.util.ArrayList;
import java.util.List;

public class UserBatchValidator {

    // ------------------------------------------------------------------
    // Validation result constants — kept in Java so both sides share them
    // ------------------------------------------------------------------
    public static final int RESULT_OK               = 0;
    public static final int RESULT_INVALID_STATUS   = 1;
    public static final int RESULT_TOO_MANY_ATTEMPTS = 2;
    public static final int RESULT_NULL_USER        = 3;

    public static final int  MAX_LOGIN_ATTEMPTS     = 5;
    public static final int  BATCH_SIZE             = 50;

    static {
        System.loadLibrary("userbatchvalidator");
    }

    // ------------------------------------------------------------------
    // Native API
    // ------------------------------------------------------------------

    /**
     * One-time initialisation: resolves and caches the jfieldIDs for
     * User.userId, User.status, and User.loginAttempts on the C++ side.
     *
     * <p>Must be called ONCE before any batch is processed.
     * Passing {@code User.class} directly from Java avoids the
     * FindClass / classloader pitfall on background threads.</p>
     *
     * @param userClass        pass {@code User.class}
     * @param maxLoginAttempts the policy limit stored on the C++ side
     */
    public static native void initNativeCache(Class<?> userClass,
                                              int maxLoginAttempts);

    /**
     * Validates a single User object by reading all three fields.
     *
     * <p>Uses only cached jfieldIDs — zero per-call field lookups.</p>
     *
     * @param  user  the User instance to validate
     * @return       one of the RESULT_* constants defined in this class
     */
    public static native int validateUser(Object user);

    // ------------------------------------------------------------------
    // Batch processing call site
    // ------------------------------------------------------------------

    /**
     * Processes a batch of users, validates each one, and prints the result.
     *
     * @param batch the list of User objects to validate (expects BATCH_SIZE)
     */
    public static void processBatch(List<User> batch) {
        System.out.printf("--- Processing batch of %d users ---%n", batch.size());

        int okCount              = 0;
        int invalidStatusCount   = 0;
        int tooManyAttemptsCount = 0;
        int nullCount            = 0;

        for (int i = 0; i < batch.size(); i++) {
            User user = batch.get(i);

            // ✅ validateUser uses only cached field IDs internally —
            //    no GetFieldID call happens inside this loop
            int result = validateUser(user);

            String label;
            switch (result) {
                case RESULT_OK:
                    label = "OK";
                    okCount++;
                    break;
                case RESULT_INVALID_STATUS:
                    label = "INVALID_STATUS";
                    invalidStatusCount++;
                    break;
                case RESULT_TOO_MANY_ATTEMPTS:
                    label = "TOO_MANY_ATTEMPTS";
                    tooManyAttemptsCount++;
                    break;
                case RESULT_NULL_USER:
                    label = "NULL_USER";
                    nullCount++;
                    break;
                default:
                    label = "UNKNOWN(" + result + ")";
                    break;
            }

            System.out.printf("  [%2d] %-15s | %s%n",
                    i,
                    label,
                    user == null ? "null" : user.toString());
        }

        System.out.println("--- Batch summary ---");
        System.out.printf("  OK: %d | INVALID_STATUS: %d | "
                        + "TOO_MANY_ATTEMPTS: %d | NULL: %d%n",
                okCount, invalidStatusCount,
                tooManyAttemptsCount, nullCount);
    }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        // ✅ Step 1 — Cache all three field IDs once before any batch runs
        initNativeCache(User.class, MAX_LOGIN_ATTEMPTS);

        // ✅ Step 2 — Build a batch of exactly 50 users
        List<User> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < BATCH_SIZE; i++) {
            if (i == 49) {
                // Edge case: one null entry to verify null-safety
                batch.add(null);
            } else if (i % 10 == 0) {
                // Every 10th user has an unrecognised status
                batch.add(new User("user-" + i, "UNKNOWN", i % 8));
            } else if (i % 7 == 0) {
                // Every 7th user has exceeded the login attempt limit
                batch.add(new User("user-" + i, "ACTIVE", MAX_LOGIN_ATTEMPTS + 1));
            } else {
                batch.add(new User("user-" + i, "ACTIVE", i % 5));
            }
        }

        // ✅ Step 3 — Process the batch (loop lives here, not inside native code)
        processBatch(batch);
    }
}