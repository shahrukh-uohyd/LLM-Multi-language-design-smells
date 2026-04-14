#include <jni.h>
#include <vector>
#include <string>

// A mock C++ validator logic
class NativeImageValidator {
public:
    static bool validateHeader(const uint8_t* data, size_t length, std::string& outError) {
        if (length < 4) {
            outError = "Invalid image header: file too small or missing header.";
            return false;
        }

        // Check magic numbers for supported formats
        // JPEG magic number: FF D8 FF
        if (data[0] == 0xFF && data[1] == 0xD8 && data[2] == 0xFF) {
            return true; 
        }
        // PNG magic number: 89 50 4E 47
        if (data[0] == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return true;
        }

        outError = "Unsupported image format or corrupted header.";
        return false;
    }
};

// Struct to hold failures temporarily in C++
struct InvalidRecord {
    std::string imageId;
    std::string errorReason;
};

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_ImageBatchValidator_validateImageBatch(JNIEnv *env, jobject thiz, jobjectArray imagesArray) {
    if (imagesArray == nullptr) {
        return nullptr;
    }

    jsize imageCount = env->GetArrayLength(imagesArray);
    
    // Cache the ImageRecord class and fields
    jclass recordClass = env->FindClass("ImageRecord");
    jfieldID fidImageId = env->GetFieldID(recordClass, "imageId", "Ljava/lang/String;");
    jfieldID fidImageData = env->GetFieldID(recordClass, "imageData", "[B");

    std::vector<InvalidRecord> invalidImages;

    // 1. Iterate over the batch and validate natively
    for (jsize i = 0; i < imageCount; ++i) {
        jobject recordObj = env->GetObjectArrayElement(imagesArray, i);
        if (recordObj == nullptr) continue;

        jstring jImageId = (jstring) env->GetObjectField(recordObj, fidImageId);
        jbyteArray jImageData = (jbyteArray) env->GetObjectField(recordObj, fidImageData);

        if (jImageId != nullptr && jImageData != nullptr) {
            // Get string ID
            const char* idChars = env->GetStringUTFChars(jImageId, nullptr);
            std::string imageId(idChars);
            env->ReleaseStringUTFChars(jImageId, idChars);

            // Access byte array memory directly without making a copy if possible
            jsize dataLength = env->GetArrayLength(jImageData);
            jbyte* dataBytes = env->GetByteArrayElements(jImageData, nullptr);

            std::string errorMessage;
            bool isValid = NativeImageValidator::validateHeader(
                reinterpret_cast<const uint8_t*>(dataBytes), 
                dataLength, 
                errorMessage
            );

            if (!isValid) {
                invalidImages.push_back({imageId, errorMessage});
            }

            // Release byte array. JNI_ABORT means we didn't modify it, so don't copy back to Java.
            env->ReleaseByteArrayElements(jImageData, dataBytes, JNI_ABORT);
        }

        // CRITICAL: Prevent JNI Local Reference table exhaustion
        env->DeleteLocalRef(jImageData);
        env->DeleteLocalRef(jImageId);
        env->DeleteLocalRef(recordObj);
    }
    env->DeleteLocalRef(recordClass);

    // 2. Prepare the result array to return to Java
    jclass resultClass = env->FindClass("ValidationResult");
    jmethodID resultConstructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");

    jobjectArray resultArray = env->NewObjectArray(invalidImages.size(), resultClass, nullptr);

    // 3. Populate the result array with the invalid images
    for (size_t i = 0; i < invalidImages.size(); ++i) {
        jstring jId = env->NewStringUTF(invalidImages[i].imageId.c_str());
        jstring jError = env->NewStringUTF(invalidImages[i].errorReason.c_str());

        jobject resultObj = env->NewObject(resultClass, resultConstructor, jId, jError);
        env->SetObjectArrayElement(resultArray, i, resultObj);

        // Clean up references
        env->DeleteLocalRef(jId);
        env->DeleteLocalRef(jError);
        env->DeleteLocalRef(resultObj);
    }

    env->DeleteLocalRef(resultClass);

    return resultArray;
}