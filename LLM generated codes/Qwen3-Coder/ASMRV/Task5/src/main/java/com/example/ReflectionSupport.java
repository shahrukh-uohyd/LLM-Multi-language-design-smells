package com.example;

public class ReflectionSupport {
    static {
        // Load the native library
        System.loadLibrary("reflection_support");
    }
    
    // Native method to get class information
    public native String getClassInfo(String className);
    
    // Native method to get field information
    public native String getFieldInfo(String className, String fieldName);
    
    // Native method to get method information
    public native String getMethodInfo(String className, String methodName, String[] parameterTypes);
    
    // Native method to get all members of a class
    public native String getAllMembers(String className);
    
    public static void main(String[] args) {
        ReflectionSupport support = new ReflectionSupport();
        
        System.out.println("=== Reflection Support Utility Results ===\n");
        
        // Test class information
        System.out.println("=== Class Information ===");
        System.out.println(support.getClassInfo("com.example.Entity"));
        System.out.println(support.getClassInfo("com.example.ServiceManager"));
        System.out.println();
        
        // Test field information
        System.out.println("=== Field Information ===");
        System.out.println(support.getFieldInfo("com.example.Entity", "id"));
        System.out.println(support.getFieldInfo("com.example.Entity", "name"));
        System.out.println(support.getFieldInfo("com.example.Entity", "TYPE"));
        System.out.println(support.getFieldInfo("com.example.ServiceManager", "serviceName"));
        System.out.println();
        
        // Test method information
        System.out.println("=== Method Information ===");
        System.out.println(support.getMethodInfo("com.example.Entity", "getId", new String[]{}));
        System.out.println(support.getMethodInfo("com.example.Entity", "setName", new String[]{"java.lang.String"}));
        System.out.println(support.getMethodInfo("com.example.Entity", "equals", new String[]{"com.example.Entity"}));
        System.out.println(support.getMethodInfo("com.example.ServiceManager", "getServiceCount", new String[]{}));
        System.out.println();
        
        // Test all members
        System.out.println("=== All Members Information ===");
        System.out.println(support.getAllMembers("com.example.Entity"));
        System.out.println();
        System.out.println(support.getAllMembers("com.example.ServiceManager"));
    }
}