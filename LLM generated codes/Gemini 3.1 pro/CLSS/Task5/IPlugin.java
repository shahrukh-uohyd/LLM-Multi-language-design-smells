package com.example;

// The common interface that all plugins must implement
public interface IPlugin {
    void execute(String payload);
}