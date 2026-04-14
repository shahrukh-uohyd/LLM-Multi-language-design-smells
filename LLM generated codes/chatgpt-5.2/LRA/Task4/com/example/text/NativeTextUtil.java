package com.example.text;

public class NativeTextUtil {
    static {
        System.loadLibrary("textutil");
    }

    public static native String[] toUpperCaseBatch(String[] input);
}
