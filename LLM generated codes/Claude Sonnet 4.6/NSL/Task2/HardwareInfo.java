/**
 * Java wrapper that exposes native hardware-information queries via JNI.
 *
 * Information categories:
 *   - CPU  : model name, physical cores, logical processors, base frequency
 *   - RAM  : total, free, and used memory in bytes
 *   - OS   : operating system name, version, architecture
 *   - Disk : total, free, and used bytes on the root filesystem
 *   - Host : hostname and CPU uptime (seconds since boot)
 */
public class HardwareInfo {

    // ------------------------------------------------------------------ //
    //  Load native library                                                 //
    // ------------------------------------------------------------------ //

    static {
        /*
         * libHardwareInfo.so (Linux/macOS) or HardwareInfo.dll (Windows)
         * must be on java.library.path.
         *
         * Run:
         *   java -Djava.library.path=./native/build -cp out Main
         */
        System.loadLibrary("HardwareInfo");
    }

    // ------------------------------------------------------------------ //
    //  Native method declarations                                          //
    // ------------------------------------------------------------------ //

    /** Returns CPU model name string (e.g. "Intel(R) Core(TM) i7-9750H"). */
    public native String getCpuModel();

    /** Returns the number of physical CPU cores. */
    public native int getCpuCores();

    /** Returns the number of logical processors (includes hyper-threading). */
    public native int getCpuLogicalProcessors();

    /** Returns the CPU base frequency in MHz. */
    public native long getCpuFrequencyMHz();

    /** Returns total installed RAM in bytes. */
    public native long getTotalMemoryBytes();

    /** Returns free (available) RAM in bytes. */
    public native long getFreeMemoryBytes();

    /** Returns the OS name (e.g. "Linux", "Windows", "Darwin"). */
    public native String getOsName();

    /** Returns the OS version string. */
    public native String getOsVersion();

    /** Returns the CPU architecture (e.g. "x86_64", "aarch64"). */
    public native String getOsArchitecture();

    /** Returns total disk space on the root filesystem in bytes. */
    public native long getTotalDiskBytes();

    /** Returns free disk space on the root filesystem in bytes. */
    public native long getFreeDiskBytes();

    /** Returns the machine hostname. */
    public native String getHostname();

    /** Returns seconds elapsed since the system last booted. */
    public native long getUptimeSeconds();

    // ------------------------------------------------------------------ //
    //  Derived helpers (pure Java)                                         //
    // ------------------------------------------------------------------ //

    /** Used RAM = total - free. */
    public long getUsedMemoryBytes() {
        return getTotalMemoryBytes() - getFreeMemoryBytes();
    }

    /** Used disk = total - free. */
    public long getUsedDiskBytes() {
        return getTotalDiskBytes() - getFreeDiskBytes();
    }

    /** Formats a byte count as a human-readable string (B / KB / MB / GB). */
    public static String formatBytes(long bytes) {
        if (bytes < 0)           return "N/A";
        if (bytes < 1024L)       return bytes + " B";
        if (bytes < 1024L * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /** Formats an uptime in seconds as "Xd Xh Xm Xs". */
    public static String formatUptime(long seconds) {
        if (seconds < 0) return "N/A";
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%dd %dh %dm %ds", d, h, m, s);
    }

    // ------------------------------------------------------------------ //
    //  Snapshot – collects all fields into one object                      //
    // ------------------------------------------------------------------ //

    /**
     * Captures all hardware data in a single call.
     * Avoids multiple round-trips across the JNI boundary.
     */
    public HardwareSnapshot snapshot() {
        return new HardwareSnapshot(
            getCpuModel(),
            getCpuCores(),
            getCpuLogicalProcessors(),
            getCpuFrequencyMHz(),
            getTotalMemoryBytes(),
            getFreeMemoryBytes(),
            getOsName(),
            getOsVersion(),
            getOsArchitecture(),
            getTotalDiskBytes(),
            getFreeDiskBytes(),
            getHostname(),
            getUptimeSeconds()
        );
    }

    // ------------------------------------------------------------------ //
    //  Snapshot value object                                               //
    // ------------------------------------------------------------------ //

    /** Immutable snapshot of all hardware information at a point in time. */
    public static class HardwareSnapshot {

        public final String cpuModel;
        public final int    cpuCores;
        public final int    cpuLogical;
        public final long   cpuFreqMHz;
        public final long   totalMemBytes;
        public final long   freeMemBytes;
        public final String osName;
        public final String osVersion;
        public final String osArch;
        public final long   totalDiskBytes;
        public final long   freeDiskBytes;
        public final String hostname;
        public final long   uptimeSeconds;

        public HardwareSnapshot(String cpuModel, int cpuCores, int cpuLogical,
                                long cpuFreqMHz,
                                long totalMemBytes, long freeMemBytes,
                                String osName, String osVersion, String osArch,
                                long totalDiskBytes, long freeDiskBytes,
                                String hostname, long uptimeSeconds) {
            this.cpuModel       = cpuModel;
            this.cpuCores       = cpuCores;
            this.cpuLogical     = cpuLogical;
            this.cpuFreqMHz     = cpuFreqMHz;
            this.totalMemBytes  = totalMemBytes;
            this.freeMemBytes   = freeMemBytes;
            this.osName         = osName;
            this.osVersion      = osVersion;
            this.osArch         = osArch;
            this.totalDiskBytes = totalDiskBytes;
            this.freeDiskBytes  = freeDiskBytes;
            this.hostname       = hostname;
            this.uptimeSeconds  = uptimeSeconds;
        }

        /** Pretty-print all fields. */
        @Override
        public String toString() {
            return String.format(
                "========================================%n" +
                "  Hardware Information Snapshot%n"         +
                "========================================%n" +
                "  Hostname       : %s%n"                   +
                "  Uptime         : %s%n"                   +
                "----------------------------------------%n" +
                "  CPU Model      : %s%n"                   +
                "  Physical Cores : %d%n"                   +
                "  Logical CPUs   : %d%n"                   +
                "  Base Frequency : %d MHz%n"               +
                "----------------------------------------%n" +
                "  Total RAM      : %s%n"                   +
                "  Free  RAM      : %s%n"                   +
                "  Used  RAM      : %s%n"                   +
                "----------------------------------------%n" +
                "  OS Name        : %s%n"                   +
                "  OS Version     : %s%n"                   +
                "  Architecture   : %s%n"                   +
                "----------------------------------------%n" +
                "  Total Disk     : %s%n"                   +
                "  Free  Disk     : %s%n"                   +
                "  Used  Disk     : %s%n"                   +
                "========================================",
                hostname,
                formatUptime(uptimeSeconds),
                cpuModel,
                cpuCores,
                cpuLogical,
                cpuFreqMHz,
                formatBytes(totalMemBytes),
                formatBytes(freeMemBytes),
                formatBytes(totalMemBytes - freeMemBytes),
                osName,
                osVersion,
                osArch,
                formatBytes(totalDiskBytes),
                formatBytes(freeDiskBytes),
                formatBytes(totalDiskBytes - freeDiskBytes)
            );
        }
    }
}