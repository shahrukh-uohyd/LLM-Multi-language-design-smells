/*
 * FieldComputeJNI.c
 *
 * Implements three JNI methods that read fields directly from a Java
 * Employee object using JNI field-access APIs (GetFieldID / Get*Field),
 * perform arithmetic, and return the results to Java.
 *
 * Compiled with:
 *   gcc -shared -fPIC \
 *       -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
 *       -Iheaders/ \
 *       -o build/libFieldComputeJNI.so \
 *       native/FieldComputeJNI.c
 */

#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/* ------------------------------------------------------------------ */
/* Helper: look up the Employee class and all required field IDs once. */
/* We cache nothing here for simplicity; production code would cache   */
/* these in a static struct initialised from JNI_OnLoad.               */
/* ------------------------------------------------------------------ */

typedef struct {
    jclass    cls;
    jfieldID  fid_name;
    jfieldID  fid_hoursWorked;
    jfieldID  fid_hourlyRate;
    jfieldID  fid_overtimeHours;
    jfieldID  fid_taxRate;
} EmployeeFields;

/**
 * Populate an EmployeeFields struct for the given object.
 * Returns 0 on success, -1 on any JNI error.
 */
static int resolveFields(JNIEnv *env, jobject empObj, EmployeeFields *ef)
{
    ef->cls = (*env)->GetObjectClass(env, empObj);
    if (!ef->cls) return -1;

    ef->fid_name = (*env)->GetFieldID(env, ef->cls, "name", "Ljava/lang/String;");
    if (!ef->fid_name) return -1;

    ef->fid_hoursWorked = (*env)->GetFieldID(env, ef->cls, "hoursWorked", "I");
    if (!ef->fid_hoursWorked) return -1;

    ef->fid_hourlyRate = (*env)->GetFieldID(env, ef->cls, "hourlyRate", "D");
    if (!ef->fid_hourlyRate) return -1;

    ef->fid_overtimeHours = (*env)->GetFieldID(env, ef->cls, "overtimeHours", "I");
    if (!ef->fid_overtimeHours) return -1;

    ef->fid_taxRate = (*env)->GetFieldID(env, ef->cls, "taxRate", "D");
    if (!ef->fid_taxRate) return -1;

    return 0;
}

/* ------------------------------------------------------------------ */
/* Helper: compute gross pay from raw field values.                    */
/*                                                                     */
/*   regularPay  = hoursWorked  * hourlyRate                           */
/*   overtimePay = overtimeHours * hourlyRate * 1.5                    */
/*   grossPay    = regularPay + overtimePay                            */
/* ------------------------------------------------------------------ */
static double calcGross(jint hoursWorked, jdouble hourlyRate, jint overtimeHours)
{
    double regularPay  = (double)hoursWorked  * hourlyRate;
    double overtimePay = (double)overtimeHours * hourlyRate * 1.5;
    return regularPay + overtimePay;
}

/* ================================================================== */
/* 1.  computeGrossPay                                                 */
/* ================================================================== */
JNIEXPORT jdouble JNICALL
Java_com_jni_fields_FieldComputeJNI_computeGrossPay(JNIEnv  *env,
                                                     jobject  thisObj,
                                                     jobject  empObj)
{
    EmployeeFields ef;
    if (resolveFields(env, empObj, &ef) != 0) {
        return -1.0;    /* exception already pending */
    }

    /* Read fields directly from the Java object */
    jint    hoursWorked   = (*env)->GetIntField   (env, empObj, ef.fid_hoursWorked);
    jdouble hourlyRate    = (*env)->GetDoubleField (env, empObj, ef.fid_hourlyRate);
    jint    overtimeHours = (*env)->GetIntField    (env, empObj, ef.fid_overtimeHours);

    /* Compute and return gross pay */
    jdouble gross = (jdouble)calcGross(hoursWorked, hourlyRate, overtimeHours);
    return gross;
}

/* ================================================================== */
/* 2.  computeNetPay                                                   */
/* ================================================================== */
JNIEXPORT jdouble JNICALL
Java_com_jni_fields_FieldComputeJNI_computeNetPay(JNIEnv  *env,
                                                   jobject  thisObj,
                                                   jobject  empObj)
{
    EmployeeFields ef;
    if (resolveFields(env, empObj, &ef) != 0) {
        return -1.0;
    }

    jint    hoursWorked   = (*env)->GetIntField   (env, empObj, ef.fid_hoursWorked);
    jdouble hourlyRate    = (*env)->GetDoubleField (env, empObj, ef.fid_hourlyRate);
    jint    overtimeHours = (*env)->GetIntField    (env, empObj, ef.fid_overtimeHours);
    jdouble taxRate       = (*env)->GetDoubleField (env, empObj, ef.fid_taxRate);

    double gross  = calcGross(hoursWorked, hourlyRate, overtimeHours);

    /* netPay = grossPay * (1.0 - taxRate) */
    double net = gross * (1.0 - (double)taxRate);
    return (jdouble)net;
}

/* ================================================================== */
/* 3.  computePaySummary                                               */
/* ================================================================== */
JNIEXPORT jstring JNICALL
Java_com_jni_fields_FieldComputeJNI_computePaySummary(JNIEnv  *env,
                                                       jobject  thisObj,
                                                       jobject  empObj)
{
    EmployeeFields ef;
    if (resolveFields(env, empObj, &ef) != 0) {
        return (*env)->NewStringUTF(env, "ERROR: could not resolve Employee fields.");
    }

    /* ---- Read every field ----------------------------------------- */
    jstring jName = (jstring)(*env)->GetObjectField(env, empObj, ef.fid_name);
    const char *nameCStr  = (*env)->GetStringUTFChars(env, jName, NULL);

    jint    hoursWorked   = (*env)->GetIntField   (env, empObj, ef.fid_hoursWorked);
    jdouble hourlyRate    = (*env)->GetDoubleField (env, empObj, ef.fid_hourlyRate);
    jint    overtimeHours = (*env)->GetIntField    (env, empObj, ef.fid_overtimeHours);
    jdouble taxRate       = (*env)->GetDoubleField (env, empObj, ef.fid_taxRate);

    /* ---- Derive computed values ------------------------------------ */
    double regularPay   = (double)hoursWorked  * (double)hourlyRate;
    double overtimePay  = (double)overtimeHours * (double)hourlyRate * 1.5;
    double grossPay     = regularPay + overtimePay;
    double taxAmount    = grossPay * (double)taxRate;
    double netPay       = grossPay - taxAmount;

    /* ---- Build summary string ------------------------------------- */
    char buf[2048];
    snprintf(buf, sizeof(buf),
        "=== Pay Slip ===\n"
        "Employee      : %s\n"
        "Hours worked  : %d  (incl. %d overtime)\n"
        "Hourly rate   : $%.2f\n"
        "Regular pay   : $%.2f  (%d hrs x $%.2f)\n"
        "Overtime pay  : $%.2f  (%d hrs x $%.2f x 1.5)\n"
        "Gross pay     : $%.2f\n"
        "Tax rate      : %.1f%%\n"
        "Tax deducted  : $%.2f\n"
        "Net pay       : $%.2f\n"
        "================",
        nameCStr,
        (int)hoursWorked, (int)overtimeHours,
        (double)hourlyRate,
        regularPay,  (int)hoursWorked,   (double)hourlyRate,
        overtimePay, (int)overtimeHours, (double)hourlyRate,
        grossPay,
        (double)taxRate * 100.0,
        taxAmount,
        netPay
    );

    (*env)->ReleaseStringUTFChars(env, jName, nameCStr);

    return (*env)->NewStringUTF(env, buf);
}