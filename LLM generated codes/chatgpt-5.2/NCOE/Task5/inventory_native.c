#include <jni.h>

#define LOW_STOCK_THRESHOLD 10

/*
 * Class:     com_example_inventory_InventoryNative
 * Method:    processProduct
 * Signature: (Lcom/example/inventory/Product;)V
 */
JNIEXPORT void JNICALL
Java_com_example_inventory_InventoryNative_processProduct(
        JNIEnv *env,
        jclass,
        jobject productObj
) {
    if (productObj == NULL) {
        return;
    }

    jclass productClass = (*env)->GetObjectClass(env, productObj);
    if (productClass == NULL) {
        return;
    }

    /* -------- First lookup: read stockCount -------- */
    jfieldID stockFieldID = (*env)->GetFieldID(
        env,
        productClass,
        "stockCount",
        "I"
    );

    if (stockFieldID == NULL) {
        (*env)->DeleteLocalRef(env, productClass);
        return;
    }

    jint currentStock = (*env)->GetIntField(
        env,
        productObj,
        stockFieldID
    );

    if (currentStock >= LOW_STOCK_THRESHOLD) {
        // Stock is sufficient, nothing to do
        (*env)->DeleteLocalRef(env, productClass);
        return;
    }

    /* -------- Complex stock calculation (example) -------- */
    /*
       Placeholder for real logic:
       - supplier lead time
       - historical demand
       - safety margin
    */
    jint calculatedStock =
        (currentStock * 2) + LOW_STOCK_THRESHOLD;

    /* -------- Second lookup: update stockCount -------- */
    jfieldID stockFieldID2 = (*env)->GetFieldID(
        env,
        productClass,
        "stockCount",
        "I"
    );

    if (stockFieldID2 != NULL) {
        (*env)->SetIntField(
            env,
            productObj,
            stockFieldID2,
            calculatedStock
        );
    }

    (*env)->DeleteLocalRef(env, productClass);
}
