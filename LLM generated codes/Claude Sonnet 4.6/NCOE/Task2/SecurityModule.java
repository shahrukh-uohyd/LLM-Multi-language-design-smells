public class SecurityModule {

    private static final int BIO_MAX_LENGTH = 100;

    static {
        System.loadLibrary("securitymodule");
    }

    /**
     * One-time JVM startup hook: resolves and caches the jfieldID for
     * UserProfile.bio so the sanitizer never pays a per-call lookup cost.
     *
     * Must be called once before any call to sanitizeUserProfile().
     *
     * @param userProfileClass  pass UserProfile.class directly from Java —
     *                          this avoids the FindClass/classloader pitfall
     *                          on background threads.
     * @param maxBioLength      the maximum permitted bio length; stored on
     *                          the C side so policy lives in one place.
     */
    public static native void initNativeCache(Class<?> userProfileClass, int maxBioLength);

    /**
     * Sanitizes the bio field of the given UserProfile in-place.
     * If bio exceeds the configured maximum length it is truncated.
     *
     * Uses a single cached jfieldID for both the read (length check)
     * and the conditional write (truncation) — no redundant lookups.
     *
     * @param  profile  the UserProfile instance to sanitize
     * @return          true if the bio was truncated, false if it was clean
     */
    public static native boolean sanitizeUserProfile(Object profile);


    // -----------------------------------------------------------------------
    // Call site
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        // ✅ Cache field ID and policy once at startup — not inside the loop
        initNativeCache(UserProfile.class, BIO_MAX_LENGTH);

        UserProfile[] profiles = {
            new UserProfile("Short bio."),
            new UserProfile("A".repeat(150)),   // 150 chars — must be truncated
            new UserProfile("B".repeat(100)),   // exactly 100 chars — must pass
            new UserProfile(null),              // null bio — must be handled safely
        };

        for (UserProfile profile : profiles) {
            boolean wasTruncated = sanitizeUserProfile(profile);
            System.out.printf("truncated=%-5b | bio length=%s | bio='%s'%n",
                wasTruncated,
                profile.bio == null ? "null" : String.valueOf(profile.bio.length()),
                profile.bio == null ? "null" : profile.bio.substring(0, Math.min(30, profile.bio.length())) + "...");
        }
    }
}