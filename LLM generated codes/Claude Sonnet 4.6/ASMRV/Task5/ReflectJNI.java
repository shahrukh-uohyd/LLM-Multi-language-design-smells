/**
 * Declares native methods that perform reflection-like operations.
 *
 * Each method targets the Vehicle class and returns member information
 * as a String array. Java then consumes and displays this information.
 *
 * Return array layout for inspectField:
 *   [0] – member kind      : "FIELD"
 *   [1] – field name       : e.g. "brand"
 *   [2] – JVM descriptor   : e.g. "Ljava/lang/String;"
 *   [3] – human type       : e.g. "String"
 *   [4] – access modifier  : "public" | "protected" | "private" | "package"
 *   [5] – found flag       : "true" | "false"
 *
 * Return array layout for inspectMethod:
 *   [0] – member kind      : "METHOD"
 *   [1] – method name      : e.g. "getBrand"
 *   [2] – JVM descriptor   : e.g. "()Ljava/lang/String;"
 *   [3] – return type      : e.g. "String"
 *   [4] – access modifier  : "public" | "protected" | "private" | "package"
 *   [5] – found flag       : "true" | "false"
 *
 * Return array layout for invokeGetter:
 *   [0] – "INVOKE_RESULT"
 *   [1] – method name
 *   [2] – return type
 *   [3] – result value as string
 *   [4] – "" (unused)
 *   [5] – found flag : "true" | "false"
 */
public class ReflectJNI {

    static {
        System.loadLibrary("ReflectJNI");
    }

    /**
     * Locates a field by name in the Vehicle class and returns
     * its descriptor, human-readable type, and access modifier.
     *
     * @param fieldName  name of the field to inspect (e.g. "brand")
     * @return           String[6] with field information
     */
    public native String[] inspectField(String fieldName);

    /**
     * Locates a method by name in the Vehicle class and returns
     * its descriptor, return type, and access modifier.
     *
     * @param methodName  name of the method to inspect (e.g. "getBrand")
     * @param descriptor  JVM descriptor of the method (e.g. "()Ljava/lang/String;")
     * @return            String[6] with method information
     */
    public native String[] inspectMethod(String methodName, String descriptor);

    /**
     * Invokes a zero-argument getter method on a Vehicle instance
     * and returns the result as a string.
     *
     * @param obj         the Vehicle instance to call the method on
     * @param methodName  name of the getter (e.g. "getBrand")
     * @param descriptor  JVM descriptor of the getter
     * @return            String[6]; slot [3] holds the result as a string
     */
    public native String[] invokeGetter(Vehicle obj,
                                        String  methodName,
                                        String  descriptor);
}