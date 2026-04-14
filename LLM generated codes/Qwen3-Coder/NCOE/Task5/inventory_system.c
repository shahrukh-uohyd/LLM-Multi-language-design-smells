// inventory_system.c
#include <jni.h>
#include <stdio.h>

// Constants for inventory management
#define LOW_STOCK_THRESHOLD 10
#define MAX_REORDER_QUANTITY 50

JNIEXPORT void JNICALL Java_Product_updateLowStock(JNIEnv *env, jobject productObj) {
    if (productObj == NULL) {
        return;
    }
    
    // Get the Product class
    jclass productClass = (*env)->GetObjectClass(env, productObj);
    
    // Step 1: Use GetFieldID to find the stockCount field and check current value
    jfieldID stockCountFieldID = (*env)->GetFieldID(env, productClass, "stockCount", "I");
    
    if (stockCountFieldID == NULL) {
        // Field not found - throw exception
        jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
        (*env)->ThrowNew(env, exceptionClass, "Could not find 'stockCount' field in Product class");
        return;
    }
    
    // Get the current stock count
    jint currentStock = (*env)->GetIntField(env, productObj, stockCountFieldID);
    
    // Check if stock is below threshold
    if (currentStock < LOW_STOCK_THRESHOLD) {
        // Perform complex calculation to determine new stock levels
        // This could involve sales forecasts, seasonal factors, etc.
        
        // Get price field to factor in pricing for reorder quantity
        jfieldID priceFieldID = (*env)->GetFieldID(env, productClass, "price", "D");
        if (priceFieldID == NULL) {
            jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
            (*env)->ThrowNew(env, exceptionClass, "Could not find 'price' field in Product class");
            return;
        }
        
        jdouble price = (*env)->GetDoubleField(env, productObj, priceFieldID);
        
        // Complex calculation based on various factors
        int baseReorderAmount = LOW_STOCK_THRESHOLD * 2;  // Reorder to twice the threshold
        
        // Factor in price category (higher priced items may have smaller reorder quantities)
        int priceFactor = 1;
        if (price > 500.0) {
            priceFactor = 1;  // High-value items - more conservative restocking
        } else if (price > 100.0) {
            priceFactor = 2;  // Medium-value items - moderate restocking
        } else {
            priceFactor = 3;  // Low-value items - aggressive restocking
        }
        
        // Calculate reorder amount with seasonal factor (simulated)
        int seasonalFactor = 1 + (rand() % 3);  // Random factor between 1-3 (could be real seasonal data)
        
        int reorderAmount = baseReorderAmount * priceFactor * seasonalFactor;
        
        // Ensure we don't exceed maximum reorder quantity
        if (reorderAmount > MAX_REORDER_QUANTITY) {
            reorderAmount = MAX_REORDER_QUANTITY;
        }
        
        // Calculate new stock level (current stock + reorder amount)
        jint newStock = currentStock + reorderAmount;
        
        // Step 3: Use GetFieldID again to find the stockCount field and update it
        jfieldID newStockCountFieldID = (*env)->GetFieldID(env, productClass, "stockCount", "I");
        
        if (newStockCountFieldID != NULL) {
            (*env)->SetIntField(env, productObj, newStockCountFieldID, newStock);
        }
    }
    // If stock is not low, no action needed - function returns without modifying anything
}