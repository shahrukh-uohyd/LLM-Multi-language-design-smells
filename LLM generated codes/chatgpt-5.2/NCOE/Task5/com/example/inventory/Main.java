package com.example.inventory;

public class Main {

    public static void main(String[] args) {
        Product product = new Product(8); // low stock

        System.out.println("Before processing: " + product);
        InventoryNative.processProduct(product);
        System.out.println("After processing:  " + product);
    }
}
