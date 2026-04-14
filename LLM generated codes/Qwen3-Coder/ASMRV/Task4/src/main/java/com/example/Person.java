package com.example;

public class Person {
    private String firstName;
    private String lastName;
    private int age;
    private String email;
    
    // Default constructor
    public Person() {
        this.firstName = "Unknown";
        this.lastName = "Unknown";
        this.age = 0;
        this.email = "unknown@example.com";
    }
    
    // Constructor with name
    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = 0;
        this.email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
    }
    
    // Constructor with full details
    public Person(String firstName, String lastName, int age, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.email = email;
    }
    
    // Copy constructor
    public Person(Person other) {
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.age = other.age;
        this.email = other.email;
    }
    
    // Getters
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    
    // Setters
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setAge(int age) { this.age = age; }
    public void setEmail(String email) { this.email = email; }
    
    @Override
    public String toString() {
        return String.format("Person{firstName='%s', lastName='%s', age=%d, email='%s'}", 
                           firstName, lastName, age, email);
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}