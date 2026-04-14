#include <jni.h>
#include <stdio.h>

JNIEXPORT void JNICALL Java_com_example_inventory_InventoryManager_processStock(
    JNIEnv *env, 
    jobject thiz,       // The InventoryManager instance
    jobject product,    // The Product instance passed as an argument
    jint threshold)     // The threshold value
{
    // 1. Safety check
    if (product == NULL) {
        return;
    }

    // 2. Get the class of the Product object
    jclass productClass = (*env)->GetObjectClass(env, product);
    if (productClass == NULL) {
        return;
    }

    // 3. First GetFieldID call: Look up the Field ID to read the stock count
    // "I" is the internal JNI signature for a primitive int
    jfieldID readStockFieldId = (*env)->GetFieldID(env, productClass, "stockCount", "I");
    if (readStockFieldId == NULL) {
        (*env)->DeleteLocalRef(env, productClass);
        return; // Field not found
    }

    // 4. Read the current stock count
    jint currentStock = (*env)->GetIntField(env, product, readStockFieldId);

    // 5. Check if the stock is below the threshold
    if (currentStock < threshold) {
        
        printf("Native Log: Stock is low (%d < %d). Calculating restock...\n", currentStock, threshold);
        
        // 6. Perform a complex calculation to determine new stock levels
        // (Simulated complex logic based on current stock)
        jint calculatedRestockAmount = (threshold - currentStock) * 2 + 50; 
        jint newStockLevel = currentStock + calculatedRestockAmount;
        
        // 7. Second GetFieldID call: Look up the Field ID again to update the stock count
        jfieldID writeStockFieldId = (*env)->GetFieldID(env, productClass, "stockCount", "I");
        if (writeStockFieldId != NULL) {
            
            // 8. Update the object's stockCount field with the new value
            (*env)->SetIntField(env, product, writeStockFieldId, newStockLevel);
            printf("Native Log: Restock complete. New level: %d\n", newStockLevel);
            
        }
    } else {
        printf("Native Log: Stock is sufficient (%d).\n", currentStock);
    }

    // 9. Memory Management: Clean up local reference
    (*env)->DeleteLocalRef(env, productClass);
}