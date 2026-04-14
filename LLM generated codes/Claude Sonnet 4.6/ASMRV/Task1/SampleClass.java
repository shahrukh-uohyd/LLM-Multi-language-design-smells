package com.jni.metadata;

/**
 * A sample class whose metadata will be retrieved via JNI.
 */
public class SampleClass extends Object {

    public int id;
    public String name;
    private double salary;

    public SampleClass() {}

    public SampleClass(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private double getSalary() {
        return salary;
    }
}