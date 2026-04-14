package com.example;

public class DataContainer {
    private int dataValue;
    private String description;
    private boolean isValid;
    
    public DataContainer(int dataValue) {
        this.dataValue = dataValue;
        this.description = "Container with value: " + dataValue;
        this.isValid = dataValue >= 0;
    }
    
    public DataContainer(int dataValue, String description) {
        this.dataValue = dataValue;
        this.description = description;
        this.isValid = dataValue >= 0;
    }
    
    public int getDataValue() {
        return dataValue;
    }
    
    public void setDataValue(int dataValue) {
        this.dataValue = dataValue;
        this.isValid = dataValue >= 0;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    @Override
    public String toString() {
        return String.format("DataContainer{dataValue=%d, description='%s', isValid=%b}", 
                           dataValue, description, isValid);
    }
}