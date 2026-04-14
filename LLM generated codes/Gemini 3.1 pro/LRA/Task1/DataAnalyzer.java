package com.example;

public class DataAnalyzer {
    // The native method declaration
    public native int calculateUserRecordSum(UserRecord[] records);
}

class UserRecord {
    public int score; // The integer field we want to extract and sum
}