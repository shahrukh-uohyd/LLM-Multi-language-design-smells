public class HardwareInfoApp {

    public static void main(String[] args) {

        int cores = NativeHardwareInfo.getCpuCoreCount();
        long memoryMB = NativeHardwareInfo.getTotalMemoryMB();

        System.out.println("Hardware Information:");
        System.out.println("CPU cores: " + cores);
        System.out.println("Total memory: " + memoryMB + " MB");

        // Example processing
        if (cores >= 8) {
            System.out.println("High-core system detected.");
        }
    }
}
