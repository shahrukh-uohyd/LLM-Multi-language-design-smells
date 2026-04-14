package com.example;

public class FieldAccessor {
    static {
        // Load the native library
        System.loadLibrary("field_accessor");
    }
    
    // Native method that retrieves an integer field value
    public native int retrieveIntegerField(Object obj, String fieldName);
    
    // Native method that retrieves multiple fields
    public native String retrieveMultipleFields(Object obj);
    
    public static void main(String[] args) {
        FieldAccessor accessor = new FieldAccessor();
        
        System.out.println("=== Field Access Validation Results ===\n");
        
        // Test with valid field
        DataContainer container = new DataContainer(42, "Test Container");
        System.out.println("Container: " + container);
        
        int result = accessor.retrieveIntegerField(container, "dataValue");
        System.out.println("Retrieved dataValue: " + result);
        
        // Test with non-existent field
        result = accessor.retrieveIntegerField(container, "nonExistentField");
        System.out.println("Attempt to access non-existent field: " + result);
        
        // Test with wrong field type
        result = accessor.retrieveIntegerField(container, "description");
        System.out.println("Attempt to access String field as int: " + result);
        
        // Test with another valid integer
        result = accessor.retrieveIntegerField(container, "isValid");
        System.out.println("Attempt to access boolean field as int: " + result);
        
        System.out.println("\nMultiple field access test:");
        String multiResult = accessor.retrieveMultipleFields(container);
        System.out.println(multiResult);
        
        // Test with null object
        try {
            result = accessor.retrieveIntegerField(null, "dataValue");
            System.out.println("Result with null object: " + result);
        } catch (Exception e) {
            System.out.println("Exception with null object: " + e.getMessage());
        }
    }
}