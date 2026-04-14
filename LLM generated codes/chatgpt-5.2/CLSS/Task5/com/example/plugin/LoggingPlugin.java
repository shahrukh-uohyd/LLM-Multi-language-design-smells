package com.example.plugin;

public class LoggingPlugin implements Plugin {

    @Override
    public void execute() {
        System.out.println("LoggingPlugin executed from native code");
    }
}
