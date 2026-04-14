package com.example;

public class ConstructorAssistant {
    static {
        // Load the native library
        System.loadLibrary("constructor_assistant");
    }
    
    // Native method to prepare constructor data
    public native String prepareConstructorData(String className, String[] parameterTypes, Object[] parameterValues);
    
    // Native method to validate constructor existence
    public native boolean validateConstructor(String className, String[] parameterTypes);
    
    public static void main(String[] args) {
        ConstructorAssistant assistant = new ConstructorAssistant();
        
        System.out.println("=== Object Construction Assistant Results ===\n");
        
        // Test cases for Person class
        System.out.println("Testing Person class constructors:");
        
        // Test default constructor
        String[] paramTypes1 = {};
        Object[] paramValues1 = {};
        System.out.println("Default constructor: " + 
                          assistant.prepareConstructorData("com.example.Person", paramTypes1, paramValues1));
        
        // Test name constructor
        String[] paramTypes2 = {"java.lang.String", "java.lang.String"};
        Object[] paramValues2 = {"John", "Doe"};
        System.out.println("Name constructor: " + 
                          assistant.prepareConstructorData("com.example.Person", paramTypes2, paramValues2));
        
        // Test full constructor
        String[] paramTypes3 = {"java.lang.String", "java.lang.String", "int", "java.lang.String"};
        Object[] paramValues3 = {"Jane", "Smith", 30, "jane.smith@example.com"};
        System.out.println("Full constructor: " + 
                          assistant.prepareConstructorData("com.example.Person", paramTypes3, paramValues3));
        
        System.out.println();
        
        // Test cases for Employee class
        System.out.println("Testing Employee class constructors:");
        
        // Test default constructor
        String[] paramTypes4 = {};
        Object[] paramValues4 = {};
        System.out.println("Default constructor: " + 
                          assistant.prepareConstructorData("com.example.Employee", paramTypes4, paramValues4));
        
        // Test partial constructor
        String[] paramTypes5 = {"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"};
        Object[] paramValues5 = {"Alice", "Johnson", "EMP001", "Engineering"};
        System.out.println("Partial constructor: " + 
                          assistant.prepareConstructorData("com.example.Employee", paramTypes5, paramValues5));
        
        // Test full constructor
        String[] paramTypes6 = {"java.lang.String", "java.lang.String", "int", "java.lang.String", 
                               "java.lang.String", "java.lang.String", "double"};
        Object[] paramValues6 = {"Bob", "Brown", 35, "bob.brown@example.com", "EMP002", "Marketing", 75000.0};
        System.out.println("Full constructor: " + 
                          assistant.prepareConstructorData("com.example.Employee", paramTypes6, paramValues6));
        
        System.out.println();
        
        // Validate constructors
        System.out.println("Validating constructors:");
        System.out.println("Person default constructor exists: " + 
                          assistant.validateConstructor("com.example.Person", new String[]{}));
        System.out.println("Person name constructor exists: " + 
                          assistant.validateConstructor("com.example.Person", new String[]{"java.lang.String", "java.lang.String"}));
        System.out.println("Person invalid constructor exists: " + 
                          assistant.validateConstructor("com.example.Person", new String[]{"java.lang.String", "int"}));
        System.out.println("Employee full constructor exists: " + 
                          assistant.validateConstructor("com.example.Employee", 
                                                      new String[]{"java.lang.String", "java.lang.String", "int", 
                                                                 "java.lang.String", "java.lang.String", 
                                                                 "java.lang.String", "double"}));
    }
}