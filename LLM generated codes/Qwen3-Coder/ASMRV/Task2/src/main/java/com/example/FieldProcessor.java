package com.example;

public class FieldProcessor {
    private final DataContainer container;
    private final String fieldName;
    private final int computationType; // 0: sum of chars, 1: length, 2: hash
    
    public FieldProcessor(DataContainer container, String fieldName, int computationType) {
        this.container = container;
        this.fieldName = fieldName;
        this.computationType = computationType;
    }
    
    public DataContainer getContainer() { return container; }
    public String getFieldName() { return fieldName; }
    public int getComputationType() { return computationType; }
}