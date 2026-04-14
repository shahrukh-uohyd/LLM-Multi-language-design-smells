public class SystemDiagnostics {

    static {
        System.loadLibrary("sysdiag");
    }

    // ----- Existing native hardware access -----

    // CPU temperature in Celsius
    public native double nativeReadCpuTemperature();

    // Total active hardware interrupts
    public native long nativeGetInterruptCount();

    // Power consumption in watts
    public native double nativeGetPowerConsumption();

    // ----- New SystemHealthReport feature -----

    public SystemHealthReport generateReport() {
        double temperature = nativeReadCpuTemperature();
        long interrupts = nativeGetInterruptCount();
        double power = nativeGetPowerConsumption();

        LoadStatus status;
        if (temperature > 85.0 || power > 150.0 || interrupts > 1_000_000) {
            status = LoadStatus.CRITICAL;
        } else if (temperature > 70.0 || power > 100.0 || interrupts > 500_000) {
            status = LoadStatus.HIGH;
        } else {
            status = LoadStatus.NORMAL;
        }

        return new SystemHealthReport(
            temperature,
            interrupts,
            power,
            status
        );
    }

    // Demo
    public static void main(String[] args) {
        SystemDiagnostics diag = new SystemDiagnostics();
        SystemHealthReport report = diag.generateReport();
        System.out.println(report);
    }
}
