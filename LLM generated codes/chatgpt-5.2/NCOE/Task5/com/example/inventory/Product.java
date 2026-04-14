package com.example.inventory;

public class Product {

    // Field accessed from native code (must not be private)
    public int stockCount;

    public Product(int stockCount) {
        this.stockCount = stockCount;
    }

    @Override
    public String toString() {
        return "Product{stockCount=" + stockCount + "}";
    }
}
