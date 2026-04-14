#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <float.h>
#include "com_example_NumericalAnalyzer.h"
#include "com_example_StatisticsResult.h"
#include "com_example_Histogram.h"

// Helper: Check for NaN (more reliable than isnan on all platforms)
static int is_nan(double x) {
    return x != x;
}

// Helper: Quickselect algorithm for median (avoids full sort)
static double quickselect(double *arr, int n, int k) {
    if (n == 1) return arr[0];
    
    double pivot = arr[n / 2];
    int i = 0, j = n - 1;
    
    while (i <= j) {
        while (arr[i] < pivot) i++;
        while (arr[j] > pivot) j--;
        if (i <= j) {
            double tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
            i++;
            j--;
        }
    }
    
    if (k < i) return quickselect(arr, i, k);
    else return quickselect(arr + i, n - i, k - i);
}

// Helper: Calculate quartile using linear interpolation
static double calculate_quartile(double *sorted, int n, double quartile) {
    if (n == 0) return NAN;
    
    double pos = quartile * (n - 1);
    int lower = (int)floor(pos);
    int upper = (int)ceil(pos);
    
    if (lower == upper) return sorted[lower];
    
    double weight = pos - lower;
    return sorted[lower] * (1.0 - weight) + sorted[upper] * weight;
}

/*
 * Class:     com_example_NumericalAnalyzer
 * Method:    calculateStatistics
 * Signature: ([D)Lcom/example/StatisticsResult;
 */
JNIEXPORT jobject JNICALL Java_com_example_NumericalAnalyzer_calculateStatistics
  (JNIEnv *env, jobject thisObj, jdoubleArray data) {
    
    // Validate input
    if (data == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be null");
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, data);
    if (length == 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be empty");
        return NULL;
    }
    
    // Get array elements (copy for safety with small/medium arrays)
    jboolean isCopy;
    jdouble *elements = (*env)->GetDoubleArrayElements(env, data, &isCopy);
    if (elements == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Failed to access array elements");
        return NULL;
    }
    
    // First pass: collect statistics excluding NaN
    double sum = 0.0;
    double sumSq = 0.0;
    double min = DBL_MAX;
    double max = -DBL_MAX;
    int validCount = 0;
    int nanCount = 0;
    
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        if (is_nan(val)) {
            nanCount++;
            continue;
        }
        
        validCount++;
        sum += val;
        sumSq += val * val;
        
        if (val < min) min = val;
        if (val > max) max = val;
    }
    
    // Check for all-NaN case
    if (validCount == 0) {
        (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "No valid (non-NaN) values in dataset");
        return NULL;
    }
    
    // Calculate mean and standard deviation
    double mean = sum / validCount;
    double variance = (sumSq - (sum * sum) / validCount) / validCount;
    double stdDev = sqrt(variance < 0 ? 0 : variance); // Handle precision errors
    
    // Second pass: collect valid values for median calculation
    double *validValues = (double*)malloc(validCount * sizeof(double));
    if (!validValues) {
        (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Native allocation failed for median calculation");
        return NULL;
    }
    
    int idx = 0;
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        if (!is_nan(val)) {
            validValues[idx++] = val;
        }
    }
    
    // Release Java array early to minimize GC pressure
    (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
    
    // Calculate median using quickselect (avoids full sort for large datasets)
    double median;
    if (validCount % 2 == 1) {
        median = quickselect(validValues, validCount, validCount / 2);
    } else {
        double lower = quickselect(validValues, validCount, (validCount - 1) / 2);
        // Need to recopy since quickselect modifies the array
        memcpy(validValues, validValues, validCount * sizeof(double)); // Actually need fresh copy
        double *temp = (double*)malloc(validCount * sizeof(double));
        if (!temp) {
            free(validValues);
            (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                            "Native allocation failed for median calculation");
            return NULL;
        }
        memcpy(temp, validValues, validCount * sizeof(double));
        double upper = quickselect(temp, validCount, validCount / 2);
        median = (lower + upper) / 2.0;
        free(temp);
    }
    
    free(validValues);
    
    // Create and return StatisticsResult object
    jclass resultClass = (*env)->FindClass(env, "com/example/StatisticsResult");
    if (!resultClass) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"),
                        "StatisticsResult class not found");
        return NULL;
    }
    
    jmethodID constructor = (*env)->GetMethodID(env, resultClass, "<init>", 
        "(DDDDDII)V");
    if (!constructor) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"),
                        "StatisticsResult constructor not found");
        return NULL;
    }
    
    return (*env)->NewObject(env, resultClass, constructor,
        mean, median, stdDev, min, max, validCount, nanCount);
}

