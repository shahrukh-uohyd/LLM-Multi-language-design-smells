/**
 * Example usage of the text processing operation
 */
public class TextProcessorExample {
    public static void main(String[] args) {
        TextProcessor processor = new TextProcessor();
        
        // Example 1: Basic text processing
        String inputText1 = "hello,world,java,jni,text,processing";
        TextProcessingResult result1 = processor.processText(inputText1, "uppercase");
        
        if (result1.isSuccess()) {
            System.out.println("Input: " + inputText1);
            System.out.println("Output: " + result1.getOutput());
            System.out.println("Processing time: " + result1.getProcessingTime() + " nanoseconds\n");
        } else {
            System.out.println("Processing failed: " + result1.getErrorMessage());
        }
        
        // Example 2: Remove duplicates processing
        String inputText2 = "apple,banana,apple,cherry,banana,date";
        TextProcessingResult result2 = processor.processText(inputText2, "remove_duplicates");
        
        if (result2.isSuccess()) {
            System.out.println("Input: " + inputText2);
            System.out.println("Output: " + result2.getOutput());
            System.out.println("Processing time: " + result2.getProcessingTime() + " nanoseconds\n");
        } else {
            System.out.println("Processing failed: " + result2.getErrorMessage());
        }
        
        // Example 3: Sort alphabetically
        String inputText3 = "zebra,apple,mango,banana,grape";
        TextProcessingResult result3 = processor.processText(inputText3, "sort_alphabetically");
        
        if (result3.isSuccess()) {
            System.out.println("Input: " + inputText3);
            System.out.println("Output: " + result3.getOutput());
            System.out.println("Processing time: " + result3.getProcessingTime() + " nanoseconds\n");
        } else {
            System.out.println("Processing failed: " + result3.getErrorMessage());
        }
    }
}