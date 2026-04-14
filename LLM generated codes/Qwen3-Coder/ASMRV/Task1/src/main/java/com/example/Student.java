package com.example;

import java.util.List;

public class Student extends Person {
    private String studentId;
    private List<String> courses;
    
    public Student(String name, int age, String studentId) {
        super(name, age);
        this.studentId = studentId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    @Override
    public void sayHello() {
        System.out.println("Hello, I'm student " + getName() + " with ID: " + studentId);
    }
}