/**
 * InfotainmentMain
 *
 * Demonstrates all four modules working together using a single
 * shared InfotainmentBridge instance.
 */
public class InfotainmentMain {

    public static void main(String[] args) {

        // One bridge instance — one native library load.
        InfotainmentBridge bridge = new InfotainmentBridge();

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  Automotive Infotainment System Boot  ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // ── 1. DashboardDisplay ──────────────────────────────────────
        System.out.println("── DashboardDisplay ──");
        DashboardDisplay dashboard = new DashboardDisplay(bridge);
        dashboard.refreshAll();   // reads fuel (sensor 0) + temp (sensor 1)
        System.out.println();

        // ── 2. MediaCenter ───────────────────────────────────────────
        System.out.println("── MediaCenter ──");
        MediaCenter media = new MediaCenter(bridge);
        // Open a stream and apply the bass-boost preset in one call.
        media.startStreamWithPreset(
                "file:///sdcard/music/journey.mp3",
                75,
                MediaCenter.EQ_PRESET_BASS_BOOST);
        System.out.println();

        // ── 3. NavigationModule ──────────────────────────────────────
        System.out.println("── NavigationModule ──");
        NavigationModule nav = new NavigationModule(bridge);
        // Stuttgart HQ → Munich — eco routing + tile pre-fetch.
        nav.startNavigationSession(
                48.7758, 9.1829,   // Stuttgart
                48.1351, 11.5820,  // Munich
                8597, 5716, 13);   // tile coords at zoom 13
        System.out.println();

        // ── 4. ConnectivitySuite ─────────────────────────────────────
        System.out.println("── ConnectivitySuite ──");
        ConnectivitySuite connectivity = new ConnectivitySuite(bridge);
        // SSP-pair phone, then find nearby fuel stations.
        connectivity.pairAndSearchFuelStations(
                "DE:AD:BE:EF:CA:FE",
                48.7758, 9.1829);  // Stuttgart coordinates
        System.out.println();

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║         Boot Sequence Complete        ║");
        System.out.println("╚══════════════════════════════════════╝");
    }
}