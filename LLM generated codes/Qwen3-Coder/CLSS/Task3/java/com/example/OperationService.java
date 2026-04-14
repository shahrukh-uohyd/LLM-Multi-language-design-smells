package com.example;

public class OperationService {
    public void performOperation(String operationName, int parameter) {
        System.out.println("Executing operation: " + operationName + " with parameter: " + parameter);
        // Your actual business logic here
    }
    
    public String executeComplexOperation(String operationType, double[] values) {
        System.out.println("Complex operation: " + operationType);
        double sum = 0;
        for (double val : values) {
            sum += val;
        }
        return "Result: " + sum + " from operation " + operationType;
    }
    
    public boolean validateOperation(String operationName) {
        return operationName != null && !operationName.isEmpty();
    }
}