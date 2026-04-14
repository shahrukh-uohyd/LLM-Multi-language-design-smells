package com.example.text;

import java.util.Arrays;

/**
 * End-to-end demonstration of batch uppercase transformation via JNI.
 */
public class Main {

    public static void main(String[] args) {

        String[] inputs = {
            "hello world",
            "JNI Text Transformation",
            "sensor_unit_hPa",
            "café",            // Latin extended — tests non-ASCII handling
            "über",            // German umlaut
            "",                // empty string edge case
            null,              // null element — must be preserved as null
            "ALREADY UPPER",   // no-op case
            "mixed123CASE!@#"  // alphanumeric + symbols
        };

        TextTransformer transformer = new TextTransformer();
        String[]        results     = transformer.toUpperCaseBatch(inputs);

        System.out.printf("Transformed %d strings:%n%n", results.length);

        for (int i = 0; i < inputs.length; i++) {
            System.out.printf("  [%d] %-30s  =>  %s%n",
                              i,
                              inputs[i] == null ? "(null)" : '"' + inputs[i] + '"',
                              results[i] == null ? "(null)" : '"' + results[i] + '"');
        }

        /*
         * Expected output:
         *
         * [0] "hello world"              =>  "HELLO WORLD"
         * [1] "JNI Text Transformation"  =>  "JNI TEXT TRANSFORMATION"
         * [2] "sensor_unit_hPa"          =>  "SENSOR_UNIT_HPA"
         * [3] "café"                     =>  "CAFÉ"
         * [4] "über"                     =>  "ÜBER"
         * [5] ""                         =>  ""
         * [6] (null)                     =>  (null)
         * [7] "ALREADY UPPER"            =>  "ALREADY UPPER"
         * [8] "mixed123CASE!@#"          =>  "MIXED123CASE!@#"
         */
    }
}