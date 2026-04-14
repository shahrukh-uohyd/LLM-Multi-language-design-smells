package com.example;

public class TextTransformer {

    // Load the native library
    static {
        System.loadLibrary("texttransformer");
    }

    /**
     * Native C method to convert an array of strings to uppercase.
     * @param input The input array of strings.
     * @return A new array containing the uppercase strings.
     */
    public native String[] toUpperCaseBatch(String[] input);

    public static void main(String[] args) {
        String[] batch = {
            "hello world",
            "jni is powerful",
            "memory management is key",
            "Batch Processing"
        };

        TextTransformer transformer = new TextTransformer();
        String[] result = transformer.toUpperCaseBatch(batch);

        if (result != null) {
            for (String s : result) {
                System.out.println(s);
            }
        }
    }
}