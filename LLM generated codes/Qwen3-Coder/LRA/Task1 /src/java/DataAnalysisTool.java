// src/java/DataAnalysisTool.java
public class DataAnalysisTool {
    static {
        try {
            System.loadLibrary("dataanalysis"); // Load native library
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native library failed to load: " + e.getMessage());
        }
    }
    
    // Native method declarations
    public native int processUserRecords(UserRecord[] userRecords);
    public native long processUserRecordsOptimized(UserRecord[] userRecords);
    public native int[] processUserRecordsWithStats(UserRecord[] userRecords);
    
    // Java wrapper methods
    public int calculateTotalScore(UserRecord[] records) {
        return processUserRecords(records);
    }
    
    public int calculateTotalId(UserRecord[] records) {
        // Modify native code to extract ID instead of score if needed
        return processUserRecords(records);
    }
    
    public static void main(String[] args) {
        // Example usage
        DataAnalysisTool tool = new DataAnalysisTool();
        
        // Create sample data (100 elements as mentioned)
        UserRecord[] records = new UserRecord[100];
        for (int i = 0; i < 100; i++) {
            records[i] = new UserRecord(i + 1, "User" + (i + 1), (i + 1) * 10, 20 + (i % 50));
        }
        
        int totalScore = tool.calculateTotalScore(records);
        System.out.println("Total Score: " + totalScore);
        
        long optimizedResult = tool.processUserRecordsOptimized(records);
        System.out.println("Optimized Result: " + optimizedResult);
    }
}