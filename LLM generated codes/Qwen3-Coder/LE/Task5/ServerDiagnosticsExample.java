/**
 * Example usage of the server diagnostics system health reporting
 */
public class ServerDiagnosticsExample {
    public static void main(String[] args) {
        ServerDiagnostics diagnostics = new ServerDiagnostics();
        
        // Generate multiple health reports to see different scenarios
        System.out.println("=== Server Health Diagnostics ===\n");
        
        // Generate initial report
        SystemHealthReport report1 = diagnostics.generateSystemHealthReport();
        System.out.println(report1.toString());
        System.out.println();
        
        // Wait a bit before next report
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Generate second report
        SystemHealthReport report2 = diagnostics.generateSystemHealthReport();
        System.out.println(report2.toString());
        System.out.println();
        
        // Wait again
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Generate third report
        SystemHealthReport report3 = diagnostics.generateSystemHealthReport();
        System.out.println(report3.toString());
        System.out.println();
        
        // Print summary
        System.out.println("=== Summary ===");
        System.out.println("Report 1 Status: " + report1.getLoadStatus().getDisplayName());
        System.out.println("Report 2 Status: " + report2.getLoadStatus().getDisplayName());
        System.out.println("Report 3 Status: " + report3.getLoadStatus().getDisplayName());
        
        // Performance metrics
        System.out.println("\nPerformance Metrics:");
        System.out.println("Report 1 Generation Time: " + report1.getReportGenerationTime() + " nanoseconds");
        System.out.println("Report 2 Generation Time: " + report2.getReportGenerationTime() + " nanoseconds");
        System.out.println("Report 3 Generation Time: " + report3.getReportGenerationTime() + " nanoseconds");
    }
}