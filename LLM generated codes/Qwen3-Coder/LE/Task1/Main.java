// Main.java - Example usage
public class Main {
    public static void main(String[] args) {
        DataProcessor processor = new DataProcessor();
        
        // Example 1: Process simple structured data
        String inputData = "  Hello , World , Java , JNI  ";
        ProcessingResult result = processor.processInput(inputData);
        
        if (result.isSuccess()) {
            System.out.println("Processing successful!");
            System.out.println("Result: " + result.getFinalResult());
        } else {
            System.out.println("Processing failed: " + result.getErrorMessage());
        }
        
        // Example 2: Process more complex data
        InputData complexInput = new InputData("John Doe, 30, Engineer|Jane Smith, 25, Designer");
        ProcessingResult complexResult = processor.processStructuredData(complexInput);
        
        if (complexResult.isSuccess()) {
            System.out.println("Complex processing successful!");
            System.out.println("Result: " + complexResult.getFinalResult());
        } else {
            System.out.println("Complex processing failed: " + complexResult.getErrorMessage());
        }
    }
}