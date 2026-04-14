public class Student {

    private String name;          // PII — must never reach native grade computation
    private int    rollNumber;    // PII — irrelevant to average calculation
    private double mathGrade;     // needed
    private double scienceGrade;  // needed
    private double literatureGrade; // needed

    public Student(String name, int rollNumber,
                   double mathGrade, double scienceGrade, double literatureGrade) {
        this.name             = name;
        this.rollNumber       = rollNumber;
        this.mathGrade        = mathGrade;
        this.scienceGrade     = scienceGrade;
        this.literatureGrade  = literatureGrade;
    }

    // -----------------------------------------------------------------------
    // APPROACH A (Less Preferred):
    // Native code receives the full Student object reference.
    // It must self-restrict to only reading the three grade fields — but
    // 'name' and 'rollNumber' are structurally accessible. NOT recommended.
    // -----------------------------------------------------------------------
    public native double computeAverageGrade();

    // -----------------------------------------------------------------------
    // APPROACH B (RECOMMENDED — Principle of Least Privilege):
    // Java extracts the three grade fields before the JNI boundary.
    // 'name' and 'rollNumber' are structurally unreachable from native code.
    // -----------------------------------------------------------------------
    public double computeAverageGradeSecure() {
        // Field extraction is controlled here in trusted Java code.
        // Native code never receives a reference to this object.
        return nativeComputeAverageGrade(mathGrade, scienceGrade, literatureGrade);
    }

    // Private: callers use computeAverageGradeSecure(), not this directly.
    // Declared private so no external caller can inject arbitrary grade values.
    private native double nativeComputeAverageGrade(double mathGrade,
                                                     double scienceGrade,
                                                     double literatureGrade);

    // -----------------------------------------------------------------------
    // Standard accessors
    // -----------------------------------------------------------------------
    public String getName()        { return name; }
    public int    getRollNumber()  { return rollNumber; }
    public double getMathGrade()   { return mathGrade; }
    public double getScienceGrade(){ return scienceGrade; }
    public double getLiteratureGrade() { return literatureGrade; }

    static {
        System.loadLibrary("student_grades"); // libstudent_grades.so / .dll / .dylib
    }
}