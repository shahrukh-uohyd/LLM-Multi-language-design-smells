package com.example.logic;

public class OperationHandler {

    public static void performOperation(int code, String message) {
        System.out.println(
            "Java logic invoked from native code | code=" +
            code + ", message=" + message
        );
    }
}
