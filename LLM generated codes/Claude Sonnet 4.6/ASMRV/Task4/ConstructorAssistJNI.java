/**
 * Declares three native methods.
 * Each one locates the Person constructor signature in the JVM,
 * prepares and validates data required by that constructor,
 * and returns the prepared data to Java as a String array.
 *
 * Java then uses the returned array to call the correct constructor.
 *
 * Returned array layout:
 *   index 0 – constructor signature tag : "FULL" | "PARTIAL" | "DEFAULT"
 *   index 1 – name   (or empty string)
 *   index 2 – age    (as string, or "0")
 *   index 3 – email  (or empty string)
 *   index 4 – role   (or empty string)
 */
public class ConstructorAssistJNI {

    static {
        System.loadLibrary("ConstructorAssistJNI");
    }

    /**
     * Locates the full 4-argument constructor.
     * Validates and formats raw input data,
     * then returns the prepared String array.
     *
     * @param rawName   raw name string (may need trimming/capitalising)
     * @param rawAge    raw age value   (validated: must be 0-120)
     * @param rawEmail  raw email       (validated: must contain '@')
     * @param rawRole   raw role string (normalised to title-case)
     * @return          prepared data array for the full constructor
     */
    public native String[] prepareFullConstructor(String rawName,
                                                  int    rawAge,
                                                  String rawEmail,
                                                  String rawRole);

    /**
     * Locates the 2-argument (name + age) constructor.
     * Validates and formats name and age only.
     *
     * @param rawName  raw name string
     * @param rawAge   raw age value
     * @return         prepared data array for the partial constructor
     */
    public native String[] preparePartialConstructor(String rawName,
                                                     int    rawAge);

    /**
     * Locates the no-argument (default) constructor.
     * Returns a data array indicating the default constructor should be used.
     *
     * @return  prepared data array for the default constructor
     */
    public native String[] prepareDefaultConstructor();
}