/*
 * Class:     com_example_NumericalAnalyzer
 * Method:    findMinMax
 * Signature: ([D)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_com_example_NumericalAnalyzer_findMinMax
  (JNIEnv *env, jobject thisObj, jdoubleArray data) {
    
    if (data == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be null");
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, data);
    if (length == 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be empty");
        return NULL;
    }
    
    jdouble *elements = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (!elements) return NULL;
    
    double min = DBL_MAX;
    double max = -DBL_MAX;
    jsize minIdx = 0;
    jsize maxIdx = 0;
    int foundValid = 0;
    
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        if (is_nan(val)) continue;
        
        if (!foundValid) {
            min = max = val;
            minIdx = maxIdx = i;
            foundValid = 1;
            continue;
        }
        
        if (val < min) {
            min = val;
            minIdx = i;
        }
        if (val > max) {
            max = val;
            maxIdx = i;
        }
    }
    
    (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
    
    if (!foundValid) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "No valid (non-NaN) values in dataset");
        return NULL;
    }
    
    jdoubleArray result = (*env)->NewDoubleArray(env, 4);
    if (!result) return NULL;
    
    jdouble values[4] = {min, (jdouble)minIdx, max, (jdouble)maxIdx};
    (*env)->SetDoubleArrayRegion(env, result, 0, 4, values);
    
    return result;
}

/*
 * Class:     com_example_NumericalAnalyzer
 * Method:    generateHistogram
 * Signature: ([DI)Lcom/example/Histogram;
 */
JNIEXPORT jobject JNICALL Java_com_example_NumericalAnalyzer_generateHistogram
  (JNIEnv *env, jobject thisObj, jdoubleArray data, jint numBins) {
    
    if (data == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be null");
        return NULL;
    }
    
    if (numBins <= 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Number of bins must be positive");
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, data);
    if (length == 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be empty");
        return NULL;
    }
    
    jdouble *elements = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (!elements) return NULL;
    
    // Find min/max excluding NaN
    double min = DBL_MAX;
    double max = -DBL_MAX;
    int validCount = 0;
    
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        if (is_nan(val)) continue;
        
        validCount++;
        if (val < min) min = val;
        if (val > max) max = val;
    }
    
    (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
    
    if (validCount == 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "No valid (non-NaN) values in dataset");
        return NULL;
    }
    
    // Handle single unique value case
    if (min == max) {
        max = min + 1.0; // Create artificial range
    }
    
    // Create bin edges
    double binWidth = (max - min) / numBins;
    jdoubleArray binEdges = (*env)->NewDoubleArray(env, numBins + 1);
    if (!binEdges) return NULL;
    
    jdouble *edges = (*env)->GetDoubleArrayElements(env, binEdges, NULL);
    if (!edges) return NULL;
    
    for (int i = 0; i <= numBins; i++) {
        edges[i] = min + i * binWidth;
    }
    edges[numBins] = max; // Ensure last edge equals max exactly
    
    (*env)->ReleaseDoubleArrayElements(env, binEdges, edges, 0);
    
    // Count values per bin (second pass)
    jintArray binCounts = (*env)->NewIntArray(env, numBins);
    if (!binCounts) return NULL;
    
    jint *counts = (*env)->GetIntArrayElements(env, binCounts, NULL);
    if (!counts) return NULL;
    memset(counts, 0, numBins * sizeof(jint));
    
    // Reacquire data array for second pass
    elements = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (!elements) {
        (*env)->ReleaseIntArrayElements(env, binCounts, counts, JNI_ABORT);
        return NULL;
    }
    
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        if (is_nan(val)) continue;
        
        // Find bin index
        int binIdx;
        if (val >= max) {
            binIdx = numBins - 1; // Last bin includes max value
        } else {
            binIdx = (int)((val - min) / binWidth);
            if (binIdx >= numBins) binIdx = numBins - 1;
        }
        counts[binIdx]++;
    }
    
    (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, binCounts, counts, 0);
    
    // Create and return Histogram object
    jclass histClass = (*env)->FindClass(env, "com/example/Histogram");
    if (!histClass) return NULL;
    
    jmethodID constructor = (*env)->GetMethodID(env, histClass, "<init>", 
        "([D[I)V");
    if (!constructor) return NULL;
    
    return (*env)->NewObject(env, histClass, constructor, binEdges, binCounts);
}

/*
 * Class:     com_example_NumericalAnalyzer
 * Method:    detectOutliers
 * Signature: ([D)[Z
 */
