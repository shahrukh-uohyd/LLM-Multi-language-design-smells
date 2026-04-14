public class Student {
    // Student information fields
    private String name;
    private int rollNumber;
    
    // Grade fields
    private double mathGrade;
    private double scienceGrade;
    private double literatureGrade;

    static {
        // Load the native library (e.g., libstudentgrades.so or studentgrades.dll)
        System.loadLibrary("studentgrades");
    }

    public Student(String name, int rollNumber, double mathGrade, double scienceGrade, double literatureGrade) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.mathGrade = mathGrade;
        this.scienceGrade = scienceGrade;
        this.literatureGrade = literatureGrade;
    }

    // Instance native method. The object itself is passed to C++ as 'thisObj'
    public native double computeAverageGrade();
}