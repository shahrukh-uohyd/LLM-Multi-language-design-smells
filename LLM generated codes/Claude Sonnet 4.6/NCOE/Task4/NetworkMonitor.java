import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NetworkMonitor {

    static {
        // Load the shared native library before any method in this class runs.
        System.loadLibrary("networkmonitor");
    }

    // ------------------------------------------------------------------
    // Native API
    // ------------------------------------------------------------------

    /**
     * One-time initialisation: resolves and caches the jfieldID for
     * NetworkPacket.timestamp on the C++ side.
     *
     * <p>Must be called ONCE on the main thread, before:
     * <ul>
     *   <li>any {@link NetworkPacket} is constructed (call site 1), and</li>
     *   <li>the background listener is started (call site 2).</li>
     * </ul>
     *
     * <p>Passing {@code NetworkPacket.class} directly sidesteps the
     * {@code FindClass} / bootstrap-classloader pitfall that would cause
     * a silent {@code null} return on background threads.
     *
     * @param packetClass pass {@code NetworkPacket.class} explicitly
     */
    public static native void initNativeCache(Class<?> packetClass);

    /**
     * Writes the current system time (milliseconds since the Unix epoch)
     * into the {@code timestamp} field of {@code packet}.
     *
     * <p>Uses only the cached jfieldID — zero per-call field lookups.
     * Safe to call from any thread.
     *
     * @param packet the {@link NetworkPacket} instance to stamp
     */
    public static native void recordTimestamp(Object packet);

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) throws InterruptedException {

        // ✅ Step 1 — Cache the field ID on the main thread, before anything else.
        //    This is the ONLY point at which FindClass-equivalent resolution
        //    happens, and it happens safely here on the main thread.
        initNativeCache(NetworkPacket.class);

        // ✅ Step 2 — Construct packets (call site 1 fires inside each constructor).
        NetworkPacket p1 = new NetworkPacket("192.168.1.10", 512);
        NetworkPacket p2 = new NetworkPacket("10.0.0.5",     1024);

        System.out.println("After construction:");
        System.out.println("  " + p1);
        System.out.println("  " + p2);

        // ✅ Step 3 — Start background listener AFTER initNativeCache has run.
        //    The refresh() calls on this thread are safe because the field ID
        //    was already resolved and cached on the main thread in Step 1.
        ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "packet-refresh-listener");
                t.setDaemon(true);
                return t;
            });

        // Simulate the background listener refreshing packets every 500 ms.
        scheduler.scheduleAtFixedRate(() -> {
            // ✅ Call site 2: refresh() → recordTimestamp() on background thread.
            p1.refresh();
            p2.refresh();
            System.out.println("[listener] After refresh:");
            System.out.println("  " + p1);
            System.out.println("  " + p2);
        }, 0, 500, TimeUnit.MILLISECONDS);

        // Let the listener run for 2 seconds then shut down.
        Thread.sleep(2_000);
        scheduler.shutdown();
        System.out.println("Monitor stopped.");
    }
}