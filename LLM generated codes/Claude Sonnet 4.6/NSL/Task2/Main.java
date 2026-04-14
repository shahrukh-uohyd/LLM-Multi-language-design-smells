/**
 * Demonstrates the JNI hardware-information pipeline.
 *
 * Build and run:
 *   ./build.sh run
 * or manually:
 *   javac -d out HardwareInfo.java Main.java
 *   javac -h native -d out HardwareInfo.java
 *   gcc -shared -fPIC -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
 *       -O2 -o native/build/libHardwareInfo.so native/HardwareInfo.c
 *   java -Djava.library.path=native/build -cp out Main
 */
public class Main {

    public static void main(String[] args) {

        HardwareInfo hw = new HardwareInfo();

        // ------------------------------------------------------------ //
        // Full snapshot (single JNI round-trip per field)               //
        // ------------------------------------------------------------ //
        HardwareInfo.HardwareSnapshot snap = hw.snapshot();
        System.out.println(snap);

        // ------------------------------------------------------------ //
        // Individual native calls                                        //
        // ------------------------------------------------------------ //
        System.out.println("\n--- Individual Field Access ---");
        System.out.println("CPU Model      : " + hw.getCpuModel());
        System.out.println("Physical Cores : " + hw.getCpuCores());
        System.out.println("Logical CPUs   : " + hw.getCpuLogicalProcessors());
        System.out.println("CPU Frequency  : " + hw.getCpuFrequencyMHz() + " MHz");
        System.out.println("Total RAM      : " + HardwareInfo.formatBytes(hw.getTotalMemoryBytes()));
        System.out.println("Free  RAM      : " + HardwareInfo.formatBytes(hw.getFreeMemoryBytes()));
        System.out.println("Used  RAM      : " + HardwareInfo.formatBytes(hw.getUsedMemoryBytes()));
        System.out.println("OS Name        : " + hw.getOsName());
        System.out.println("OS Version     : " + hw.getOsVersion());
        System.out.println("Architecture   : " + hw.getOsArchitecture());
        System.out.println("Total Disk     : " + HardwareInfo.formatBytes(hw.getTotalDiskBytes()));
        System.out.println("Free  Disk     : " + HardwareInfo.formatBytes(hw.getFreeDiskBytes()));
        System.out.println("Used  Disk     : " + HardwareInfo.formatBytes(hw.getUsedDiskBytes()));
        System.out.println("Hostname       : " + hw.getHostname());
        System.out.println("Uptime         : " + HardwareInfo.formatUptime(hw.getUptimeSeconds()));

        // ------------------------------------------------------------ //
        // Memory utilisation percentage                                  //
        // ------------------------------------------------------------ //
        long total = hw.getTotalMemoryBytes();
        long used  = hw.getUsedMemoryBytes();
        if (total > 0) {
            double pct = 100.0 * used / total;
            System.out.printf("%nMemory utilisation: %.1f%%%n", pct);
            if (pct > 90.0)
                System.out.println("WARNING: Memory usage is critically high!");
            else if (pct > 75.0)
                System.out.println("NOTICE : Memory usage is elevated.");
            else
                System.out.println("STATUS : Memory usage is normal.");
        }

        // ------------------------------------------------------------ //
        // Disk utilisation percentage                                    //
        // ------------------------------------------------------------ //
        long diskTotal = hw.getTotalDiskBytes();
        long diskUsed  = hw.getUsedDiskBytes();
        if (diskTotal > 0) {
            double pct = 100.0 * diskUsed / diskTotal;
            System.out.printf("Disk   utilisation: %.1f%%%n", pct);
            if (pct > 90.0)
                System.out.println("WARNING: Disk is almost full!");
            else if (pct > 75.0)
                System.out.println("NOTICE : Disk usage is elevated.");
            else
                System.out.println("STATUS : Disk usage is normal.");
        }
    }
}