JNIEXPORT jbooleanArray JNICALL Java_com_example_NumericalAnalyzer_detectOutliers
  (JNIEnv *env, jobject thisObj, jdoubleArray data) {
    
    if (data == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be null");
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, data);
    if (length == 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be empty");
        return NULL;
    }
    
    jdouble *elements = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (!elements) return NULL;
    
    // Collect valid values
    double *validValues = (double*)malloc(length * sizeof(double));
    if (!validValues) {
        (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                        "Native allocation failed");
        return NULL;
    }
    
    int validCount = 0;
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        if (!is_nan(val)) {
            validValues[validCount++] = val;
        }
    }
    
    (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
    
    if (validCount < 4) { // Need at least 4 points for meaningful IQR
        free(validValues);
        jbooleanArray result = (*env)->NewBooleanArray(env, length);
        if (result) {
            jboolean *bools = (*env)->GetBooleanArrayElements(env, result, NULL);
            if (bools) {
                memset(bools, JNI_FALSE, length * sizeof(jboolean));
                (*env)->ReleaseBooleanArrayElements(env, result, bools, 0);
            }
        }
        return result;
    }
    
    // Sort valid values for quartile calculation
    qsort(validValues, validCount, sizeof(double), 
        [](const void *a, const void *b) {
            double d1 = *(const double*)a;
            double d2 = *(const double*)b;
            return (d1 > d2) - (d1 < d2);
        });
    
    // Calculate quartiles and IQR
    double q1 = calculate_quartile(validValues, validCount, 0.25);
    double q3 = calculate_quartile(validValues, validCount, 0.75);
    double iqr = q3 - q1;
    double lowerBound = q1 - 1.5 * iqr;
    double upperBound = q3 + 1.5 * iqr;
    
    free(validValues);
    
    // Create result array and mark outliers
    jbooleanArray result = (*env)->NewBooleanArray(env, length);
    if (!result) return NULL;
    
    jboolean *outliers = (*env)->GetBooleanArrayElements(env, result, NULL);
    if (!outliers) return NULL;
    
    elements = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (!elements) {
        (*env)->ReleaseBooleanArrayElements(env, result, outliers, JNI_ABORT);
        return NULL;
    }
    
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        outliers[i] = (is_nan(val) || val < lowerBound || val > upperBound) ? JNI_TRUE : JNI_FALSE;
    }
    
    (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
    (*env)->ReleaseBooleanArrayElements(env, result, outliers, 0);
    
    return result;
}

/*
 * Class:     com_example_NumericalAnalyzer
 * Method:    normalizeZScore
 * Signature: ([D)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_com_example_NumericalAnalyzer_normalizeZScore
  (JNIEnv *env, jobject thisObj, jdoubleArray data) {
    
    if (data == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be null");
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, data);
    if (length == 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be empty");
        return NULL;
    }
    
    jdouble *elements = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (!elements) return NULL;
    
    // Calculate mean and std dev (first pass)
    double sum = 0.0;
    int validCount = 0;
    
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        if (!is_nan(val)) {
            sum += val;
            validCount++;
        }
    }
    
    if (validCount == 0) {
        (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "No valid (non-NaN) values in dataset");
        return NULL;
    }
    
    double mean = sum / validCount;
    
    double sumSq = 0.0;
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        if (!is_nan(val)) {
            double diff = val - mean;
            sumSq += diff * diff;
        }
    }
    
    double variance = (sumSq - (sum * sum) / validCount) / validCount;
    double stdDev = sqrt(variance < 0 ? 0 : variance);
    
    // Handle zero standard deviation case
    if (stdDev < 1e-10) {
        stdDev = 1.0; // Prevent division by zero; all values become 0 after normalization
    }
    
    // Create result array and normalize (second pass)
    jdoubleArray result = (*env)->NewDoubleArray(env, length);
    if (!result) {
        (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
        return NULL;
    }
    
    jdouble *normalized = (*env)->GetDoubleArrayElements(env, result, NULL);
    if (!normalized) {
        (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
        return NULL;
    }
    
    for (jsize i = 0; i < length; i++) {
        double val = elements[i];
        normalized[i] = is_nan(val) ? NAN : (val - mean) / stdDev;
    }
    
    (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, result, normalized, 0);
    
    return result;
}

/*
 * Class:     com_example_NumericalAnalyzer
 * Method:    calculateMovingAverage
 * Signature: ([DI)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_com_example_NumericalAnalyzer_calculateMovingAverage
  (JNIEnv *env, jobject thisObj, jdoubleArray data, jint windowSize) {
    
    if (data == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be null");
        return NULL;
    }
    
    if (windowSize <= 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Window size must be positive");
        return NULL;
    }
    
    jsize length = (*env)->GetArrayLength(env, data);
    if (length == 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Data array cannot be empty");
        return NULL;
    }
    
    if (windowSize > length) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalArgumentException"),
                        "Window size cannot exceed data length");
        return NULL;
    }
    
    jdouble *elements = (*env)->GetDoubleArrayElements(env, data, NULL);
    if (!elements) return NULL;
    
    jsize resultLength = length - windowSize + 1;
    jdoubleArray result = (*env)->NewDoubleArray(env, resultLength);
    if (!result) {
        (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
        return NULL;
    }
    
    jdouble *averages = (*env)->GetDoubleArrayElements(env, result, NULL);
    if (!averages) {
        (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
        return NULL;
    }
    
    // Calculate moving averages
    for (jsize i = 0; i < resultLength; i++) {
        double sum = 0.0;
        int count = 0;
        
        for (int j = 0; j < windowSize; j++) {
            double val = elements[i + j];
            if (!is_nan(val)) {
                sum += val;
                count++;
            }
        }
        
        averages[i] = (count > 0) ? (sum / count) : NAN;
    }
    
    (*env)->ReleaseDoubleArrayElements(env, data, elements, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, result, averages, 0);
    
    return result;
}