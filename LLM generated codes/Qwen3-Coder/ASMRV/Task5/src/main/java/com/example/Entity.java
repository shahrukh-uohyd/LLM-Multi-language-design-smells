package com.example;

import java.util.List;
import java.util.Map;

public class Entity {
    // Fields
    public static final String TYPE = "ENTITY";
    private int id;
    protected String name;
    public boolean isActive;
    private List<String> tags;
    protected double score;
    
    // Constructors
    public Entity() {
        this.id = 0;
        this.name = "default";
        this.isActive = false;
        this.score = 0.0;
    }
    
    public Entity(int id, String name) {
        this.id = id;
        this.name = name;
        this.isActive = true;
        this.score = 100.0;
    }
    
    // Public methods
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
    
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    // Protected method
    protected void internalUpdate() {
        System.out.println("Internal update called for " + name);
    }
    
    // Private method
    private void validate() {
        if (id < 0) {
            throw new IllegalArgumentException("ID cannot be negative");
        }
    }
    
    // Public method with parameters
    public boolean equals(Entity other) {
        if (other == null) return false;
        return this.id == other.id && this.name.equals(other.name);
    }
    
    @Override
    public String toString() {
        return String.format("Entity{id=%d, name='%s', isActive=%b, score=%.2f}", id, name, isActive, score);
    }
}