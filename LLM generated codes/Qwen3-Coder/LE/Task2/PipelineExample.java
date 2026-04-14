import java.util.*;

/**
 * Example usage of the transformation pipeline
 */
public class PipelineExample {
    public static void main(String[] args) {
        TransformationPipeline pipeline = new TransformationPipeline();
        
        // Create sample low-level data
        String lowLevelDataStr = "name=John Doe;age=30;city=New York;salary=50000";
        byte[] lowLevelData = lowLevelDataStr.getBytes();
        
        // Define transformation rules
        List<TransformationRule> rules = new ArrayList<>();
        rules.add(new TransformationRule("rule1", "name", "name_upper", "uppercase", null));
        rules.add(new TransformationRule("rule2", "age", "age_doubled", "multiply", 2.0));
        rules.add(new TransformationRule("rule3", "salary", "salary_with_bonus", "add", 10000.0));
        
        try {
            // Execute the pipeline
            byte[] result = pipeline.executePipeline(lowLevelData, rules);
            String output = new String(result);
            
            System.out.println("Original: " + lowLevelDataStr);
            System.out.println("Transformed: " + output);
        } catch (Exception e) {
            System.err.println("Pipeline execution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}