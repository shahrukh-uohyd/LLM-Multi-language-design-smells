package com.example.inventory;

public final class InventoryNative {

    static {
        System.loadLibrary("inventory_native");
    }

    // Native method declaration
    public static native void processProduct(Product product);
}
