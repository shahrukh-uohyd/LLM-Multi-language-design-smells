package com.example;

public class FieldComputer {
    static {
        // Load the native library
        System.loadLibrary("field_computer");
    }
    
    // Native method declaration
    public native long computeFieldValueInfo(Object obj, String fieldName, int computationType);
    
    public static void main(String[] args) {
        FieldComputer computer = new FieldComputer();
        
        // Create test objects
        DataContainer container1 = new DataContainer(42, "Hello World", new double[]{1.5, 2.7, 3.9}, true, 'A');
        DataContainer container2 = new DataContainer(-10, "Java Programming", new double[]{10.0, 20.0}, false, 'B');
        
        // Define computation tasks
        FieldProcessor[] processors = {
            new FieldProcessor(container1, "text", 0),      // Sum of character codes in text
            new FieldProcessor(container1, "values", 1),    // Length of values array
            new FieldProcessor(container1, "number", 2),    // Hash of number field
            new FieldProcessor(container2, "text", 0),      // Sum of character codes in text
            new FieldProcessor(container2, "values", 1),    // Length of values array
            new FieldProcessor(container2, "isActive", 2),  // Hash of boolean field
            new FieldProcessor(container2, "category", 0),  // Character code of category
        };
        
        System.out.println("=== Field Value Information Computation Results ===\n");
        
        for (FieldProcessor processor : processors) {
            try {
                long result = computer.computeFieldValueInfo(
                    processor.getContainer(),
                    processor.getFieldName(),
                    processor.getComputationType()
                );
                
                System.out.printf(
                    "Container: %s\n",
                    processor.getContainer().toString()
                );
                System.out.printf(
                    "Field: %s, Computation Type: %s, Result: %d\n",
                    processor.getFieldName(),
                    getComputationTypeName(processor.getComputationType()),
                    result
                );
                System.out.println();
            } catch (Exception e) {
                System.err.printf(
                    "Error processing field '%s' on container: %s\n\n",
                    processor.getFieldName(),
                    e.getMessage()
                );
            }
        }
    }
    
    private static String getComputationTypeName(int type) {
        switch (type) {
            case 0: return "Character Code Sum";
            case 1: return "Length/Size";
            case 2: return "Hash Value";
            default: return "Unknown";
        }
    }
}