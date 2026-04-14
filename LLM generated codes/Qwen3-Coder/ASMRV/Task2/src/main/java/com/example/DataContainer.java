package com.example;

import java.util.Arrays;

public class DataContainer {
    private int number;
    private String text;
    private double[] values;
    private boolean isActive;
    private char category;
    private long timestamp;
    
    public DataContainer(int number, String text, double[] values, boolean isActive, char category) {
        this.number = number;
        this.text = text;
        this.values = values != null ? values.clone() : null;
        this.isActive = isActive;
        this.category = category;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public int getNumber() { return number; }
    public String getText() { return text; }
    public double[] getValues() { return values != null ? values.clone() : null; }
    public boolean isActive() { return isActive; }
    public char getCategory() { return category; }
    public long getTimestamp() { return timestamp; }
    
    // Setters
    public void setNumber(int number) { this.number = number; }
    public void setText(String text) { this.text = text; }
    public void setValues(double[] values) { 
        this.values = values != null ? values.clone() : null; 
    }
    public void setActive(boolean active) { this.isActive = active; }
    public void setCategory(char category) { this.category = category; }
    
    @Override
    public String toString() {
        return String.format(
            "DataContainer{number=%d, text='%s', values=%s, isActive=%b, category=%c, timestamp=%d}",
            number, text, Arrays.toString(values), isActive, category, timestamp
        );
    }
}